package com.vikas.gtr2e;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import com.vikas.gtr2e.beans.DeviceInfo;

import java.text.MessageFormat;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int BLUETOOTH_REQUEST_CODE = 2;

    private Button btnConnect;
    private Button btnRefresh;
    private Button btnTest;
    private TextView tvStatus;
    private TextView tvDeviceInfo;

    private BluetoothAdapter bluetoothAdapter;
    private GTR2eManager gtr2eManager;

    private final DeviceInfo deviceInfo = new DeviceInfo();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initBluetooth();
        initGTR2eManager();
        checkPermissions();
    }

    private void initViews() {
        btnConnect = findViewById(R.id.btn_connect);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnTest = findViewById(R.id.btn_test);
        tvStatus = findViewById(R.id.tv_status);
        tvDeviceInfo = findViewById(R.id.tv_device_info);

        btnConnect.setOnClickListener(v -> connectToDevice());
        btnRefresh.setOnClickListener(v -> refreshBattery());
        btnTest.setOnClickListener(v -> launchTestActivity());
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
                        tvStatus.setText("Connected to GTR 2e");
                        btnConnect.setText("Disconnect");
                        deviceInfo.setConnected(true);
                    });
                } else {
                    Log.d("MainActivity", "onDisconnected() called");
                    runOnUiThread(() -> {
                        tvStatus.setText("Disconnected");
                        btnConnect.setText("Connect");
                        btnRefresh.setEnabled(false);
                        tvDeviceInfo.setText("No device connected");
                        deviceInfo.setConnected(false);
                    });
                }
            }
            
            @Override
            public void onAuthenticated() {
                runOnUiThread(() -> {
                    tvStatus.setText("Connected & Authenticated");
                    btnRefresh.setEnabled(true);
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
                    tvStatus.setText(MessageFormat.format("Error: {0}", error));
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }

//            @Override
//            public void onDeviceInfoUpdated(DeviceInfo info) {
//                runOnUiThread(() -> updateDeviceInfo());
//            }
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
            gtr2eManager.startScan();
        }
    }

    private void updateDeviceInfo() {
        if (gtr2eManager.isConnected()) {
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
            tvDeviceInfo.setText(info.toString());
        }
    }
    
    private void refreshBattery() {
        if (gtr2eManager.isAuthenticated()) {
//            gtr2eManager.refreshBatteryInfo();
            Toast.makeText(this, "Refreshing battery info...", Toast.LENGTH_SHORT).show();
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
}