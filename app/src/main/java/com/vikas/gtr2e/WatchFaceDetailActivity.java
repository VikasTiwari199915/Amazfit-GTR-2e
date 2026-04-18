package com.vikas.gtr2e;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.vikas.gtr2e.beans.ZeppCloudBeans.WatchfaceItem;
import com.vikas.gtr2e.databinding.ActivityWatchFaceDetailBinding;
import com.vikas.gtr2e.interfaces.FirmwareUpdateListener;
import com.vikas.gtr2e.services.GTR2eBleService;
import com.vikas.gtr2e.utils.GTR2eManager;
import com.vikas.gtr2e.watchFeatureUtilities.GTR2eFirmwareUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WatchFaceDetailActivity extends AppCompatActivity {

    private ActivityWatchFaceDetailBinding binding;
    private WatchfaceItem item;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private File downloadedFile;
    public static final String TAG = "WatchFaceDetailActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityWatchFaceDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        item = (WatchfaceItem) getIntent().getSerializableExtra("watchface_item");

        String name = "", imageUrl = "", downloadUrl;
        int size = 0;

        if (item != null) {
            name = item.getName();
            imageUrl = item.getImage();
            downloadUrl = item.getDownload_url();
            size = item.getSize();
        } else {
            downloadUrl = null;
            Toast.makeText(this, "WatchFace details not valid, returning.", Toast.LENGTH_SHORT).show();
            finish();
        }

        binding.watchFaceName.setText(name);
        if(item.getDescription()!=null && !item.getDescription().isEmpty()) {
            binding.watchFaceDesc.setText(item.getDescription());
            binding.watchFaceDesc.setVisibility(View.VISIBLE);
        } else {
            binding.watchFaceDesc.setVisibility(View.GONE);
        }
        binding.watchFaceSize.setText(MessageFormat.format("Size: {0} KB", size / 1024));

        Glide.with(this)
                .load(imageUrl)
                .into(binding.watchFacePreview);

        binding.btnDownloadInstall.setOnClickListener(v -> {
            if (binding.btnDownloadInstall.getText().toString().equalsIgnoreCase("Download")) {
                startDownload(downloadUrl);
            } else if (binding.btnDownloadInstall.getText().toString().equalsIgnoreCase("Done")) {
                finish();
            } else {
                installWatchFace();
            }
        });
    }

    private void startDownload(String urlString) {
        binding.btnDownloadInstall.setEnabled(false);
        binding.btnDownloadInstall.setText("Downloading...");
        binding.downloadProgress.setVisibility(View.VISIBLE);
        binding.downloadProgress.setProgress(0);

        executor.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                int fileLength = connection.getContentLength();
                InputStream input = connection.getInputStream();
                
                downloadedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "temp_watchface.bin");
                FileOutputStream output = new FileOutputStream(downloadedFile);

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    if (fileLength > 0) {
                        int progress = (int) (total * 100 / fileLength);
                        mainHandler.post(() -> {
                            binding.downloadProgress.setProgress(progress);
                            binding.watchFaceSize.setText(MessageFormat.format("Downloaded : {0}%", progress));
                        });
                    }
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

                mainHandler.post(() -> {
                    binding.btnDownloadInstall.setText("Install");
                    binding.btnDownloadInstall.setEnabled(true);
                    binding.downloadProgress.setVisibility(View.GONE);
                });

            } catch (IOException e) {
                mainHandler.post(() -> {
                    binding.btnDownloadInstall.setEnabled(true);
                    binding.btnDownloadInstall.setText("Download");
                    binding.downloadProgress.setVisibility(View.GONE);
                    Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void installWatchFace() {
        if (downloadedFile != null && downloadedFile.exists()) {
            binding.watchFaceSize.setText("Installation in progress");
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileProvider", downloadedFile);
            
            GTR2eFirmwareUtil fwUtil = GTR2eFirmwareUtil.getFirmwareUtil(contentUri, getApplicationContext());
            GTR2eBleService bleService = GTR2eManager.getInstance(this).getBleService();
            if(fwUtil!= null && bleService != null) {
                binding.btnDownloadInstall.setEnabled(false);
                binding.btnDownloadInstall.setText("Installing...");
                binding.downloadProgress.setVisibility(View.VISIBLE);
                binding.downloadProgress.setProgress(0);

                fwUtil.setListener(new FirmwareUpdateListener() {
                    @Override
                    public void onProgress(int progress) {
                        // Update the progress bar
                        Log.e(TAG, "Progress : "+progress);
                        mainHandler.post(() -> {
                            binding.downloadProgress.setProgress(progress);
                            binding.btnDownloadInstall.setText(MessageFormat.format("Installing... {0}%", progress));
                        });
                    }

                    @Override
                    public void onEnablingFw(){
                        Log.e(TAG, "Installation finished, Enabling watch face");
                        mainHandler.post(() -> {
                            binding.btnDownloadInstall.setEnabled(false);
                            binding.btnDownloadInstall.setText("Enabling WatchFace...");
                            binding.downloadProgress.setVisibility(View.GONE);
                        });
                    }

                    @Override
                    public void onFinish() {
                        Log.e(TAG, "Installation finished");
                        mainHandler.post(() -> {
                            binding.btnDownloadInstall.setEnabled(true);
                            binding.btnDownloadInstall.setText("Done");
                            binding.downloadProgress.setVisibility(View.GONE);
                            Toast.makeText(WatchFaceDetailActivity.this, "Installation finished", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Installation failed: " + message);
                        mainHandler.post(() -> {
                            binding.btnDownloadInstall.setEnabled(true);
                            binding.btnDownloadInstall.setText("Install");
                            binding.downloadProgress.setVisibility(View.GONE);
                            Toast.makeText(WatchFaceDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });

                bleService.setFwUtil(fwUtil);
                fwUtil.startInstallation(bleService);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
