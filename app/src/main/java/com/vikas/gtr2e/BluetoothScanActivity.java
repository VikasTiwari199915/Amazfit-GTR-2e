package com.vikas.gtr2e;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.BluetoothLeDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.vikas.gtr2e.utils.Prefs;

public class BluetoothScanActivity extends AppCompatActivity {

    private static final String TAG = "BluetoothScanActivity";
    private static final int REQUEST_PERMISSION_CODE = 101;
    private static final int BLUETOOTH_REQUEST_CODE = 2345;
    private static final int SELECT_DEVICE_REQUEST_CODE = 42;

    private TextView scanStatus;
    private ProgressBar scanProgress;
    private MaterialButton rescanButton;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bluetooth_scan);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        scanStatus = findViewById(R.id.scanStatus);
        scanProgress = findViewById(R.id.scanProgress);
        rescanButton = findViewById(R.id.rescanButton);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        rescanButton.setOnClickListener(v -> startCompanionDeviceAssociation());
        startScanWithPermissionCheck();
    }

    private void startScanWithPermissionCheck() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            scanStatus.setText(R.string.bluetooth_not_supported);
            scanProgress.setVisibility(View.GONE);
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSION_CODE);
                return;
            }
            startActivityForResult(enableBtIntent, BLUETOOTH_REQUEST_CODE);
            return;
        }

        startCompanionDeviceAssociation();
    }

    private void startCompanionDeviceAssociation() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            startScanWithPermissionCheck();
            return;
        }

        CompanionDeviceManager deviceManager = (CompanionDeviceManager) getSystemService(Context.COMPANION_DEVICE_SERVICE);

        // Filter for GTR 2e devices using its auth service UUID
        BluetoothLeDeviceFilter deviceFilter = new BluetoothLeDeviceFilter.Builder()
                .setScanFilter(new ScanFilter.Builder()
                        .setDeviceAddress(Prefs.getLastDeviceMac(getApplicationContext()))
                        .build())
                .build();
//        BluetoothLeDeviceFilter deviceFilter = new BluetoothLeDeviceFilter.Builder()
//                        .setNamePattern(Pattern.compile("Amazfit GTR 2e"))
//                        .build();
//        BluetoothLeDeviceFilter deviceFilter = new BluetoothLeDeviceFilter.Builder()
//                        .setScanFilter(new ScanFilter.Builder().setDeviceName("Amazfit GTR 2e").build())
//                        .build();

        AssociationRequest pairingRequest = new AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .setSingleDevice(false)
                .setDeviceProfile(AssociationRequest.DEVICE_PROFILE_WATCH)
                .build();

        scanStatus.setText(R.string.searching_for_devices);
        scanProgress.setVisibility(View.VISIBLE);
        rescanButton.setEnabled(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            deviceManager.associate(pairingRequest, getMainExecutor(), companionCallback);
        } else {
            deviceManager.associate(pairingRequest, companionCallback, null);
        }
    }

    private final CompanionDeviceManager.Callback companionCallback = new CompanionDeviceManager.Callback() {
        @Override
        public void onAssociationPending(@NonNull IntentSender intentSender) {
            try {
                startIntentSenderForResult(intentSender, SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Failed to send intent", e);
            }
        }

        @Override
        public void onAssociationCreated(@NonNull AssociationInfo associationInfo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Log.d(TAG, "Association created: " + associationInfo.getDeviceMacAddress());
                Prefs.setLastDeviceAssociationId(getApplicationContext(), associationInfo.getId());
            } else {
                Log.d(TAG, "Association created");
            }

        }

        @Override
        public void onFailure(CharSequence error) {
            Log.e(TAG, "Association failed: " + error);
            runOnUiThread(() -> {
                scanStatus.setText("Search failed or cancelled");
                scanProgress.setVisibility(View.GONE);
                rescanButton.setEnabled(true);
            });
        }
    };

    @SuppressLint("MissingPermission")
    private void handleDeviceSelected(BluetoothDevice device) {
        Prefs.setLastDeviceMac(this, device.getAddress());
        Prefs.setDeviceAdded(getApplicationContext(),true);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("DEVICE_ADDED_JUST_NOW", true);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanWithPermissionCheck();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            startCompanionDeviceAssociation();
        } else if (requestCode == SELECT_DEVICE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Object device = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
            if (device instanceof BluetoothDevice) {
                handleDeviceSelected((BluetoothDevice) device);
            } else if (device instanceof ScanResult) {
                handleDeviceSelected(((ScanResult) device).getDevice());
            }
            scanStatus.setText("Device selected");
            scanProgress.setVisibility(View.GONE);
            rescanButton.setEnabled(true);
        } else if (requestCode == SELECT_DEVICE_REQUEST_CODE) {
            scanStatus.setText("Discovery cancelled");
            scanProgress.setVisibility(View.GONE);
            rescanButton.setEnabled(true);
        }
    }
}
