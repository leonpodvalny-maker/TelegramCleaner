package com.telegramcleaner.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.drinkless.tdlib.TdApi;

public class AuthStateManager {

    private static final String TAG = "AuthStateManager";
    private static final String PREFS_NAME = "TelegramCleanerPrefs";
    private static final String KEY_API_ID = "apiId";
    private static final String KEY_API_HASH = "apiHash";
    private static final String KEY_AUTH_STATE = "authState";

    private final SharedPreferences prefs;
    private TdApi.AuthorizationState currentState;
    private long currentUserId;
    private String phoneNumber;
    private String phoneCodeHash;

    public AuthStateManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.currentState = null;
        this.currentUserId = 0;
    }

    public void updateAuthState(TdApi.AuthorizationState state) {
        this.currentState = state;
        Log.d(TAG, "Auth state updated: " + state.getClass().getSimpleName());

        if (state instanceof TdApi.AuthorizationStateReady) {
            saveAuthState("ready");
        } else if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
            saveAuthState("wait_phone");
        } else if (state instanceof TdApi.AuthorizationStateWaitCode) {
            saveAuthState("wait_code");
        } else if (state instanceof TdApi.AuthorizationStateWaitPassword) {
            saveAuthState("wait_password");
        } else if (state instanceof TdApi.AuthorizationStateLoggingOut) {
            saveAuthState("logging_out");
        } else if (state instanceof TdApi.AuthorizationStateClosing) {
            saveAuthState("closing");
        } else if (state instanceof TdApi.AuthorizationStateClosed) {
            saveAuthState("closed");
            clearAuthData();
        }
    }

    public TdApi.AuthorizationState getCurrentState() {
        return currentState;
    }

    public boolean isReady() {
        return currentState instanceof TdApi.AuthorizationStateReady;
    }

    public boolean needsPhoneNumber() {
        return currentState instanceof TdApi.AuthorizationStateWaitPhoneNumber;
    }

    public boolean needsCode() {
        return currentState instanceof TdApi.AuthorizationStateWaitCode;
    }

    public boolean needsPassword() {
        return currentState instanceof TdApi.AuthorizationStateWaitPassword;
    }

    public void setCurrentUserId(long userId) {
        this.currentUserId = userId;
    }

    public long getCurrentUserId() {
        return currentUserId;
    }

    public void setPhoneNumber(String phone) {
        this.phoneNumber = phone;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneCodeHash(String hash) {
        this.phoneCodeHash = hash;
    }

    public String getPhoneCodeHash() {
        return phoneCodeHash;
    }

    public void saveApiCredentials(int apiId, String apiHash) {
        prefs.edit()
                .putInt(KEY_API_ID, apiId)
                .putString(KEY_API_HASH, apiHash)
                .apply();
        Log.d(TAG, "API credentials saved");
    }

    public int getSavedApiId() {
        return prefs.getInt(KEY_API_ID, 0);
    }

    public String getSavedApiHash() {
        return prefs.getString(KEY_API_HASH, "");
    }

    public boolean hasApiCredentials() {
        return getSavedApiId() != 0 && !getSavedApiHash().isEmpty();
    }

    private void saveAuthState(String state) {
        prefs.edit().putString(KEY_AUTH_STATE, state).apply();
    }

    public String getSavedAuthState() {
        return prefs.getString(KEY_AUTH_STATE, "unknown");
    }

    public void clearAuthData() {
        prefs.edit()
                .remove(KEY_AUTH_STATE)
                .apply();
        currentUserId = 0;
        phoneNumber = null;
        phoneCodeHash = null;
        Log.d(TAG, "Auth data cleared");
    }

    public void clearAllData() {
        prefs.edit().clear().apply();
        currentState = null;
        currentUserId = 0;
        phoneNumber = null;
        phoneCodeHash = null;
        Log.d(TAG, "All data cleared");
    }
}
