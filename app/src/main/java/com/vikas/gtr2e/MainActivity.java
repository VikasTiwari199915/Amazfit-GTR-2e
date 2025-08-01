package com.vikas.gtr2e;

import android.animation.ObjectAnimator;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import com.vikas.gtr2e.beans.DeviceInfo;
import com.vikas.gtr2e.databinding.ActivityMainBinding;

import java.text.MessageFormat;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int BLUETOOTH_REQUEST_CODE = 2;

    private BluetoothAdapter bluetoothAdapter;
    private GTR2eManager gtr2eManager;

    private final DeviceInfo deviceInfo = new DeviceInfo();

    ActivityMainBinding binding;
    MediaPlayer mediaPlayer;

    //Flags
    boolean programmaticallyChangingHeartRateSwitch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        initBluetooth();
        initGTR2eManager();
        checkPermissions();
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

    private void initViews() {
        binding.connectDeviceButton.setOnClickListener(v -> connectToDevice());
        binding.watchBatteryProgress.setProgress(0f);
        binding.doNotDisturbBtn.setOnClickListener(v->enableDoNotDisturb());
        binding.findWatchButton.setOnClickListener(v -> gtr2eManager.performAction("FIND_WATCH_START"));
        binding.volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                gtr2eManager.performAction("SET_PHONE_VOLUME", String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        binding.continuousHeartRateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(programmaticallyChangingHeartRateSwitch) {
                programmaticallyChangingHeartRateSwitch = false;
                return;
            }
            if (isChecked) {
                gtr2eManager.performAction("HEART_RATE_MONITORING_ON");
            } else {
                gtr2eManager.performAction("HEART_RATE_MONITORING_OFF");
            }
        });
    }

    private void initBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    private void initGTR2eManager() {
        gtr2eManager = new GTR2eManager(this);
        gtr2eManager.setConnectionListener(new GTR2eManager.ConnectionListener() {

            @Override
            public void onConnectedChanged(boolean connected) {
                if(connected) {
                    runOnUiThread(() -> {
                        binding.tvStatus.setText("Connected to GTR 2e");
                        binding.connectDeviceButton.setIconResource(R.drawable.rounded_bluetooth_disabled_24);
                        binding.connectDeviceButton.setText("Disconnect");
                        deviceInfo.setConnected(true);
                    });
                } else {
                    Log.d("MainActivity", "onDisconnected() called");
                    runOnUiThread(() -> {
                        deviceInfo.setConnected(false);
                        deviceInfo.setAuthenticated(false);
                        deviceInfo.updateBatteryInfo(null);
                        binding.connectDeviceButton.setIconResource(R.drawable.rounded_bluetooth_connected_24);
                        binding.connectDeviceButton.setText("Connect");
                        updateDeviceInfo();
                    });
                }
            }
            
            @Override
            public void onAuthenticated() {
                runOnUiThread(() -> {
                    deviceInfo.setAuthenticated(true);
                    Toast.makeText(MainActivity.this, "Authentication successful!", Toast.LENGTH_SHORT).show();
                    updateDeviceInfo();
                });
            }
            
            @Override
            public void onBatteryInfoUpdated(HuamiBatteryInfo batteryInfo) {
                runOnUiThread(() -> {
                    deviceInfo.updateBatteryInfo(batteryInfo);
                    updateDeviceInfo();
                });
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
                runOnUiThread(() -> {
                    binding.watchHeartRateText.setText(MessageFormat.format("{0}", heartRate));
                });
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
                    });
                }
            }

            @Override
            public void findPhoneStateChanged(boolean started) {
                runOnUiThread(()->{
                    if(started) {
                        initFindPhoneTone();
                        mediaPlayer.start();
                        binding.blackBg.setImageResource(R.drawable.find_phone);
                        binding.chargingIndicatorImgView.setVisibility(View.INVISIBLE);
                        binding.tvStatus.setText("Finding Phone...");
                        binding.watchHeartRateIcon.setVisibility(View.INVISIBLE);
                        binding.watchHeartRateText.setVisibility(View.INVISIBLE);
                        binding.batteryPercentLabel.setVisibility(View.INVISIBLE);
                        binding.watchBatteryProgress.setVisibility(View.INVISIBLE);
                        binding.chargingIndicatorImgView.setVisibility(View.INVISIBLE);
                    } else {
                        mediaPlayer.stop();
                        binding.blackBg.setImageResource(R.drawable.gtr_bg);
                        updateDeviceInfo();
                    }
                });
            }
        });
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void connectToDevice() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth permission not granted", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivityForResult(enableBtIntent, BLUETOOTH_REQUEST_CODE);
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
        if (gtr2eManager.isConnected()) {
            StringBuilder info = buildDeviceInfoString();
            animateProgressBar(deviceInfo.getBatteryPercentage());
            binding.batteryPercentLabel.setText(MessageFormat.format("{0}%", deviceInfo.getBatteryPercentage()));
            binding.batteryPercentLabel.setVisibility(View.VISIBLE);
            binding.tvDeviceInfo.setText(info.toString());
            binding.blutoothStatusIndicatorImgView.setImageResource(R.drawable.rounded_bluetooth_connected_24);
            if(deviceInfo.isCharging()) {
                binding.chargingIndicatorImgView.setVisibility(View.VISIBLE);
            } else {
                binding.chargingIndicatorImgView.setVisibility(View.INVISIBLE);
            }
            binding.continuousHeartRateSwitch.setEnabled(true);
            binding.findWatchButton.setEnabled(true);
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
        }
    }

    @NonNull
    private StringBuilder buildDeviceInfoString() {
        StringBuilder info = new StringBuilder();
        if (!deviceInfo.getDeviceName().isEmpty()) info.append("Device Name: ").append(deviceInfo.getDeviceName()).append("\n");
        if (!deviceInfo.getSerialNumber().isEmpty()) info.append("Serial Number: ").append(deviceInfo.getSerialNumber()).append("\n");
        if (!deviceInfo.getHardwareRevision().isEmpty()) info.append("Hardware Revision: ").append(deviceInfo.getHardwareRevision()).append("\n");
        if (!deviceInfo.getSoftwareRevision().isEmpty()) info.append("Software Revision: ").append(deviceInfo.getSoftwareRevision()).append("\n");
        if (!deviceInfo.getSystemId().isEmpty()) info.append("System ID: ").append(deviceInfo.getSystemId()).append("\n");
        if (!deviceInfo.getPnpId().isEmpty()) info.append("PnP ID: ").append(deviceInfo.getPnpId()).append("\n");
        info.append("Battery: ").append(deviceInfo.getBatteryPercentage()).append("%\n");
        info.append("Charging: ").append(deviceInfo.getChargingStatus()).append("\n");
        info.append("Authenticated: ").append(deviceInfo.isAuthenticated() ? "Yes" : "No");
        return info;
    }

    private void refreshBattery() {
        if (gtr2eManager.isAuthenticated()) {
//            gtr2eManager.refreshBatteryInfo();
            Toast.makeText(this, "Refreshing battery info...", Toast.LENGTH_SHORT).show();
        }
    }

    private void enableDoNotDisturb(){
        if(gtr2eManager.isAuthenticated() && gtr2eManager.isConnected()) {
            gtr2eManager.performAction("DO_NOT_DISTURB_ON");
        }
    }
    
    private void launchTestActivity() {
        Intent intent = new Intent(this, TestActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                connectToDevice();
            } else {
                Toast.makeText(this, "Bluetooth is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gtr2eManager != null) {
            gtr2eManager.disconnect();
        }
    }

    private void animateProgressBar(float newProgress) {
        binding.watchBatteryProgress.setVisibility(View.VISIBLE);
        // Create an ObjectAnimator to animate the "progress" property
        ObjectAnimator animator = ObjectAnimator.ofFloat(binding.watchBatteryProgress, "progress", binding.watchBatteryProgress.getProgress(), newProgress);
        animator.setDuration(500); // Animation duration in milliseconds
        animator.setInterpolator(new AccelerateDecelerateInterpolator()); // Optional: control animation speed
        animator.start();
    }
}