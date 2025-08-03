package com.vikas.gtr2e.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    public static final String PREF_NAME = "prefs";
    public static final String PREF_KEEP_SERVICE_RUNNING_IN_BG = "keepServiceRunningInBG";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean getKeepServiceRunningInBG(Context context) {
        return getPrefs(context).getBoolean(PREF_KEEP_SERVICE_RUNNING_IN_BG, true);
    }

    public static void setKeepServiceRunningInBG(Context context, boolean keepServiceRunningInBG) {
        getPrefs(context).edit().putBoolean(PREF_KEEP_SERVICE_RUNNING_IN_BG, keepServiceRunningInBG).apply();
    }



}
