package com.vikas.gtr2e;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.vikas.gtr2e.beans.zeppAuthBeans.ZeppDeviceItem;
import com.vikas.gtr2e.beans.zeppAuthBeans.ZeppDevicesResponse;
import com.vikas.gtr2e.beans.zeppAuthBeans.ZeppLoginResponse;
import com.vikas.gtr2e.listAdapters.ZeppDeviceAdapter;
import com.vikas.gtr2e.utils.AmazfitAuthUtil;
import com.vikas.gtr2e.utils.Prefs;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZeppLoginActivity extends AppCompatActivity {

    private TextInputLayout usernameLayout, passwordLayout;
    private TextInputEditText usernameEdit;
    private TextInputEditText passwordEdit;
    private MaterialButton loginButton, continueButton;
    private CircularProgressIndicator progressBar;
    private TextView statusText, titleText;
    private RecyclerView devicesRecyclerView;
    private ZeppDeviceAdapter adapter;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        if (Prefs.getDeviceAdded(getApplicationContext())) {
            startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        }

        setContentView(R.layout.activity_zepp_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        titleText = findViewById(R.id.textView2);
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        usernameEdit = findViewById(R.id.usernameEdit);
        passwordEdit = findViewById(R.id.passwordEdit);
        loginButton = findViewById(R.id.loginButton);
        continueButton = findViewById(R.id.continueButton);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        devicesRecyclerView = findViewById(R.id.devicesRecyclerView);

        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loginButton.setOnClickListener(v -> performLogin());
        continueButton.setOnClickListener(v -> handleContinue());
    }

    private void performLogin() {
        String username = usernameEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        updateStatus("Starting login process...");

        executorService.execute(() -> {
            try {
                // 1. Get Tokens
                updateStatusOnMain("Getting authentication tokens...");
                HashMap<String, String> tokens;
                try {
                    tokens = AmazfitAuthUtil.getTokens(username, password);
                } catch (Exception e) {
                    showErrorOnMain(e.getMessage());
                    return;
                }
                if (!tokens.containsKey("access")) {
                    showErrorOnMain("Failed to get tokens. Check credentials.");
                    return;
                }
                String accessToken = tokens.get("access");

                // 2. Perform Login
                updateStatusOnMain("Logging in to Zepp...");
                ZeppLoginResponse loginResponse = AmazfitAuthUtil.login(accessToken);
                if (loginResponse == null || loginResponse.getToken_info() == null) {
                    showErrorOnMain("Login failed.");
                    return;
                }

                String userId = loginResponse.getToken_info().getUser_id();
                String appToken = loginResponse.getToken_info().getApp_token();

                // 3. Fetch Devices
                updateStatusOnMain("Fetching devices...");
                ZeppDevicesResponse devicesResponse = AmazfitAuthUtil.getDevices(userId, appToken);

                mainHandler.post(() -> {
                    setLoading(false);
                    if (devicesResponse != null && devicesResponse.getItems() != null && !devicesResponse.getItems().isEmpty()) {
                        onLoginSuccess(loginResponse, devicesResponse);
                    } else {
                        statusText.setText("Login successful, but no devices found.");
                    }
                });

            } catch (Exception e) {
                showErrorOnMain("Error: " + e.getMessage());
            }
        });
    }

    private void onLoginSuccess(ZeppLoginResponse loginResponse, ZeppDevicesResponse devicesResponse) {
        // Hide login UI
        titleText.setVisibility(View.GONE);
        usernameLayout.setVisibility(View.GONE);
        passwordLayout.setVisibility(View.GONE);
        loginButton.setVisibility(View.GONE);

        // Show device list
        devicesRecyclerView.setVisibility(View.VISIBLE);
        continueButton.setVisibility(View.VISIBLE);

        if (devicesResponse.getItems().size() == 1) {
            statusText.setText("One device found. Selected automatically.");
        } else {
            statusText.setText("Select your device from the list.");
        }

        adapter = new ZeppDeviceAdapter(devicesResponse.getItems(), device -> {
            continueButton.setEnabled(true);
        });
        devicesRecyclerView.setAdapter(adapter);

        continueButton.setEnabled(devicesResponse.getItems().size() == 1);
    }

    private void handleContinue() {
        ZeppDeviceItem selectedDevice = adapter.getSelectedDevice();
        if (selectedDevice != null) {
            String authKey = null;
            if (selectedDevice.getAdditionalInfo() != null) {
                authKey = selectedDevice.getAdditionalInfo().getAuthKey();
            }

            if (authKey != null) {
                // Save to Prefs
                Prefs.setLastDeviceMac(this, selectedDevice.getMacAddress());
                Prefs.setAuthKey(this, authKey);

                Toast.makeText(this, "Device details saved. Opening scan...", Toast.LENGTH_SHORT).show();

                // Open Bluetooth Scan Activity
                Intent intent = new Intent(this, BluetoothScanActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Could not find Auth Key for this device", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Please select a device first", Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
        usernameEdit.setEnabled(!loading);
        passwordEdit.setEnabled(!loading);
    }

    private void updateStatus(String message) {
        statusText.setText(message);
    }

    private void updateStatusOnMain(String message) {
        mainHandler.post(() -> updateStatus(message));
    }

    private void showErrorOnMain(String error) {
        mainHandler.post(() -> {
            setLoading(false);
            statusText.setText(error);
            Toast.makeText(ZeppLoginActivity.this, error, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
