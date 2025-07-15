package com.vikas.gtr2e;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vikas.gtr2e.databinding.ActivityDashboardBinding;

public class DashboardActivity extends AppCompatActivity {
    private TextView tvSteps;
    private TextView tvCalories;
    private TextView tvHeartRate;
    private TextView tvBattery;
    private TextView tvDistance;
    private TextView tvSleep;

    ActivityDashboardBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        updateDashboard();
    }

    private void initViews() {
        tvSteps = findViewById(R.id.tv_steps);
        tvCalories = findViewById(R.id.tv_calories);
        tvHeartRate = findViewById(R.id.tv_heart_rate);
        tvBattery = findViewById(R.id.tv_battery);
        tvDistance = findViewById(R.id.tv_distance);
        tvSleep = findViewById(R.id.tv_sleep);
    }

    private void updateDashboard() {
        // Update with real data from GTR2eManager
        tvSteps.setText("Steps: 8,432");
        tvCalories.setText("Calories: 456");
        tvHeartRate.setText("Heart Rate: 72 bpm");
        tvBattery.setText("Battery: 85%");
        tvDistance.setText("Distance: 6.2 km");
        tvSleep.setText("Sleep: 7h 23m");
    }
}