package com.vikas.gtr2e.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class for shared preferences
 * @author Vikas Tiwari
 */
public class Prefs {
    public static final String PREF_NAME = "prefs";
    public static final String PREF_KEEP_SERVICE_RUNNING_IN_BG = "keepServiceRunningInBG";
    public static final String PREF_LAST_DEVICE_MAC = "lastDeviceMac";
    public static final String PREF_AUTH_KEY = "authKey";
    public static final String PREF_DEVICE_ADDED = "deviceAdded";
    public static final String PREF_LAST_DEVICE_ASSOCIATION = "PREF_LAST_DEVICE_ASSOCIATION";


    public static boolean getDeviceAdded(Context context) {
        return getPrefs(context).getBoolean(PREF_DEVICE_ADDED, false);
    }
    public static void setDeviceAdded(Context context, boolean deviceAdded) {
        getPrefs(context).edit().putBoolean(PREF_DEVICE_ADDED, deviceAdded).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean getKeepServiceRunningInBG(Context context) {
        return getPrefs(context).getBoolean(PREF_KEEP_SERVICE_RUNNING_IN_BG, true);
    }

    public static void setKeepServiceRunningInBG(Context context, boolean keepServiceRunningInBG) {
        getPrefs(context).edit().putBoolean(PREF_KEEP_SERVICE_RUNNING_IN_BG, keepServiceRunningInBG).apply();
    }

    public static String getLastDeviceMac(Context context) {
        return getPrefs(context).getString(PREF_LAST_DEVICE_MAC, null);
    }

    public static void setLastDeviceMac(Context context, String mac) {
        getPrefs(context).edit().putString(PREF_LAST_DEVICE_MAC, mac).apply();
    }

    public static String getAuthKey(Context context) {
        return getPrefs(context).getString(PREF_AUTH_KEY, null);
    }

    public static void setAuthKey(Context context, String authKey) {
        getPrefs(context).edit().putString(PREF_AUTH_KEY, authKey).apply();
    }

    public static int getLastDeviceAssociationId(Context context) {
        return getPrefs(context).getInt(PREF_LAST_DEVICE_ASSOCIATION, -1);
    }

    public static void setLastDeviceAssociationId(Context context, int id) {
        getPrefs(context).edit().putInt(PREF_LAST_DEVICE_ASSOCIATION, id).apply();
    }
}
