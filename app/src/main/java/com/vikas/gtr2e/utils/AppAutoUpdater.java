package com.vikas.gtr2e.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.vikas.gtr2e.apiInterfaces.GitHubApiService;
import com.vikas.gtr2e.beans.github.GithubRelease;
import com.vikas.gtr2e.beans.github.ReleaseAssets;

import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppAutoUpdater {

    private static final String TAG = "AppAutoUpdater";

    public interface AppUpdateListener {
        void onUpdateAvailable(String currentVersion, String latestVersion, String updateUrl);
    }

    public static void checkForUpdates(Context context, AppUpdateListener listener) {
        GitHubApiService apiService = RetrofitClient.getApiService();
        apiService.getReleases().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<GithubRelease>> call, @NonNull Response<List<GithubRelease>> response) {
                String latestVersion = "";
                String updateUrl = "";
                if (response.isSuccessful() && response.body() != null) {
                    List<GithubRelease> releases = response.body();
                    for (GithubRelease release : releases) {
                        Log.d(TAG, "Release Tag: " + release.getTagName());
                        Log.d(TAG, "Release Name: " + release.getName());
                        if(isNewVersionAvailable(latestVersion.isEmpty()?getAppVersionName(context):latestVersion, release.getTagName())) {
                            if (release.getAssets() != null) {
                                for (ReleaseAssets asset : release.getAssets()) {
                                    if(asset.getContentType().equals("application/vnd.android.package-archive")) {
                                        Log.d(TAG, "***** Update Available *****");
                                        Log.d(TAG, asset.getName() + " - " + asset.getBrowserDownloadUrl());
                                        latestVersion = release.getTagName().replaceAll("version","").replaceAll("v","");
                                        updateUrl = asset.getBrowserDownloadUrl();
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Error fetching releases: Code: " + response.code());
                }
                if(!latestVersion.isEmpty()) {
                    listener.onUpdateAvailable(getAppVersionName(context), latestVersion, updateUrl);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<GithubRelease>> call, @NonNull Throwable t) {
                Log.e(TAG+"-API Failure", Objects.requireNonNull(t.getMessage()));
            }
        });
    }

    public static String getAppVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app version name", e);
            return "0";
        }
    }

    public static boolean isNewVersionAvailable(String currentVersion, String latestGitHubVersion) {
        try {
            String cleanLatest = latestGitHubVersion.toLowerCase().replaceAll("version","");
            cleanLatest = cleanLatest.replaceAll("v","");

            String[] currentParts = currentVersion.split("\\.");
            String[] latestParts = cleanLatest.split("\\.");

            // Compare each part numerically
            for (int i = 0; i < Math.max(currentParts.length, latestParts.length); i++) {
                int current = (i < currentParts.length) ? Integer.parseInt(currentParts[i]) : 0;
                int latest = (i < latestParts.length) ? Integer.parseInt(latestParts[i]) : 0;

                if (latest > current) {
                    return true; // Newer version available
                } else if (latest < current) {
                    return false; // Current is newer
                }
            }
            return false;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing version numbers", e);
            return false;
        }
    }

}
