package com.telegramcleaner.app;

import android.os.Bundle;
import android.util.Log;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private static final String TAG = "MainActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "MainActivity.onCreate() - Registering TelegramCleanerPlugin...");
        registerPlugin(TelegramCleanerPlugin.class);
        Log.i(TAG, "MainActivity.onCreate() - Plugin registered, calling super.onCreate()");
        super.onCreate(savedInstanceState);
        Log.i(TAG, "MainActivity.onCreate() - Completed");
    }
}
