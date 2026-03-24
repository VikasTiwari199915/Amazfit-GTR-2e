package com.vikas.gtr2e;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vikas.gtr2e.databinding.ActivitySettingsBinding;
import com.vikas.gtr2e.utils.MediaUtil;

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
    }

    private void initViews() {
        switchNotifications = binding.switchNotifications;
        switchHeartRate = binding.switchHeartRate;
        switchAutoSync = binding.switchAutoSync;
        tvDeviceInfo = binding.tvDeviceInfo;

        switchNotifications.setOnClickListener(v -> {
            // Open Notification Access settings
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        });

        switchHeartRate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save heart rate monitoring setting
        });

        switchAutoSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save auto sync setting
        });
    }

    private void loadSettings() {
        // Check if Notification Access is granted
        boolean hasNotificationAccess = MediaUtil.isNotificationListenerEnabled(this);
        switchNotifications.setChecked(hasNotificationAccess);

        // Load other settings from SharedPreferences (mocked for now)
        switchHeartRate.setChecked(true);
        switchAutoSync.setChecked(true);

        tvDeviceInfo.setText("GTR 2e\nFirmware: v1.0.0\nSerial: GTR2E123456");
    }
}
