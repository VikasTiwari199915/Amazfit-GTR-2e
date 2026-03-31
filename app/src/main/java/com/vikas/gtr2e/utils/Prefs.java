package com.vikas.gtr2e.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.vikas.gtr2e.ZeppLoginActivity;

/**
 * Utility class for shared preferences
 *
 * @author Vikas Tiwari
 */
public class Prefs {
    public static final String PREF_KEEP_SERVICE_RUNNING_IN_BG = "keepServiceRunningInBG";
    public static final String PREF_LAST_DEVICE_MAC = "lastDeviceMac";
    public static final String PREF_AUTH_KEY = "authKey";
    public static final String PREF_DEVICE_ADDED = "deviceAdded";
    public static final String PREF_LAST_DEVICE_ASSOCIATION = "PREF_LAST_DEVICE_ASSOCIATION";
    public static final String PREF_ENABLE_VOIP_ALERTS = "enable_voip_call_alerts";
    public static final String PREF_AUTO_APP_UPDATES =  "autoCheckForAppUpdates";
    public static final String IS_ZEPP_ACCOUNT_LOGIN = "zeppAccountLogin";
    public static final String ZEPP_COUNTRY_CODE = "zeppCountryCode";
    public static final String ZEPP_REGION = "zeppRegion";
    public static final String ZEPP_USER_ID = "zeppUserId";
    public static final String ZEPP_LOGIN_TOKEN = "zeppLoginToken";
    public static final String ZEPP_APP_TOKEN = "zeppAppToken";


    public static boolean getDeviceAdded(Context context) {
        return getPrefs(context).getBoolean(PREF_DEVICE_ADDED, false);
    }

    public static void setDeviceAdded(Context context, boolean deviceAdded) {
        getPrefs(context).edit().putBoolean(PREF_DEVICE_ADDED, deviceAdded).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
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

    public static boolean getVoipCallAlertsEnabled(Context context) {
        return getPrefs(context).getBoolean(PREF_ENABLE_VOIP_ALERTS, true);
    }

    public static boolean getAutoAppUpdatesEnabled(Context context) {
        return getPrefs(context).getBoolean(PREF_AUTO_APP_UPDATES, true);
    }

    public static void setZeppAppToken(Context context, String appToken) {
        getPrefs(context).edit().putString(ZEPP_APP_TOKEN, appToken).apply();
    }
    public static String getZeppAppToken(Context context) {
        return getPrefs(context).getString(ZEPP_APP_TOKEN, "");
    }

    public static void setZeppLoginToken(Context context, String loginToken) {
        getPrefs(context).edit().putString(ZEPP_LOGIN_TOKEN, loginToken).apply();
    }
    public static String getZeppLoginToken(Context context) {
        return getPrefs(context).getString(ZEPP_LOGIN_TOKEN, "");
    }

    public static void setZeppUserId(Context context, String userId) {
        getPrefs(context).edit().putString(ZEPP_USER_ID, userId).apply();
    }
    public static String getZeppUserId(Context context) {
        return getPrefs(context).getString(ZEPP_USER_ID, "");
    }

    public static void setZeppRegion(Context context, String region) {
        getPrefs(context).edit().putString(ZEPP_REGION, region).apply();
    }
    public static String getZeppRegion(Context context) {
        return getPrefs(context).getString(ZEPP_REGION, "");
    }

    public static void setZeppCountryCode(Context context, String countryCode) {
        getPrefs(context).edit().putString(ZEPP_COUNTRY_CODE, countryCode).apply();
    }
    public static String getZeppCountryCode(Context context) {
        return getPrefs(context).getString(ZEPP_COUNTRY_CODE, "");
    }

    public static void setZeppAccountLogin(Context context, boolean zeppLogin) {
        getPrefs(context).edit().putBoolean(IS_ZEPP_ACCOUNT_LOGIN, zeppLogin).apply();
    }
    public static boolean getZeppAccountLogin(Context context) {
        return getPrefs(context).getBoolean(IS_ZEPP_ACCOUNT_LOGIN, false);
    }
}
