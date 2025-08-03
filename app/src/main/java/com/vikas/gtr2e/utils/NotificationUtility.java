package com.vikas.gtr2e.utils;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.vikas.gtr2e.R;

public class NotificationUtility {


    private static final int NOTIFICATION_ID = 101;
    private static final String CHANNEL_ID = "gtr2e_ble_service_channel";

    @SuppressLint("MissingPermission")
    public static void updateNotification(Context context, boolean isConnected, Service service) {
        Notification notification = createNotification(context, isConnected);
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification);

        // Re-start as foreground if needed (when user dismissed but service should stay)
        if (!isNotificationActive(context)) {
            startAsForegroundService(service, isConnected);
        }
    }

    private static boolean isNotificationActive(Context context) {
        StatusBarNotification[] notifications = NotificationManagerCompat.from(context)
                .getActiveNotifications().toArray(new StatusBarNotification[0]);
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == NOTIFICATION_ID) {
                return true;
            }
        }
        return false;
    }

    public static Notification createNotification(Context context, boolean isConnected) {
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("GTR 2e Service")
                .setContentText(isConnected ?
                        "Connected to Amazfit GTR 2e" : "Device disconnected")
                .setSmallIcon(R.drawable.rounded_aod_watch_24)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(false) // Allow dismissal
                .setAutoCancel(false)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                //.addAction(createDisconnectAction())
                .build();
    }

    public static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Device Connection",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("BLE connection status");
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    public static void startAsForegroundService(Service context, boolean isConnected) {
        Notification notification = NotificationUtility.createNotification(context, isConnected);
        context.startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
    }

}
