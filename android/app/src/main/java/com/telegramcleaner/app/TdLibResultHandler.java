package com.telegramcleaner.app;

import com.getcapacitor.JSObject;

public interface TdLibResultHandler {
    void onSuccess(JSObject result);
    void onError(String error, String code);
}
