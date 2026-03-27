package com.vikas.gtr2e;

import android.app.Application;
import android.util.Log;

import com.vikas.gtr2e.services.GTR2eBleService;
import com.vikas.gtr2e.utils.GTR2eManager;

import lombok.Getter;
import lombok.Setter;

/**
 * Application implementation for GTR 2e
 * @author Vikas Tiwari
 */

public class GTR2eApp extends Application {
    public static final String TAG = "GTR2eAPP";
    private static GTR2eManager managerInstance;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "App Started");
    }

    public static void setGTR2eManager(GTR2eManager instance) {
        managerInstance = instance;
    }
    public static GTR2eManager getGTR2eManager() {
        return managerInstance;
    }
}
