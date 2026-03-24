package com.vikas.gtr2e.services;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.vikas.gtr2e.GTR2eManager;

public class NotificationListener extends NotificationListenerService {
    private static final String TAG = "NotificationListener";

    @Override
    public void onListenerConnected() {
        Log.i(TAG, "Notification listener connected");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Handle notification posted if needed
        Log.i(TAG, "Notification posted: " + sbn.getPackageName());
        if (getGtr2eManager()!=null && getBleService()!=null) {
            getBleService().updateMediaController();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle notification removed if needed
        Log.i(TAG, "Notification removed: " + sbn.getPackageName());
    }

    private GTR2eManager getGtr2eManager() {
        return GTR2eManager.getInstance(getApplicationContext());
    }

    private GTR2eBleService getBleService() {
        return getGtr2eManager().getBleService();
    }
}
