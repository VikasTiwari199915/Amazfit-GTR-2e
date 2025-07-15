package com.vikas.gtr2e;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;
import android.widget.TextView;

import com.vikas.gtr2e.databinding.ActivityDeviceBinding;

public class DeviceActivity extends AppCompatActivity {
    private TextView tvDeviceName;
    private TextView tvDeviceAddress;
    private TextView tvConnectionStatus;
    private Button btnSync;
    private Button btnFindDevice;
    private Button btnVibrate;
    ActivityDeviceBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityDeviceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initViews();
        updateDeviceInfo();
    }

    private void initViews() {
        tvDeviceName = findViewById(R.id.tv_device_name);
        tvDeviceAddress = findViewById(R.id.tv_device_address);
        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        btnSync = findViewById(R.id.btn_sync);
        btnFindDevice = findViewById(R.id.btn_find_device);
        btnVibrate = findViewById(R.id.btn_vibrate);

        btnSync.setOnClickListener(v -> syncDevice());
        btnFindDevice.setOnClickListener(v -> findDevice());
        btnVibrate.setOnClickListener(v -> vibrateDevice());
    }

    private void updateDeviceInfo() {
        tvDeviceName.setText("Amazfit GTR 2e");
        tvDeviceAddress.setText("MAC: 12:34:56:78:9A:BC");
        tvConnectionStatus.setText("Status: Connected");
    }

    private void syncDevice() {
        // Implement device sync
    }

    private void findDevice() {
        // Implement find device feature
    }

    private void vibrateDevice() {
        // Implement device vibration
    }
}