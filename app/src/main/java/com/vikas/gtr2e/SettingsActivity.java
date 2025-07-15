package com.vikas.gtr2e;

import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vikas.gtr2e.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchNotifications;
    private Switch switchHeartRate;
    private Switch switchAutoSync;
    private TextView tvDeviceInfo;

    ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        loadSettings();
    }

    private void initViews() {
        switchNotifications = findViewById(R.id.switch_notifications);
        switchHeartRate = findViewById(R.id.switch_heart_rate);
        switchAutoSync = findViewById(R.id.switch_auto_sync);
        tvDeviceInfo = findViewById(R.id.tv_device_info);

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save notification setting
        });

        switchHeartRate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save heart rate monitoring setting
        });

        switchAutoSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save auto sync setting
        });
    }

    private void loadSettings() {
        // Load settings from SharedPreferences
        switchNotifications.setChecked(true);
        switchHeartRate.setChecked(true);
        switchAutoSync.setChecked(true);

        tvDeviceInfo.setText("GTR 2e\nFirmware: v1.0.0\nSerial: GTR2E123456");
    }
}