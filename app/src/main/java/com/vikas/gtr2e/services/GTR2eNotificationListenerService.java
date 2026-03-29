package com.vikas.gtr2e.services;

import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.vikas.gtr2e.GTR2eApp;
import com.vikas.gtr2e.utils.GTR2eNotificationUtil;
import com.vikas.gtr2e.utils.Prefs;

/**
 * NotificationListenerService implementation for GTR2e
 * Notifies when notification is posted or removed
 *
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

        handleNotification(sbn);
    }

    private void handleNotification(StatusBarNotification sbn) {
        GTR2eBleService bleService = getBleService();
        if (bleService != null) {
            Notification notification = sbn.getNotification();
            Bundle extras = notification.extras;
            if (extras != null) {
                CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
                CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
                CharSequence subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
                CharSequence template = extras.getCharSequence(Notification.EXTRA_TEMPLATE);

                String packageName = sbn.getPackageName();

                Log.d("NOTIF", "App: " + packageName);
                Log.d("NOTIF", "Title: " + title);
                Log.d("NOTIF", "Text: " + text);
                Log.d("NOTIF", "SubText: " + subText);

                if (!isMediaSessionNotification(notification)) {
                    logBundleData(extras);
                    String source, message;
                    if (isMessageType(template) || GTR2eNotificationUtil.shouldUseSenderAsSource(packageName, 1)) {
                        source = title != null ? title.toString() : getAppName(sbn.getPackageName());
                        message = text != null ? text.toString() : (subText != null ? subText.toString() : "");
                    } else {
                        source = getAppName(sbn.getPackageName());
                        message = (title != null ? title.toString() : "") + "\n" + (text != null ? text.toString() : "");
                    }
                    if (Prefs.getVoipCallAlertsEnabled(getApplicationContext()) && isCallType(template)) {
                        GTR2eCallService.addNotificationForActiveCall(sbn);
                        bleService.sendIncomingCallAlert(source);
                    } else {
                        bleService.onNotification(sbn.getPackageName(), source, message);
                    }
                } else {
                    bleService.updateMediaController();
                }
            } else {
                Log.w(TAG, "Posted notification has no extras");
            }
        }

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle notification removed if needed
        Log.i(TAG, "Notification removed: " + sbn.getPackageName());
        if (getBleService() != null) {
            Notification notification = sbn.getNotification();
            Bundle extras = notification.extras;
            if (extras != null) {
                CharSequence template = extras.getCharSequence(Notification.EXTRA_TEMPLATE);
                if (isCallType(template)) {
                    GTR2eCallService.removeNotificationForActiveCall();
                }
            }
        }
    }

    private GTR2eBleService getBleService() {
        if (GTR2eApp.getGTR2eManager() != null) {
            return GTR2eApp.getGTR2eManager().getBleService();
        } else {
            return null;
        }
    }

    private boolean isMediaSessionNotification(Notification notification) {
        return (notification.extras != null && notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION))
                || Notification.CATEGORY_TRANSPORT.equals(notification.category);
    }

    private String getAppName(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return "Unknown App";
        }
    }

    private void logBundleData(Bundle bundle) {
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                Log.d("BUNDLE", key + " = " + value);
            }
        }
    }

    private boolean isMessageType(CharSequence template) {
        if (template != null) {
            return template.equals("android.app.Notification$InboxStyle");
        }
        return false;
    }

    private boolean isCallType(CharSequence template) {
        if (template != null) {
            return template.equals("android.app.Notification$CallStyle");
        }
        return false;
    }
}
