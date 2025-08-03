package com.vikas.gtr2e;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.util.Log;

import com.vikas.gtr2e.beans.DeviceInfo;
import com.vikas.gtr2e.databinding.ActivityMainBinding;
import com.vikas.gtr2e.utils.AppAutoUpdater;
import com.vikas.gtr2e.utils.Prefs;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int BLUETOOTH_REQUEST_CODE = 2;
    public static final String TAG = "MAIN_ACTIVITY";
    private static final int REQUEST_INSTALL_PERMISSION = 3;

    private BluetoothAdapter bluetoothAdapter;
    private GTR2eManager gtr2eManager;

    ActivityMainBinding binding;
    MediaPlayer mediaPlayer;

    //Flags
    boolean programmaticallyChangingHeartRateSwitch = false;
    boolean isProgrammaticallyChangingLiftToWakeSwitch = false;
    boolean isProgrammaticallyChangingKeepRunningInBackground = false;


    //Updates
    Uri pendingInstallUri = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkPermissions();
        initViews();
        initBluetooth();
        initGTR2eManager();
        AppAutoUpdater.checkForUpdates(this, this::showUpdateDialog);
    }


    private void initViews() {
        binding.connectDeviceButton.setOnClickListener(v -> connectToDevice());
        binding.watchBatteryProgress.setProgress(0f);
        binding.doNotDisturbBtn.setOnClickListener(v->enableDoNotDisturb());
        binding.findWatchButton.setOnClickListener(v -> {
            if (gtr2eManager != null) gtr2eManager.performAction("FIND_WATCH_START");
        });
        binding.volumeBar.setOnSeekBarChangeListener(volumeChangeHandler);
        binding.continuousHeartRateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(programmaticallyChangingHeartRateSwitch) {
                programmaticallyChangingHeartRateSwitch = false;
                return;
            }
            if (gtr2eManager == null) return;
            if (isChecked) {
                gtr2eManager.performAction("HEART_RATE_MONITORING_ON");
                binding.batteryPercentLabel.setVisibility(View.INVISIBLE);
            } else {
                gtr2eManager.performAction("HEART_RATE_MONITORING_OFF");
                binding.batteryPercentLabel.setVisibility(View.VISIBLE);
                updateDeviceInfo();
            }
        });
        binding.liftToWakeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isProgrammaticallyChangingLiftToWakeSwitch) {
                isProgrammaticallyChangingLiftToWakeSwitch = false;
                return;
            }
            if (gtr2eManager == null) return;
            if (isChecked) {
                gtr2eManager.performAction("LIFT_WRIST_TO_WAKE_ON");
            } else {
                gtr2eManager.performAction("LIFT_WRIST_TO_WAKE_OFF");
            }
        });
        binding.testBtn.setOnClickListener(v->{
            if (gtr2eManager != null) gtr2eManager.performAction("TEST");
        });

        isProgrammaticallyChangingKeepRunningInBackground = true;
        binding.keepRunningInBgSwitch.setChecked(Prefs.getKeepServiceRunningInBG(this));
        isProgrammaticallyChangingKeepRunningInBackground = false;
        binding.keepRunningInBgSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isProgrammaticallyChangingKeepRunningInBackground) {
                isProgrammaticallyChangingKeepRunningInBackground = false;
                return;
            }
            Prefs.setKeepServiceRunningInBG(MainActivity.this, isChecked);
        });
    }

    private void initBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    private void initGTR2eManager() {
        gtr2eManager = GTR2eManager.getInstance(this);
        GTR2eManager.ConnectionListener connectionListener = new GTR2eManager.ConnectionListener() {

            @Override
            public void onBackgroundServiceBound(boolean bound) {
                if(bound) {
                    if (gtr2eManager.isConnected()) {
                        watchConnectionStatusChanged(true);
                        if (gtr2eManager.isAuthenticated()) {
                            watchAuthenticatedStatusChanged();
                        }
                    } else {
                        watchConnectionStatusChanged(false);
                    }
                } else {
                    watchConnectionStatusChanged(false);
                    Log.e(TAG, "Background service not bound");
                }
            }

            @Override
            public void onConnectedChanged(boolean connected) {
                watchConnectionStatusChanged(connected);
            }

            @Override
            public void onAuthenticated() {
                watchAuthenticatedStatusChanged();
            }

            @Override
            public void onBatteryInfoUpdated(HuamiBatteryInfo batteryInfo) {
                 DeviceInfo currentDeviceInfo = gtr2eManager.getDeviceInfo();
                 if (currentDeviceInfo == null) {
                    Log.e(TAG, "DeviceInfo is null in onBatteryInfoUpdated"); return;
                 }
                runOnUiThread(() -> updateDeviceInfo());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.tvStatus.setText(MessageFormat.format("Error: {0}", error));
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onHeartRateChanged(int heartRate) {
                runOnUiThread(() -> binding.watchHeartRateText.setText(MessageFormat.format("{0}", heartRate)));
            }

            @Override
            public void onHeartRateMonitoringChanged(boolean enabled) {
                if(enabled) {
                    runOnUiThread(() -> {
                        binding.watchHeartRateIcon.setVisibility(View.VISIBLE);
                        binding.watchHeartRateText.setVisibility(View.VISIBLE);
                        programmaticallyChangingHeartRateSwitch = true;
                        binding.continuousHeartRateSwitch.setChecked(true);
                    });
                } else {
                    runOnUiThread(() -> {
                        binding.watchHeartRateIcon.setVisibility(View.INVISIBLE);
                        binding.watchHeartRateText.setVisibility(View.INVISIBLE);
                        programmaticallyChangingHeartRateSwitch = true;
                        binding.continuousHeartRateSwitch.setChecked(false);
                        binding.batteryPercentLabel.setVisibility(View.VISIBLE);
                        updateDeviceInfo();
                    });
                }
            }

            @Override
            public void findPhoneStateChanged(boolean started) {
                runOnUiThread(()->{
                    if(started) {
                        initFindPhoneTone();
                        if (mediaPlayer != null) mediaPlayer.start();
                        binding.blackBg.setImageResource(R.drawable.find_phone);
                        binding.chargingIndicatorImgView.setVisibility(View.INVISIBLE);
                        binding.tvStatus.setText("Finding Phone...");
                        binding.watchHeartRateIcon.setVisibility(View.INVISIBLE);
                        binding.watchHeartRateText.setVisibility(View.INVISIBLE);
                        binding.batteryPercentLabel.setVisibility(View.INVISIBLE);
                        binding.watchBatteryProgress.setVisibility(View.INVISIBLE);
                    } else {
                        if (mediaPlayer != null) mediaPlayer.stop();
                        binding.blackBg.setImageResource(R.drawable.gtr_bg);
                        updateDeviceInfo();
                    }
                });
            }

            @Override
            public void pendingBleProcessChanged(int count) {
                runOnUiThread(()->{
                    if(count>0) {
                        binding.pendingProcessLabel.setText(MessageFormat.format("{0}", count));
                    } else {
                        binding.pendingProcessLabel.setText("0");
                    }
                });
            }

            @Override
            public void onDeviceInfoChanged(DeviceInfo deviceInfo) {
                runOnUiThread(()-> updateDeviceInfo());
            }


        };
        gtr2eManager.setConnectionListener(connectionListener);
        gtr2eManager.onMainActivityResumed();
        updateDeviceInfo();
    }

    private void watchAuthenticatedStatusChanged() {
        DeviceInfo currentDeviceInfo = gtr2eManager.getDeviceInfo();
        if (currentDeviceInfo == null) {
             Log.e(TAG, "DeviceInfo is null in onAuthenticated");
            return;
        }
        runOnUiThread(this::updateDeviceInfo);
    }

    private void watchConnectionStatusChanged(boolean connected) {
        DeviceInfo currentDeviceInfo = gtr2eManager.getDeviceInfo();
        if (currentDeviceInfo == null) {
            Log.e(TAG, "DeviceInfo is null in onConnectedChanged");
            binding.tvStatus.setText(connected ? "Connected (No DeviceInfo)" : "Disconnected (No DeviceInfo)");
             if (!connected) updateDeviceInfo();
            return;
        }
        if(connected) {
            runOnUiThread(() -> {
                binding.tvStatus.setText("Connected to GTR 2e");
                binding.connectDeviceButton.setIconResource(R.drawable.rounded_bluetooth_disabled_24);
                binding.connectDeviceButton.setText("Disconnect");
            });
        } else {
            Log.d(TAG, "onDisconnected() called in MainActivity");
            runOnUiThread(() -> {
                binding.connectDeviceButton.setIconResource(R.drawable.rounded_bluetooth_connected_24);
                binding.connectDeviceButton.setText("Connect");
                updateDeviceInfo();
            });
        }
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();

        Collections.addAll(permissions,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.MODIFY_AUDIO_SETTINGS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
        requestNotificationPolicyAccess();
    }
    private void requestNotificationPolicyAccess() {
        if (!isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }
    }

    private void connectToDevice() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth permission not granted", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivityForResult(enableBtIntent, BLUETOOTH_REQUEST_CODE);
            return;
        }

        if (gtr2eManager == null) {
            Log.e(TAG, "gtr2eManager is null in connectToDevice");
            return;
        }

        if (gtr2eManager.isConnected()) {
            gtr2eManager.disconnect();
        } else {
            binding.blutoothStatusIndicatorImgView.setImageResource(R.drawable.rounded_bluetooth_searching_24);
            gtr2eManager.startScan();
        }
    }

    private void updateDeviceInfo() {
        if (gtr2eManager == null) {
            Log.w(TAG, "updateDeviceInfo called but gtr2eManager is null.");
            // Set UI to a default disconnected state
            binding.tvDeviceInfo.setText("No device connected (manager null)");
            animateProgressBar(0f);
            binding.batteryPercentLabel.setVisibility(View.INVISIBLE);
            binding.batteryPercentLabel.setText("0%");
            binding.tvStatus.setText("Disconnected");
            binding.chargingIndicatorImgView.setVisibility(View.INVISIBLE);
            binding.blutoothStatusIndicatorImgView.setImageResource(R.drawable.rounded_bluetooth_24);
            binding.continuousHeartRateSwitch.setChecked(false);
            binding.continuousHeartRateSwitch.setEnabled(false);
            binding.findWatchButton.setEnabled(false);
            binding.liftToWakeSwitch.setEnabled(false);
            return;
        }

        DeviceInfo currentDeviceInfo = gtr2eManager.getDeviceInfo();

        if (currentDeviceInfo != null && currentDeviceInfo.isConnected()) {
            StringBuilder info = buildDeviceInfoString(currentDeviceInfo); // Pass currentDeviceInfo
            animateProgressBar(currentDeviceInfo.getBatteryPercentage());
            binding.batteryPercentLabel.setText(MessageFormat.format("{0}%", currentDeviceInfo.getBatteryPercentage()));
            binding.batteryPercentLabel.setVisibility(View.VISIBLE);
            binding.tvDeviceInfo.setText(info.toString());
            binding.blutoothStatusIndicatorImgView.setImageResource(R.drawable.rounded_bluetooth_connected_24);
            if(currentDeviceInfo.isCharging()) {
                binding.chargingIndicatorImgView.setVisibility(View.VISIBLE);
            } else {
                binding.chargingIndicatorImgView.setVisibility(View.INVISIBLE);
            }
            binding.continuousHeartRateSwitch.setEnabled(true);
            binding.findWatchButton.setEnabled(true);
            binding.liftToWakeSwitch.setEnabled(true);
            binding.stepsCountLabel.setText(String.format(Locale.ENGLISH,"%d",currentDeviceInfo.getSteps()));
        } else {
            binding.tvDeviceInfo.setText("No device connected");
            animateProgressBar(0f);
            binding.batteryPercentLabel.setVisibility(View.INVISIBLE);
            binding.batteryPercentLabel.setText("0%");
            binding.tvStatus.setText("Disconnected");
            binding.chargingIndicatorImgView.setVisibility(View.INVISIBLE);
            binding.blutoothStatusIndicatorImgView.setImageResource(R.drawable.rounded_bluetooth_24);
            binding.continuousHeartRateSwitch.setChecked(false);
            binding.continuousHeartRateSwitch.setEnabled(false);
            binding.findWatchButton.setEnabled(false);
            binding.liftToWakeSwitch.setEnabled(false);
             if (currentDeviceInfo == null) {
                 Log.w(TAG, "updateDeviceInfo: currentDeviceInfo is null, UI set to disconnected.");
                 binding.stepsCountLabel.setText("N/a");
             }
        }
    }

    @NonNull
    private StringBuilder buildDeviceInfoString(DeviceInfo deviceInfoToDisplay) { // Accept DeviceInfo as parameter
        StringBuilder info = new StringBuilder();
        if (deviceInfoToDisplay == null) {
            info.append("Device information not available.");
            return info;
        }
        if (deviceInfoToDisplay.getDeviceName() != null && !deviceInfoToDisplay.getDeviceName().isEmpty()) info.append("Device Name: ").append(deviceInfoToDisplay.getDeviceName()).append("\n");
        if (deviceInfoToDisplay.getSerialNumber() != null && !deviceInfoToDisplay.getSerialNumber().isEmpty()) info.append("Serial Number: ").append(deviceInfoToDisplay.getSerialNumber()).append("\n");
        if (deviceInfoToDisplay.getHardwareRevision() != null && !deviceInfoToDisplay.getHardwareRevision().isEmpty()) info.append("Hardware Revision: ").append(deviceInfoToDisplay.getHardwareRevision()).append("\n");
        if (deviceInfoToDisplay.getSoftwareRevision() != null && !deviceInfoToDisplay.getSoftwareRevision().isEmpty()) info.append("Software Revision: ").append(deviceInfoToDisplay.getSoftwareRevision()).append("\n");
        // if (!deviceInfoToDisplay.getSystemId().isEmpty()) info.append("System ID: ").append(deviceInfoToDisplay.getSystemId()).append("\n"); // Assuming getSystemId might not exist or be relevant now
        // if (!deviceInfoToDisplay.getPnpId().isEmpty()) info.append("PnP ID: ").append(deviceInfoToDisplay.getPnpId()).append("\n"); // Assuming getPnpId might not exist or be relevant now
        info.append("Battery: ").append(deviceInfoToDisplay.getBatteryPercentage()).append("%\n");
        info.append("Charging: ").append(deviceInfoToDisplay.getChargingStatus()).append("\n");
        info.append("Authenticated: ").append(deviceInfoToDisplay.isAuthenticated() ? "Yes" : "No");
        return info;
    }

    private void enableDoNotDisturb(){
        if(gtr2eManager!=null) gtr2eManager.performAction("DO_NOT_DISTURB_ON");
    }

    private void animateProgressBar(float toProgress) {
        ObjectAnimator animation = ObjectAnimator.ofFloat(binding.watchBatteryProgress, "progress", toProgress);
        animation.setDuration(1000); // 1 second
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.start();
    }

    private void initFindPhoneTone(){
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        // Initialize MediaPlayer with the tone
        mediaPlayer = MediaPlayer.create(this, R.raw.findphone);
        mediaPlayer.setLooping(true); // Loop indefinitely
    }

    private void initHeartBeatTone(){
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        // Initialize MediaPlayer with the tone
        mediaPlayer = MediaPlayer.create(this, R.raw.hr_reminder_beep);
        mediaPlayer.setLooping(false); // Loop indefinitely
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                connectToDevice(); // Retry connection if Bluetooth was enabled
            } else {
                Toast.makeText(this, "Bluetooth not enabled, cannot connect.", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == REQUEST_INSTALL_PERMISSION && resultCode == RESULT_OK) {
            // User granted permission, retry installation
            installApk(pendingInstallUri);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gtr2eManager != null) {
            gtr2eManager.unbindServiceIfNeeded();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private boolean isNotificationPolicyAccessGranted() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return notificationManager.isNotificationPolicyAccessGranted();
    }

    private void showUpdateDialog(String currentVersion, String latestVersion, String downloadUrl) {
        new AlertDialog.Builder(this)
                .setTitle("New Version Available")
                .setMessage(String.format("Version %s is available.Current version is %s, Would you like to update now?", latestVersion, currentVersion))
                .setPositiveButton("Update", (dialog, which) -> {
                    // Start download
                    downloadAndInstallApk(downloadUrl);
                })
                .setNegativeButton("Later", null)
                .setCancelable(false)
                .show();
    }

    private void downloadAndInstallApk(String apkUrl) {
        // Create download request
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle("App Update");
        request.setDescription("Downloading new version...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app-update.apk");

        // Get download service and enqueue file
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = manager.enqueue(request);

        // Register receiver to install when download completes
        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    installApk(manager.getUriForDownloadedFile(downloadId));
                    context.unregisterReceiver(this);
                }
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
        } else {
            ContextCompat.registerReceiver(this, onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
        }
    }

    private void installApk(Uri apkUri) {
        if (!getPackageManager().canRequestPackageInstalls()) {
            // Store the URI for later use
            this.pendingInstallUri = apkUri;
            startActivityForResult(
                    new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse("package:" + getPackageName())),
                    REQUEST_INSTALL_PERMISSION);
            return;
        }

        // The filename used when setting the download destination
        String downloadedApkFilename = "app-update.apk";
        File apkFileOnDisk = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), downloadedApkFilename);
        if(!apkFileOnDisk.exists()) {
            return;
        }
        apkUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileProvider",
                apkFileOnDisk);

        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(apkUri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(install);
    }


    //region Seekbar change handlers
    private final SeekBar.OnSeekBarChangeListener volumeChangeHandler = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (gtr2eManager != null) gtr2eManager.performAction("SET_PHONE_VOLUME", String.valueOf(progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };
    //endregion

}
