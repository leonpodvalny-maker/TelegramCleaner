package com.telegramcleaner.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TdLibHandler {

    private static final String TAG = "TdLibHandler";
    private static final int TIMEOUT_SECONDS = 30;

    private final Context context;
    private final AuthStateManager authStateManager;
    private final Handler mainHandler;
    private final Lock lock;

    private Client client;
    private boolean isInitialized;
    private int apiId;
    private String apiHash;

    private Map<Long, TdApi.Chat> chatCache;
    private Map<Long, TdApi.User> userCache;

    // Rate limiters for optimal API usage
    private DeletionRateLimiter deletionRateLimiter;
    private RetrievalRateLimiter retrievalRateLimiter;

    static {
        try {
            System.loadLibrary("tdjni");
            Log.d(TAG, "TDLib native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load TDLib native library", e);
        }
    }

    public TdLibHandler(Context context) {
        this.context = context;
        this.authStateManager = new AuthStateManager(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.lock = new ReentrantLock();
        this.isInitialized = false;
        this.chatCache = new HashMap<>();
        this.userCache = new HashMap<>();

        // Initialize rate limiters
        this.deletionRateLimiter = new DeletionRateLimiter();
        this.retrievalRateLimiter = new RetrievalRateLimiter();

        try {
            Client.execute(new TdApi.SetLogVerbosityLevel(2));
        } catch (Throwable e) {
            Log.e(TAG, "Failed to set log verbosity level", e);
        }
    }

    public void initialize(int apiId, String apiHash, TdLibResultHandler handler) {
        Log.d(TAG, "Initializing TDLib with API ID: " + apiId);

        this.apiId = apiId;
        this.apiHash = apiHash;

        authStateManager.saveApiCredentials(apiId, apiHash);

        if (client != null) {
            Log.d(TAG, "Client already exists, checking auth state");
            checkAuthStatus(handler);
            return;
        }

        try {
            client = Client.create(
                update -> {
                    handleUpdate(update);
                },
                null,
                null
            );

            sendSetTdlibParameters(handler);

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize TDLib", e);
            handler.onError("Failed to initialize: " + e.getMessage(), "INIT_ERROR");
        }
    }

    private void handleUpdate(TdApi.Object update) {
        if (update instanceof TdApi.UpdateAuthorizationState) {
            TdApi.UpdateAuthorizationState authUpdate = (TdApi.UpdateAuthorizationState) update;
            authStateManager.updateAuthState(authUpdate.authorizationState);
            Log.d(TAG, "Authorization state: " + authUpdate.authorizationState.getClass().getSimpleName());
        } else if (update instanceof TdApi.UpdateUser) {
            TdApi.UpdateUser userUpdate = (TdApi.UpdateUser) update;
            userCache.put(userUpdate.user.id, userUpdate.user);
        } else if (update instanceof TdApi.UpdateNewChat) {
            TdApi.UpdateNewChat chatUpdate = (TdApi.UpdateNewChat) update;
            chatCache.put(chatUpdate.chat.id, chatUpdate.chat);
        }
    }

    private void sendSetTdlibParameters(TdLibResultHandler handler) {
        TdApi.SetTdlibParameters parameters = new TdApi.SetTdlibParameters();
        parameters.databaseDirectory = context.getFilesDir().getAbsolutePath() + "/tdlib";
        parameters.useMessageDatabase = true;
        parameters.useSecretChats = false;
        parameters.apiId = apiId;
        parameters.apiHash = apiHash;
        parameters.systemLanguageCode = "en";
        parameters.deviceModel = "Android";
        parameters.systemVersion = android.os.Build.VERSION.RELEASE;
        parameters.applicationVersion = "1.0";

        sendRequest(parameters, result -> {
            if (result instanceof TdApi.Ok) {
                Log.d(TAG, "TDLib parameters set successfully");
                isInitialized = true;
                checkAuthStatus(handler);
            } else if (result instanceof TdApi.Error) {
                TdApi.Error error = (TdApi.Error) result;
                Log.e(TAG, "Failed to set parameters: " + error.message);
                handler.onError(error.message, String.valueOf(error.code));
            }
        });
    }

    private void checkAuthStatus(TdLibResultHandler handler) {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] done = {false};

        new Thread(() -> {
            try {
                latch.await(5, TimeUnit.SECONDS);

                if (!done[0]) {
                    JSObject response = new JSObject();
                    response.put("success", true);
                    response.put("isAuthorized", authStateManager.isReady());

                    if (authStateManager.isReady()) {
                        long userId = authStateManager.getCurrentUserId();
                        if (userId > 0 && userCache.containsKey(userId)) {
                            TdApi.User user = userCache.get(userId);
                            response.put("user", userToJson(user));
                        }
                    }

                    done[0] = true;
                    mainHandler.post(() -> handler.onSuccess(response));
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Check auth status interrupted", e);
            }
        }).start();

        sendRequest(new TdApi.GetMe(), result -> {
            if (result instanceof TdApi.User) {
                TdApi.User user = (TdApi.User) result;
                authStateManager.setCurrentUserId(user.id);
                userCache.put(user.id, user);

                JSObject response = new JSObject();
                response.put("success", true);
                response.put("isAuthorized", true);
                response.put("user", userToJson(user));

                done[0] = true;
                latch.countDown();
                mainHandler.post(() -> handler.onSuccess(response));
            } else {
                done[0] = true;
                latch.countDown();
            }
        });
    }

    public void sendCode(String phoneNumber, TdLibResultHandler handler) {
        Log.d(TAG, "Sending code to: " + phoneNumber);

        authStateManager.setPhoneNumber(phoneNumber);

        TdApi.SetAuthenticationPhoneNumber authPhone = new TdApi.SetAuthenticationPhoneNumber();
        authPhone.phoneNumber = phoneNumber;
        authPhone.settings = new TdApi.PhoneNumberAuthenticationSettings();
        authPhone.settings.allowFlashCall = false;
        authPhone.settings.isCurrentPhoneNumber = false;
        authPhone.settings.allowSmsRetrieverApi = false;

        sendRequest(authPhone, result -> {
            if (result instanceof TdApi.Ok) {
                JSObject response = new JSObject();
                response.put("success", true);
                mainHandler.post(() -> handler.onSuccess(response));
            } else if (result instanceof TdApi.Error) {
                TdApi.Error error = (TdApi.Error) result;
                Log.e(TAG, "Send code error: " + error.message);
                mainHandler.post(() -> handler.onError(error.message, String.valueOf(error.code)));
            }
        });
    }

    public void signIn(String code, TdLibResultHandler handler) {
        Log.d(TAG, "Signing in with code");

        TdApi.CheckAuthenticationCode checkCode = new TdApi.CheckAuthenticationCode();
        checkCode.code = code;

        sendRequest(checkCode, result -> {
            if (result instanceof TdApi.Ok) {
                waitForAuthReady(handler);
            } else if (result instanceof TdApi.Error) {
                TdApi.Error error = (TdApi.Error) result;
                Log.e(TAG, "Sign in error: " + error.message);

                if (authStateManager.needsPassword()) {
                    JSObject response = new JSObject();
                    response.put("success", false);
                    response.put("needs2FA", true);
                    mainHandler.post(() -> handler.onSuccess(response));
                } else {
                    mainHandler.post(() -> handler.onError(error.message, String.valueOf(error.code)));
                }
            }
        });
    }

    public void signIn2FA(String password, TdLibResultHandler handler) {
        Log.d(TAG, "Signing in with 2FA password");

        TdApi.CheckAuthenticationPassword checkPassword = new TdApi.CheckAuthenticationPassword();
        checkPassword.password = password;

        sendRequest(checkPassword, result -> {
            if (result instanceof TdApi.Ok) {
                waitForAuthReady(handler);
            } else if (result instanceof TdApi.Error) {
                TdApi.Error error = (TdApi.Error) result;
                Log.e(TAG, "2FA error: " + error.message);
                mainHandler.post(() -> handler.onError(error.message, String.valueOf(error.code)));
            }
        });
    }

    private void waitForAuthReady(TdLibResultHandler handler) {
        new Thread(() -> {
            int attempts = 0;
            while (attempts < 50) {
                if (authStateManager.isReady()) {
                    sendRequest(new TdApi.GetMe(), result -> {
                        if (result instanceof TdApi.User) {
                            TdApi.User user = (TdApi.User) result;
                            authStateManager.setCurrentUserId(user.id);
                            userCache.put(user.id, user);

                            JSObject response = new JSObject();
                            response.put("success", true);
                            response.put("user", userToJson(user));
                            response.put("sessionString", "");

                            mainHandler.post(() -> handler.onSuccess(response));
                        }
                    });
                    return;
                }

                try {
                    Thread.sleep(100);
                    attempts++;
                } catch (InterruptedException e) {
                    break;
                }
            }

            mainHandler.post(() -> handler.onError("Authentication timeout", "TIMEOUT"));
        }).start();
    }

    public void getDialogs(TdLibResultHandler handler) {
        Log.d(TAG, "Getting dialogs");

        if (!authStateManager.isReady()) {
            handler.onError("Not authorized", "NOT_AUTHORIZED");
            return;
        }

        TdApi.GetChats getChats = new TdApi.GetChats();
        getChats.chatList = new TdApi.ChatListMain();
        getChats.limit = 50;

        sendRequest(getChats, result -> {
            if (result instanceof TdApi.Chats) {
                TdApi.Chats chats = (TdApi.Chats) result;
                List<TdApi.Chat> chatList = new ArrayList<>();

                CountDownLatch latch = new CountDownLatch(chats.chatIds.length);

                for (long chatId : chats.chatIds) {
                    sendRequest(new TdApi.GetChat(chatId), chatResult -> {
                        if (chatResult instanceof TdApi.Chat) {
                            chatList.add((TdApi.Chat) chatResult);
                        }
                        latch.countDown();
                    });
                }

                new Thread(() -> {
                    try {
                        latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                        JSArray dialogsArray = new JSArray();
                        for (TdApi.Chat chat : chatList) {
                            dialogsArray.put(chatToJson(chat));
                        }

                        JSObject response = new JSObject();
                        response.put("success", true);
                        response.put("dialogs", dialogsArray);

                        mainHandler.post(() -> handler.onSuccess(response));

                    } catch (InterruptedException e) {
                        Log.e(TAG, "Get dialogs interrupted", e);
                        mainHandler.post(() -> handler.onError("Timeout loading chats", "TIMEOUT"));
                    }
                }).start();

            } else if (result instanceof TdApi.Error) {
                TdApi.Error error = (TdApi.Error) result;
                Log.e(TAG, "Get chats error: " + error.message);
                mainHandler.post(() -> handler.onError(error.message, String.valueOf(error.code)));
            }
        });
    }

    public void getMessages(String chatIdStr, int offsetId, int limit, TdLibResultHandler handler) {
        Log.d(TAG, "Getting messages for chat: " + chatIdStr);

        if (!authStateManager.isReady()) {
            handler.onError("Not authorized", "NOT_AUTHORIZED");
            return;
        }

        long chatId;
        try {
            chatId = Long.parseLong(chatIdStr);
        } catch (NumberFormatException e) {
            handler.onError("Invalid chat ID", "INVALID_CHAT_ID");
            return;
        }

        // Use optimal page size (100 messages max per request)
        int optimalLimit = Math.min(limit, retrievalRateLimiter.getOptimalPageSize());

        // Apply adaptive pacing before request
        new Thread(() -> {
            retrievalRateLimiter.waitBeforeRequest();

            TdApi.GetChatHistory getChatHistory = new TdApi.GetChatHistory();
            getChatHistory.chatId = chatId;
            getChatHistory.fromMessageId = offsetId;
            getChatHistory.offset = 0;
            getChatHistory.limit = optimalLimit;
            getChatHistory.onlyLocal = false;

            long currentUserId = authStateManager.getCurrentUserId();

            sendRequest(getChatHistory, result -> {
                if (result instanceof TdApi.Messages) {
                    TdApi.Messages messages = (TdApi.Messages) result;
                    JSArray messagesArray = new JSArray();

                    for (TdApi.Message msg : messages.messages) {
                        long senderId = getSenderId(msg);
                        if (senderId == currentUserId) {
                            messagesArray.put(messageToJson(msg));
                        }
                    }

                    JSObject response = new JSObject();
                    response.put("success", true);
                    response.put("messages", messagesArray);

                    // Track successful retrieval
                    retrievalRateLimiter.onSuccess();

                    mainHandler.post(() -> handler.onSuccess(response));

                } else if (result instanceof TdApi.Error) {
                    TdApi.Error error = (TdApi.Error) result;
                    Log.e(TAG, "Get messages error: " + error.message);

                    // Handle FloodWait for retrievals
                    if (error.code == 429) {
                        int waitSeconds = extractWaitTime(error.message);
                        retrievalRateLimiter.onFloodWait(waitSeconds);

                        JSObject response = new JSObject();
                        response.put("success", false);
                        response.put("floodWait", waitSeconds);
                        mainHandler.post(() -> handler.onSuccess(response));
                    } else {
                        retrievalRateLimiter.onError();
                        mainHandler.post(() -> handler.onError(error.message, String.valueOf(error.code)));
                    }
                }
            });
        }).start();
    }

    public void deleteMessages(String chatIdStr, long[] messageIds, boolean revoke, TdLibResultHandler handler) {
        Log.d(TAG, "Deleting " + messageIds.length + " messages from chat: " + chatIdStr);

        if (!authStateManager.isReady()) {
            handler.onError("Not authorized", "NOT_AUTHORIZED");
            return;
        }

        long chatId;
        try {
            chatId = Long.parseLong(chatIdStr);
        } catch (NumberFormatException e) {
            handler.onError("Invalid chat ID", "INVALID_CHAT_ID");
            return;
        }

        // Get chat type for optimal batch sizing
        String chatType = getChatTypeFromCache(chatId);

        // Sort message IDs in descending order (newer messages first - higher IDs = newer)
        long[] sortedIds = messageIds.clone();
        java.util.Arrays.sort(sortedIds);
        reverseArray(sortedIds);

        // Calculate message age for oldest message (approximate)
        long oldestMessageId = sortedIds[sortedIds.length - 1];
        long messageAgeHours = estimateMessageAgeHours(oldestMessageId);

        // Get optimal batch size
        int batchSize = deletionRateLimiter.getOptimalBatchSize(chatType, messageAgeHours);

        Log.d(TAG, "Using batch size: " + batchSize + " for chat type: " + chatType);

        // Split into batches
        List<long[]> batches = splitIntoBatches(sortedIds, batchSize);

        // Process batches using global rate limiter queue
        processDeletionBatches(chatId, batches, revoke, handler);
    }

    /**
     * Process deletion batches sequentially through rate limiter
     */
    private void processDeletionBatches(long chatId, List<long[]> batches, boolean revoke, TdLibResultHandler handler) {
        final int totalBatches = batches.size();
        final AtomicInteger completedBatches = new AtomicInteger(0);
        final AtomicInteger totalDeleted = new AtomicInteger(0);
        final AtomicBoolean hasError = new AtomicBoolean(false);

        for (int i = 0; i < batches.size(); i++) {
            final long[] batch = batches.get(i);
            final int batchNumber = i + 1;

            deletionRateLimiter.queueDeletion(() -> {
                if (hasError.get()) {
                    return; // Skip remaining batches if error occurred
                }

                Log.d(TAG, "Processing batch " + batchNumber + "/" + totalBatches + " (" + batch.length + " messages)");

                TdApi.DeleteMessages deleteMessages = new TdApi.DeleteMessages();
                deleteMessages.chatId = chatId;
                deleteMessages.messageIds = batch;
                deleteMessages.revoke = revoke;

                sendRequest(deleteMessages, result -> {
                    if (result instanceof TdApi.Ok) {
                        totalDeleted.addAndGet(batch.length);
                        deletionRateLimiter.onSuccess();

                        int completed = completedBatches.incrementAndGet();
                        Log.d(TAG, "Batch " + batchNumber + " completed. Total progress: " + completed + "/" + totalBatches);

                        // Send final response when all batches complete
                        if (completed == totalBatches) {
                            JSObject response = new JSObject();
                            response.put("success", true);
                            response.put("deleted", totalDeleted.get());

                            mainHandler.post(() -> handler.onSuccess(response));
                        }

                    } else if (result instanceof TdApi.Error) {
                        TdApi.Error error = (TdApi.Error) result;
                        Log.e(TAG, "Delete batch error: " + error.message);

                        if (error.code == 429) {
                            // FloodWait - pause and notify
                            int waitSeconds = extractWaitTime(error.message);
                            deletionRateLimiter.onFloodWait(waitSeconds);

                            hasError.set(true);

                            JSObject response = new JSObject();
                            response.put("success", false);
                            response.put("floodWait", waitSeconds);
                            response.put("deleted", totalDeleted.get());
                            response.put("remaining", (totalBatches - completedBatches.get()) * batch.length);

                            mainHandler.post(() -> handler.onSuccess(response));

                        } else {
                            deletionRateLimiter.onError();
                            hasError.set(true);

                            mainHandler.post(() -> handler.onError(error.message, String.valueOf(error.code)));
                        }
                    }
                });
            });
        }
    }

    /**
     * Split message IDs into batches
     */
    private List<long[]> splitIntoBatches(long[] messageIds, int batchSize) {
        List<long[]> batches = new ArrayList<>();

        for (int i = 0; i < messageIds.length; i += batchSize) {
            int end = Math.min(i + batchSize, messageIds.length);
            long[] batch = java.util.Arrays.copyOfRange(messageIds, i, end);
            batches.add(batch);
        }

        return batches;
    }

    /**
     * Reverse array in place
     */
    private void reverseArray(long[] array) {
        int left = 0;
        int right = array.length - 1;

        while (left < right) {
            long temp = array[left];
            array[left] = array[right];
            array[right] = temp;
            left++;
            right--;
        }
    }

    /**
     * Get chat type from cache
     */
    private String getChatTypeFromCache(long chatId) {
        TdApi.Chat chat = chatCache.get(chatId);
        if (chat != null) {
            return getChatType(chat.type);
        }
        return "private"; // Default to private (more conservative)
    }

    /**
     * Estimate message age in hours based on message ID
     * This is approximate - newer messages typically have higher IDs
     */
    private long estimateMessageAgeHours(long messageId) {
        // Very rough estimation: message IDs increase over time
        // Recent messages (last 24h) typically have IDs > 1000000000
        // Older messages have lower IDs

        if (messageId > 100000000) {
            return 1; // Assume recent (< 24h)
        } else if (messageId > 10000000) {
            return 48; // ~2 days
        } else {
            return 7 * 24; // > 1 week
        }
    }

    public void logout(TdLibResultHandler handler) {
        Log.d(TAG, "Logging out");

        sendRequest(new TdApi.LogOut(), result -> {
            if (result instanceof TdApi.Ok) {
                authStateManager.clearAuthData();
                chatCache.clear();
                userCache.clear();

                JSObject response = new JSObject();
                response.put("success", true);

                mainHandler.post(() -> handler.onSuccess(response));

            } else if (result instanceof TdApi.Error) {
                TdApi.Error error = (TdApi.Error) result;
                Log.e(TAG, "Logout error: " + error.message);
                mainHandler.post(() -> handler.onError(error.message, String.valueOf(error.code)));
            }
        });
    }

    private void sendRequest(TdApi.Function<?> request, Client.ResultHandler resultHandler) {
        if (client == null) {
            Log.e(TAG, "Client is null, cannot send request");
            return;
        }
        client.send(request, resultHandler);
    }

    private long getSenderId(TdApi.Message message) {
        if (message.senderId instanceof TdApi.MessageSenderUser) {
            return ((TdApi.MessageSenderUser) message.senderId).userId;
        }
        return 0;
    }

    private int extractWaitTime(String errorMessage) {
        Pattern pattern = Pattern.compile("retry after (\\d+)");
        Matcher matcher = pattern.matcher(errorMessage);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 60;
    }

    private JSObject userToJson(TdApi.User user) {
        JSObject json = new JSObject();
        json.put("id", user.id);
        json.put("firstName", user.firstName);
        json.put("lastName", user.lastName);
        json.put("username", user.usernames != null && user.usernames.activeUsernames.length > 0
            ? user.usernames.activeUsernames[0] : "");
        json.put("phoneNumber", user.phoneNumber);
        return json;
    }

    private JSObject chatToJson(TdApi.Chat chat) {
        JSObject json = new JSObject();
        json.put("id", String.valueOf(chat.id));
        json.put("name", chat.title);
        json.put("type", getChatType(chat.type));
        json.put("avatar", getAvatarInitials(chat.title));

        String lastMessage = "";
        if (chat.lastMessage != null && chat.lastMessage.content instanceof TdApi.MessageText) {
            TdApi.MessageText textContent = (TdApi.MessageText) chat.lastMessage.content;
            lastMessage = textContent.text.text;
        }
        json.put("lastMessage", lastMessage);

        json.put("messageCount", 0);
        json.put("date", chat.lastMessage != null ? chat.lastMessage.date : 0);
        json.put("time", "");

        return json;
    }

    private JSObject messageToJson(TdApi.Message message) {
        JSObject json = new JSObject();
        json.put("id", message.id);

        String text = "";
        String type = "text";

        if (message.content instanceof TdApi.MessageText) {
            TdApi.MessageText textContent = (TdApi.MessageText) message.content;
            text = textContent.text.text;
        } else if (message.content instanceof TdApi.MessagePhoto) {
            text = "[Photo]";
            type = "photo";
        } else if (message.content instanceof TdApi.MessageVideo) {
            text = "[Video]";
            type = "video";
        } else if (message.content instanceof TdApi.MessageDocument) {
            text = "[Document]";
            type = "file";
        }

        json.put("text", text);
        json.put("type", type);
        json.put("date", new java.util.Date(message.date * 1000L).toString());
        json.put("sender", "You");

        return json;
    }

    private String getChatType(TdApi.ChatType type) {
        if (type instanceof TdApi.ChatTypePrivate) {
            return "private";
        } else if (type instanceof TdApi.ChatTypeBasicGroup) {
            return "group";
        } else if (type instanceof TdApi.ChatTypeSupergroup) {
            TdApi.ChatTypeSupergroup supergroup = (TdApi.ChatTypeSupergroup) type;
            return supergroup.isChannel ? "channel" : "group";
        }
        return "unknown";
    }

    private String getAvatarInitials(String name) {
        if (name == null || name.isEmpty()) {
            return "?";
        }
        String[] words = name.split(" ");
        if (words.length >= 2) {
            return (words[0].charAt(0) + "" + words[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    public void close() {
        if (client != null) {
            sendRequest(new TdApi.Close(), result -> {
                Log.d(TAG, "Client closed");
            });
            client = null;
        }
        isInitialized = false;
    }
}
