package com.vikas.gtr2e;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TestActivity extends AppCompatActivity {
    private static final String TAG = "TestActivity";
    
    private TextView tvLog;
    private Button btnTestBattery;
    private Button btnTestAuth;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        
        tvLog = findViewById(R.id.tv_log);
        btnTestBattery = findViewById(R.id.btn_test_battery);
        btnTestAuth = findViewById(R.id.btn_test_auth);
        
        btnTestBattery.setOnClickListener(v -> testBatteryParsing());
        btnTestAuth.setOnClickListener(v -> testAuthCommands());
    }
    
    private void testBatteryParsing() {
        log("Testing battery parsing...");
        
        // Test valid battery response
        byte[] validBatteryData = {0x10, 75, 0}; // 75% battery, not charging
        HuamiBatteryInfo batteryInfo = HuamiBatteryInfo.parseBatteryResponse(validBatteryData);
        if (batteryInfo != null) {
            log("Valid battery data: " + batteryInfo.toString());
        } else {
            log("Failed to parse valid battery data");
        }
        
        // Test charging battery
        byte[] chargingBatteryData = {0x10, 90, 1}; // 90% battery, charging
        HuamiBatteryInfo chargingInfo = HuamiBatteryInfo.parseBatteryResponse(chargingBatteryData);
        if (chargingInfo != null) {
            log("Charging battery data: " + chargingInfo.toString());
        } else {
            log("Failed to parse charging battery data");
        }
        
        // Test invalid data
        byte[] invalidData = {0x11, 50, 0}; // Wrong response byte
        HuamiBatteryInfo invalidInfo = HuamiBatteryInfo.parseBatteryResponse(invalidData);
        if (invalidInfo == null) {
            log("Correctly rejected invalid battery data");
        } else {
            log("Should have rejected invalid battery data");
        }
    }
    
    private void testAuthCommands() {
        log("Testing auth commands...");
        
        // Test auth commands
        byte[] authKey = {0x01, 0x08, 0x6c, (byte)0xd3, 0x1f, 0x60, 0x6d, (byte)0xf4, (byte)0xb7, (byte)0x8d, 0x3a, (byte)0xfd, (byte)0xcc, 0x0f, 0x7b, 0x61, 0x74, (byte)0x84};
        log("Auth key command: " + bytesToHex(authKey));
        
        byte[] authRandom = {0x02, 0x08};
        log("Auth random command: " + bytesToHex(authRandom));
        
        byte[] authEncrypted = {0x03, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        log("Auth encrypted command: " + bytesToHex(authEncrypted));
        
        // Test battery request
        byte[] batteryRequest = {0x10, 0x01, 0x01};
        log("Battery request command: " + bytesToHex(batteryRequest));
    }
    
    private void log(String message) {
        Log.d(TAG, message);
        runOnUiThread(() -> {
            tvLog.append(message + "\n");
        });
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
} 