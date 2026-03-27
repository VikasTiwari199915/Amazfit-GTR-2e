package com.vikas.gtr2e;

import android.app.Application;
import android.util.Log;

/**
 * Application implementation for GTR 2e
 * @author Vikas Tiwari
 */
public class GTR2eApp extends Application {
    public static final String TAG = "GTR2eAPP";
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "App Started");
    }
}
