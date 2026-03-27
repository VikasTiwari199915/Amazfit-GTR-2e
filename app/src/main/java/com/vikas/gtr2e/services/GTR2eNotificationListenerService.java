package com.vikas.gtr2e.services;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.vikas.gtr2e.GTR2eApp;
import com.vikas.gtr2e.utils.GTR2eManager;

/**
 * NotificationListenerService implementation for GTR2e
 * Notifies when notification is posted or removed
 * @author Vikas Tiwari
 */
public class GTR2eNotificationListenerService extends NotificationListenerService {
    private static final String TAG = "NotificationListener";

    @Override
    public void onListenerConnected() {
        Log.i(TAG, "Notification listener connected");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Handle notification posted if needed
        Log.i(TAG, "Notification posted: " + sbn.getPackageName());
        if (getBleService()!=null) {
            getBleService().updateMediaController();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle notification removed if needed
        Log.i(TAG, "Notification removed: " + sbn.getPackageName());
    }

    private GTR2eBleService getBleService() {
        if(GTR2eApp.getGTR2eManager()!=null) {
            return GTR2eApp.getGTR2eManager().getBleService();
        } else {
            return null;
        }
    }
}
