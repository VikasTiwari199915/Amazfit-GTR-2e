package com.vikas.gtr2e;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.vikas.gtr2e.beans.zeppAuthBeans.ZeppDeviceItem;
import com.vikas.gtr2e.beans.zeppAuthBeans.ZeppDevicesResponse;
import com.vikas.gtr2e.beans.zeppAuthBeans.ZeppLoginResponse;
import com.vikas.gtr2e.databinding.ActivityZeppLoginBinding;
import com.vikas.gtr2e.listAdapters.ZeppDeviceAdapter;
import com.vikas.gtr2e.utils.AmazfitAuthUtil;
import com.vikas.gtr2e.utils.Prefs;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZeppLoginActivity extends AppCompatActivity {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    ZeppDeviceAdapter adapter;

    ActivityZeppLoginBinding binding;
    boolean loginOnlyFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityZeppLoginBinding.inflate(getLayoutInflater());
        loginOnlyFlag = getIntent().getBooleanExtra("LOGIN_ONLY", false);
        if (!loginOnlyFlag && Prefs.getDeviceAdded(getApplicationContext())) {
            startActivity(new Intent(this, HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        }

        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if(loginOnlyFlag) {
            binding.titleText.setText("Hi There!\nPlease login again");
            binding.method2TextLabel.setVisibility(View.GONE);
            binding.method1TextLabel.setText("Your app token has been revoked. Please login again.\nThis might have happened if you logged in to zepp account somewhere else.");
            binding.authKeyLayout.setVisibility(View.GONE);
            binding.loginButton.setText(R.string.renew_session);
        }


        binding.devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        binding.loginButton.setOnClickListener(v -> performLogin());
        binding.continueButton.setOnClickListener(v -> handleContinue());
    }

    private void performLogin() {
        String username;
        String password;
        String authKey;

        if(binding.authKeyLayout.getEditText() != null) {
            authKey = binding.authKeyLayout.getEditText().getText().toString().trim();
        } else {
            authKey = "";
        }
        if(binding.usernameLayout.getEditText() != null) {
            username = binding.usernameLayout.getEditText().getText().toString().trim();
        } else {
            username = "";
        }
        if(binding.passwordLayout.getEditText() != null) {
            password = binding.passwordLayout.getEditText().getText().toString().trim();
        } else {
            password = "";
        }

        if(username.isEmpty() && password.isEmpty() && !authKey.isEmpty()){

            if(!authKey.startsWith("0x")){
                Toast.makeText(this, "Please enter a correct auth key, (It should start with 0x)", Toast.LENGTH_SHORT).show();
                return;
            }

            // Auto login with auth key
            Prefs.setAuthKey(getApplicationContext(), authKey);
            Prefs.setZeppAccountLogin(getApplicationContext(), false);
            Intent intent = new Intent(this, DeviceBondingActivity.class);
            startActivity(intent);
            finish();
        }



        if (username.isEmpty() || password.isEmpty() && authKey.isEmpty()) {
            if(loginOnlyFlag) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enter username and password or auth key", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        setLoading(true);
        updateStatus("Starting login process...");

        binding.authKeyLayout.setVisibility(View.GONE);
        binding.method2TextLabel.setVisibility(View.GONE);

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

                saveUserDetails(loginResponse);

                if(loginOnlyFlag) {
                    mainHandler.post(() -> {
                        setLoading(false);
                        Toast.makeText(ZeppLoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, HomeActivity.class);
                        intent.putExtra("DEVICE_ADDED_JUST_NOW", true);
                        startActivity(intent);
                        finish();
                    });
                    return;
                }

                // 3. Fetch Devices
                updateStatusOnMain("Fetching devices...");
                ZeppDevicesResponse devicesResponse = AmazfitAuthUtil.getDevices(userId, appToken);

                mainHandler.post(() -> {
                    setLoading(false);
                    if (devicesResponse != null && devicesResponse.getItems() != null && !devicesResponse.getItems().isEmpty()) {
                        onLoginSuccess(loginResponse, devicesResponse);
                    } else {
                        binding.statusText.setText(R.string.login_successful_but_no_devices_found);
                    }
                });

            } catch (Exception e) {
                showErrorOnMain("Error: " + e.getMessage());
            }
        });
    }

    private void saveUserDetails(@NotNull ZeppLoginResponse loginResponse) {
        Prefs.setZeppAppToken(this, loginResponse.getToken_info().getApp_token());
        Prefs.setZeppLoginToken(this, loginResponse.getToken_info().getLogin_token());
        Prefs.setZeppUserId(this, loginResponse.getToken_info().getUser_id());
        Prefs.setZeppRegion(this, loginResponse.getRegist_info().getRegion());
        Prefs.setZeppCountryCode(this, loginResponse.getRegist_info().getCountry_code());

    }

    private void onLoginSuccess(ZeppLoginResponse loginResponse, ZeppDevicesResponse devicesResponse) {
        // Hide login UI
        binding.titleText.setVisibility(View.GONE);
        binding.usernameLayout.setVisibility(View.GONE);
        binding.passwordLayout.setVisibility(View.GONE);
        binding.loginButton.setVisibility(View.GONE);
        binding.method1TextLabel.setVisibility(View.GONE);
        binding.method2TextLabel.setVisibility(View.GONE);
        binding.authKeyLayout.setVisibility(View.GONE);


        // Show device list
        binding.devicesRecyclerView.setVisibility(View.VISIBLE);
        binding.continueButton.setVisibility(View.VISIBLE);

        if (devicesResponse.getItems().size() == 1) {
            binding.statusText.setText(R.string.one_device_found_selected_automatically);
        } else {
            binding.statusText.setText(R.string.select_your_device_from_the_list);
        }

        adapter = new ZeppDeviceAdapter(devicesResponse.getItems(), device -> {
            binding.continueButton.setEnabled(true);
        });
        binding.devicesRecyclerView.setAdapter(adapter);

        binding.continueButton.setEnabled(devicesResponse.getItems().size() == 1);
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
                Intent intent = new Intent(this, DeviceBondingActivity.class);
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
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.statusText.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!loading);
        binding.usernameEdit.setEnabled(!loading);
        binding.passwordEdit.setEnabled(!loading);
    }

    private void updateStatus(String message) {
        binding.statusText.setText(message);
    }

    private void updateStatusOnMain(String message) {
        mainHandler.post(() -> updateStatus(message));
    }

    private void showErrorOnMain(String error) {
        mainHandler.post(() -> {
            setLoading(false);
            binding.statusText.setText(error);
            Toast.makeText(ZeppLoginActivity.this, error, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
