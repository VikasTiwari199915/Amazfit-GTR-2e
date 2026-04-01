package com.vikas.gtr2e.interfaces;

public interface AppUpdateListener {
    void onUpdateAvailable(String currentVersion, String latestVersion, String updateUrl);
}