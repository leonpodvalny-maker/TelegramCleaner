package com.telegramcleaner.app;

import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "TelegramCleanerPlugin")
public class TelegramCleanerPlugin extends Plugin {

    private static final String TAG = "TelegramCleanerPlugin";
    private TdLibHandler tdLibHandler;

    public TelegramCleanerPlugin() {
        super();
        Log.i(TAG, "TelegramCleanerPlugin constructor called");
    }

    @Override
    public void load() {
        super.load();
        Log.i(TAG, "TelegramCleanerPlugin.load() called - Plugin is now loaded!");

        // Initialize TDLib handler
        tdLibHandler = new TdLibHandler(getContext());
    }

    @PluginMethod
    public void ping(PluginCall call) {
        String message = call.getString("message", "ping");
        Log.d(TAG, "Ping received: " + message);

        JSObject response = new JSObject();
        response.put("success", true);
        response.put("message", "pong: " + message);

        call.resolve(response);
    }

    @PluginMethod
    public void initialize(PluginCall call) {
        Log.d(TAG, "Initialize called");

        int apiId = call.getInt("apiId", 0);
        String apiHash = call.getString("apiHash", "");
        String sessionString = call.getString("sessionString", "");

        if (apiId == 0 || apiHash.isEmpty()) {
            call.reject("API ID and API Hash are required");
            return;
        }

        tdLibHandler.initialize(apiId, apiHash, new TdLibResultHandler() {
            @Override
            public void onSuccess(JSObject result) {
                call.resolve(result);
            }

            @Override
            public void onError(String error, String code) {
                call.reject(error, code);
            }
        });
    }

    @PluginMethod
    public void sendCode(PluginCall call) {
        Log.d(TAG, "SendCode called");

        String phone = call.getString("phone", "");

        if (phone.isEmpty()) {
            call.reject("Phone number is required");
            return;
        }

        tdLibHandler.sendCode(phone, new TdLibResultHandler() {
            @Override
            public void onSuccess(JSObject result) {
                call.resolve(result);
            }

            @Override
            public void onError(String error, String code) {
                call.reject(error, code);
            }
        });
    }

    @PluginMethod
    public void signIn(PluginCall call) {
        Log.d(TAG, "SignIn called");

        String code = call.getString("code", "");

        if (code.isEmpty()) {
            call.reject("Verification code is required");
            return;
        }

        tdLibHandler.signIn(code, new TdLibResultHandler() {
            @Override
            public void onSuccess(JSObject result) {
                call.resolve(result);
            }

            @Override
            public void onError(String error, String errorCode) {
                call.reject(error, errorCode);
            }
        });
    }

    @PluginMethod
    public void signIn2FA(PluginCall call) {
        Log.d(TAG, "SignIn2FA called");

        String password = call.getString("password", "");

        if (password.isEmpty()) {
            call.reject("Password is required");
            return;
        }

        tdLibHandler.signIn2FA(password, new TdLibResultHandler() {
            @Override
            public void onSuccess(JSObject result) {
                call.resolve(result);
            }

            @Override
            public void onError(String error, String code) {
                call.reject(error, code);
            }
        });
    }

    @PluginMethod
    public void getDialogs(PluginCall call) {
        Log.d(TAG, "GetDialogs called");

        tdLibHandler.getDialogs(new TdLibResultHandler() {
            @Override
            public void onSuccess(JSObject result) {
                call.resolve(result);
            }

            @Override
            public void onError(String error, String code) {
                call.reject(error, code);
            }
        });
    }

    @PluginMethod
    public void getMessages(PluginCall call) {
        Log.d(TAG, "GetMessages called");

        String chatId = call.getString("chatId", "");
        int offsetId = call.getInt("offsetId", 0);
        int limit = call.getInt("limit", 100);

        if (chatId.isEmpty()) {
            call.reject("Chat ID is required");
            return;
        }

        tdLibHandler.getMessages(chatId, offsetId, limit, new TdLibResultHandler() {
            @Override
            public void onSuccess(JSObject result) {
                call.resolve(result);
            }

            @Override
            public void onError(String error, String code) {
                call.reject(error, code);
            }
        });
    }

    @PluginMethod
    public void batchScan(PluginCall call) {
        Log.d(TAG, "BatchScan called");

        // TODO: Implement batch scanning
        JSObject response = new JSObject();
        response.put("success", true);
        response.put("counts", new JSObject());
        call.resolve(response);
    }

    @PluginMethod
    public void deleteMessages(PluginCall call) {
        Log.d(TAG, "DeleteMessages called");

        String chatId = call.getString("chatId", "");
        boolean revoke = call.getBoolean("revoke", true);

        if (chatId.isEmpty()) {
            call.reject("Chat ID is required");
            return;
        }

        // Get message IDs from call
        try {
            org.json.JSONArray messageIdsArray = call.getArray("messageIds");
            if (messageIdsArray == null) {
                call.reject("Message IDs are required");
                return;
            }

            long[] messageIds = new long[messageIdsArray.length()];
            for (int i = 0; i < messageIdsArray.length(); i++) {
                messageIds[i] = messageIdsArray.getLong(i);
            }

            tdLibHandler.deleteMessages(chatId, messageIds, revoke, new TdLibResultHandler() {
                @Override
                public void onSuccess(JSObject result) {
                    call.resolve(result);
                }

                @Override
                public void onError(String error, String code) {
                    call.reject(error, code);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error parsing message IDs", e);
            call.reject("Invalid message IDs format");
        }
    }

    @PluginMethod
    public void logout(PluginCall call) {
        Log.d(TAG, "Logout called");

        tdLibHandler.logout(new TdLibResultHandler() {
            @Override
            public void onSuccess(JSObject result) {
                call.resolve(result);
            }

            @Override
            public void onError(String error, String code) {
                call.reject(error, code);
            }
        });
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
        if (tdLibHandler != null) {
            tdLibHandler.close();
        }
    }
}
