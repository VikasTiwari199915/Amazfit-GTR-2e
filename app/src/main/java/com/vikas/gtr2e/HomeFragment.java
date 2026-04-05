package com.vikas.gtr2e;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.vikas.gtr2e.beans.DeviceInfo;
import com.vikas.gtr2e.beans.HuamiBatteryInfo;
import com.vikas.gtr2e.databinding.FragmentHomeBinding;
import com.vikas.gtr2e.interfaces.ConnectionListener;
import com.vikas.gtr2e.utils.AppAutoUpdater;
import com.vikas.gtr2e.utils.GTR2eManager;
import com.vikas.gtr2e.utils.MediaUtil;
import com.vikas.gtr2e.utils.Prefs;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    public static final String TAG = "HOME_FRAGMENT";
    FragmentHomeBinding binding;
    MediaPlayer mediaPlayer;
    //Flags
    boolean programmaticallyChangingHeartRateSwitch = false;
    boolean isProgrammaticallyChangingLiftToWakeSwitch = false;
    boolean isProgrammaticallyChangingKeepRunningInBackground = false;
    boolean deviceAddedJustNow = false;
    //Updates
    Uri pendingInstallUri = null;
    private BluetoothAdapter bluetoothAdapter;
    private GTR2eManager gtr2eManager;
    private String pendingVersionName;

    private final ActivityResultLauncher<Intent> bluetoothLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    connectToDevice();
                } else {
                    Toast.makeText(getContext(), "Bluetooth not enabled, cannot connect.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Log.e(TAG, "PERMISSION RESULT");
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    initBluetooth();
                    initGTR2eManager();
                }
            });

    private final ActivityResultLauncher<Intent> installPermissionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    installApk(pendingInstallUri, pendingVersionName);
                    this.pendingVersionName = null;
                }
            });


    public HomeFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getActivity() != null && getActivity().getIntent() != null) {
            deviceAddedJustNow = getActivity().getIntent().getBooleanExtra("DEVICE_ADDED_JUST_NOW", false);
        }

        initViews();
        if (!MediaUtil.isNotificationListenerEnabled(requireContext())) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        }
        if (checkPermissions()) {
            initBluetooth();
            initGTR2eManager();
        }
        if (Prefs.getAutoAppUpdatesEnabled(requireContext())) {
            AppAutoUpdater.checkForUpdates(requireContext(), this::showUpdateDialog);
        }
    }

    private void initViews() {
        Glide.with(requireContext())
                .load(Prefs.getLastSelectedWatchFaceImageUrl(requireContext()))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop()
                .into(binding.watchFaceImage);
        binding.connectDeviceButton.setOnClickListener(v -> connectToDevice());
        binding.connectDeviceButton.setOnLongClickListener(v -> {
            startActivity(new Intent(requireContext(), ZeppLoginActivity.class));
            return true;
        });

        binding.sideBatteryProgressBar.setProgress(0);
        binding.findWatchButton.setOnClickListener(v -> {
            if (gtr2eManager != null) gtr2eManager.performAction("FIND_WATCH_START");
        });
        binding.continuousHeartRateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (programmaticallyChangingHeartRateSwitch) {
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
            if (isProgrammaticallyChangingLiftToWakeSwitch) {
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

        isProgrammaticallyChangingKeepRunningInBackground = true;
        binding.keepRunningInBgSwitch.setChecked(Prefs.getKeepServiceRunningInBG(requireContext()));
        isProgrammaticallyChangingKeepRunningInBackground = false;
        binding.keepRunningInBgSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticallyChangingKeepRunningInBackground) {
                isProgrammaticallyChangingKeepRunningInBackground = false;
                return;
            }
            Prefs.setKeepServiceRunningInBG(requireContext(), isChecked);
        });
    }



    private void initBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    private void initGTR2eManager() {
        gtr2eManager = GTR2eManager.getInstance(requireActivity());
        ConnectionListener connectionListener = new ConnectionListener() {

            @Override
            public void onBackgroundServiceBound(boolean bound) {
                if (bound) {
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
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateDeviceInfo());
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        binding.tvStatus.setText(MessageFormat.format("Error: {0}", error));
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onHeartRateChanged(int heartRate) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> binding.watchHeartRateText.setText(MessageFormat.format("{0}", heartRate)));
                }
            }

            @Override
            public void onHeartRateMonitoringChanged(boolean enabled) {
                if (getActivity() != null) {
                    if (enabled) {
                        getActivity().runOnUiThread(() -> {
                            binding.watchHeartRateIcon.setVisibility(View.VISIBLE);
                            binding.watchHeartRateText.setVisibility(View.VISIBLE);
                            programmaticallyChangingHeartRateSwitch = true;
                            binding.continuousHeartRateSwitch.setChecked(true);
                        });
                    } else {
                        getActivity().runOnUiThread(() -> {
                            binding.watchHeartRateIcon.setVisibility(View.INVISIBLE);
                            binding.watchHeartRateText.setVisibility(View.INVISIBLE);
                            programmaticallyChangingHeartRateSwitch = true;
                            binding.continuousHeartRateSwitch.setChecked(false);
                            binding.batteryPercentLabel.setVisibility(View.VISIBLE);
                            updateDeviceInfo();
                        });
                    }
                }
            }

            @Override
            public void findPhoneStateChanged(boolean started) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (started) {
                            initFindPhoneTone();
                            if (mediaPlayer != null) mediaPlayer.start();
//                            binding.blackBg.setImageResource(R.drawable.find_phone);
                            binding.chargingIndicatorImgView.setVisibility(View.INVISIBLE);
                            binding.tvStatus.setText(R.string.finding_phone);
                            binding.watchHeartRateIcon.setVisibility(View.INVISIBLE);
                            binding.watchHeartRateText.setVisibility(View.INVISIBLE);
                            binding.batteryPercentLabel.setVisibility(View.INVISIBLE);
                        } else {
                            if (mediaPlayer != null) mediaPlayer.stop();
//                            binding.blackBg.setImageResource(R.drawable.gtr_bg);
                            updateDeviceInfo();
                        }
                    });
                }
            }

            @Override
            public void pendingBleProcessChanged(int count) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (count > 0) {
                            binding.pendingProcessLabel.setText(MessageFormat.format("{0}", count));
                        } else {
                            binding.pendingProcessLabel.setText("0");
                        }
                    });
                }
            }

            @Override
            public void onDeviceInfoChanged(DeviceInfo deviceInfo) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateDeviceInfo());
                }
            }

            @Override
            public void onWatchFaceSet(boolean success) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), success ? "Watch face set successfully" : "Failed to set watch face", Toast.LENGTH_SHORT).show();
                    });
                }
            }


        };
        gtr2eManager.setConnectionListener(connectionListener);
        gtr2eManager.onMainActivityResumed();
        updateDeviceInfo();
        if (deviceAddedJustNow) {
            connectToDevice();
            deviceAddedJustNow = false;
        }
    }

    private void watchAuthenticatedStatusChanged() {
        if (gtr2eManager == null || gtr2eManager.getDeviceInfo() == null) {
            Log.e(TAG, "DeviceInfo is null in onAuthenticated");
            return;
        }
        if (getActivity() != null) {
            getActivity().runOnUiThread(this::updateDeviceInfo);
        }
    }

    private void watchConnectionStatusChanged(boolean connected) {
        if (gtr2eManager == null || gtr2eManager.getDeviceInfo() == null) {
            Log.e(TAG, "DeviceInfo is null in onConnectedChanged");
            binding.tvStatus.setText(connected ? "Connected (No DeviceInfo)" : "Disconnected (No DeviceInfo)");
            if (!connected) updateDeviceInfo();
            return;
        }
        if (getActivity() != null) {
            if (connected) {
                getActivity().runOnUiThread(() -> {
                    binding.tvStatus.setText(R.string.connected_to_gtr_2e);
                    binding.connectDeviceButton.setIconResource(R.drawable.rounded_bluetooth_disabled_24);
                    binding.connectDeviceButton.setText(R.string.disconnect);
                });
            } else {
                Log.d(TAG, "onDisconnected() called in HomeFragment");
                getActivity().runOnUiThread(() -> {
                    binding.connectDeviceButton.setIconResource(R.drawable.rounded_bluetooth_connected_24);
                    binding.connectDeviceButton.setText(R.string.connect);
                    updateDeviceInfo();
                });
            }
        }
    }

    private boolean checkPermissions() {
        List<String> permissions = new ArrayList<>();

        Collections.addAll(permissions,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.ACCESS_NOTIFICATION_POLICY
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> missingPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toArray(new String[0]));
            return false;
        }

        requestNotificationPolicyAccess();
        return true;
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
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Bluetooth permission not granted", Toast.LENGTH_SHORT).show();
                return;
            }
            bluetoothLauncher.launch(enableBtIntent);
            return;
        }

        if (gtr2eManager == null) {
            Log.e(TAG, "gtr2eManager is null in connectToDevice");
            return;
        }

        if (gtr2eManager.isConnected()) {
            gtr2eManager.disconnect();
            if (gtr2eManager.getDeviceInfo() != null) {
                gtr2eManager.getDeviceInfo().setForceDisconnected(true);
            }
        } else {
            binding.blutoothStatusIndicatorImgView.setImageResource(R.drawable.rounded_bluetooth_searching_24);
            if (gtr2eManager.getDeviceInfo() != null) {
                gtr2eManager.getDeviceInfo().setForceDisconnected(false);
            }
            gtr2eManager.startScan();
        }
    }

    private void updateDeviceInfo() {
        if (gtr2eManager == null) {
            Log.w(TAG, "updateDeviceInfo called but gtr2eManager is null.");
            // Set UI to a default disconnected state
            binding.tvDeviceInfo.setText(R.string.no_device_connected_manager_is_null);
            animateProgressBar(0);
            binding.batteryPercentLabel.setText("0%");
            binding.tvStatus.setText(R.string.disconnected);
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
            StringBuilder info = buildDeviceInfoString(currentDeviceInfo);
            animateProgressBar(currentDeviceInfo.getBatteryPercentage());
            binding.batteryPercentLabel.setText(MessageFormat.format("{0}%", currentDeviceInfo.getBatteryPercentage()));
            binding.tvDeviceInfo.setText(info.toString());
            binding.blutoothStatusIndicatorImgView.setImageResource(R.drawable.rounded_bluetooth_connected_24);
            if (currentDeviceInfo.isCharging()) {
                binding.chargingIndicatorImgView.setVisibility(View.VISIBLE);
            } else {
                binding.chargingIndicatorImgView.setVisibility(View.INVISIBLE);
            }
            binding.continuousHeartRateSwitch.setEnabled(true);
            binding.findWatchButton.setEnabled(true);
            binding.liftToWakeSwitch.setEnabled(true);
            binding.stepsCountLabel.setText(String.format(Locale.ENGLISH, "%d", currentDeviceInfo.getSteps()));
        } else {
            binding.tvDeviceInfo.setText(R.string.no_device_connected);
            animateProgressBar(0);
            binding.batteryPercentLabel.setText("0%");
            binding.tvStatus.setText(R.string.disconnected);
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
    private StringBuilder buildDeviceInfoString(DeviceInfo deviceInfoToDisplay) {
        StringBuilder info = new StringBuilder();
        if (deviceInfoToDisplay == null) {
            info.append("Device information not available.");
            return info;
        }
        if (deviceInfoToDisplay.getDeviceName() != null && !deviceInfoToDisplay.getDeviceName().isEmpty())
            info.append("Device Name: ").append(deviceInfoToDisplay.getDeviceName()).append("\n");
        if (deviceInfoToDisplay.getSerialNumber() != null && !deviceInfoToDisplay.getSerialNumber().isEmpty())
            info.append("Serial Number: ").append(deviceInfoToDisplay.getSerialNumber()).append("\n");
        if (deviceInfoToDisplay.getHardwareRevision() != null && !deviceInfoToDisplay.getHardwareRevision().isEmpty())
            info.append("Hardware Revision: ").append(deviceInfoToDisplay.getHardwareRevision()).append("\n");
        if (deviceInfoToDisplay.getSoftwareRevision() != null && !deviceInfoToDisplay.getSoftwareRevision().isEmpty())
            info.append("Software Revision: ").append(deviceInfoToDisplay.getSoftwareRevision()).append("\n");
        info.append("Battery: ").append(deviceInfoToDisplay.getBatteryPercentage()).append("%\n");
        info.append("Charging: ").append(deviceInfoToDisplay.getChargingStatus()).append("\n");
        info.append("Authenticated: ").append(deviceInfoToDisplay.isAuthenticated() ? "Yes" : "No");
        return info;
    }

    private void animateProgressBar(int toProgress) {
        requireActivity().runOnUiThread(() -> {
            ObjectAnimator animation = ObjectAnimator.ofInt(binding.sideBatteryProgressBar, getString(R.string.progress), toProgress);
            animation.setDuration(1000); // 1 second
            animation.setInterpolator(new AccelerateDecelerateInterpolator());
            animation.start();
        });

    }

    private void initFindPhoneTone() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.findphone);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
        }
    }

    @Override
    public void onDestroy() {
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
        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        return notificationManager != null && notificationManager.isNotificationPolicyAccessGranted();
    }

    private void showUpdateDialog(String currentVersion, String latestVersion, String downloadUrl) {
        new AlertDialog.Builder(requireContext())
                .setTitle("New Version Available")
                .setMessage(String.format("Version %s is available.Current version is %s, Would you like to update now?", latestVersion, currentVersion))
                .setPositiveButton("Update", (dialog, which) -> downloadAndInstallApk(downloadUrl, latestVersion))
                .setNegativeButton("Later", null)
                .setCancelable(false)
                .show();
    }

    private void downloadAndInstallApk(String apkUrl, String versionName) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle("App Update");
        request.setDescription("Downloading new version...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "gtr2e-" + versionName + ".apk");

        DownloadManager manager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) return;
        long downloadId = manager.enqueue(request);

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    installApk(manager.getUriForDownloadedFile(downloadId), versionName);
                    context.unregisterReceiver(this);
                }
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
        } else {
            ContextCompat.registerReceiver(requireContext(), onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
        }
    }

    private void installApk(Uri apkUri, String versionName) {
        if (!requireContext().getPackageManager().canRequestPackageInstalls()) {
            this.pendingInstallUri = apkUri;
            this.pendingVersionName = versionName;
            installPermissionLauncher.launch(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse("package:" + requireContext().getPackageName())));
            return;
        }
        String downloadedApkFilename = "gtr2e-" + versionName + ".apk";
        File apkFileOnDisk = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), downloadedApkFilename);
        if (!apkFileOnDisk.exists()) {
            return;
        }
        apkUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileProvider",
                apkFileOnDisk);

        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(apkUri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(install);
    }

}
