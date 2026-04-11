package com.vikas.gtr2e;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.vikas.gtr2e.beans.ZeppCloudBeans.StoreResponse;
import com.vikas.gtr2e.beans.ZeppCloudBeans.WatchFaceStoreSection;
import com.vikas.gtr2e.databinding.ActivityWatchFaceStoreBinding;
import com.vikas.gtr2e.interfaces.RenewTokenCallback;
import com.vikas.gtr2e.listAdapters.WatchFaceStoreSectionAdapter;
import com.vikas.gtr2e.utils.AmazfitAuthUtil;
import com.vikas.gtr2e.utils.Prefs;
import com.vikas.gtr2e.utils.RetrofitClient;
import com.vikas.gtr2e.utils.WatchFaceStoreSections;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Zepp watch face store: lists New, rankings, and categories from {@code getWatchfaceHomepage}.
 */
public class WatchFaceStoreActivity extends AppCompatActivity {

    private ActivityWatchFaceStoreBinding binding;
    private WatchFaceStoreSectionAdapter sectionAdapter;
    private boolean isFetchingData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityWatchFaceStoreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // calling the action bar
        ActionBar actionBar = getSupportActionBar();
        // showing the back button in action bar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        sectionAdapter = new WatchFaceStoreSectionAdapter();
        binding.categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.categoriesRecyclerView.setAdapter(sectionAdapter);

        loadWatchfaceHomepage();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadWatchfaceHomepage() {
        isFetchingData = true;
        String userId = Prefs.getZeppUserId(this);
        if (TextUtils.isEmpty(userId)) {
            binding.loadingIndicator.setVisibility(View.GONE);
            binding.emptyOrErrorText.setVisibility(View.VISIBLE);
            binding.emptyOrErrorText.setText(R.string.error_store_login_required);
            return;
        }

        binding.loadingIndicator.setVisibility(View.VISIBLE);
        binding.emptyOrErrorText.setVisibility(View.GONE);

        RetrofitClient.getZeppApiService(this).getWatchfaceHomepage(userId).enqueue(new Callback<StoreResponse>() {
            @Override
            public void onResponse(@NonNull Call<StoreResponse> call, @NonNull Response<StoreResponse> response) {
                isFetchingData = false;
                binding.loadingIndicator.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    StoreResponse body = response.body();
                    List<WatchFaceStoreSection> sections = WatchFaceStoreSections.fromResponse(WatchFaceStoreActivity.this, body);
                    sectionAdapter.submitSections(sections);
                    if (sections.isEmpty()) {
                        binding.emptyOrErrorText.setVisibility(View.VISIBLE);
                        binding.emptyOrErrorText.setText(R.string.empty_store_categories);
                    } else {
                        binding.emptyOrErrorText.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Failed to fetch watch face details", Toast.LENGTH_SHORT).show();
                    if (response.code() == 401) {
                        AmazfitAuthUtil.refreshToken(getApplicationContext(), new RenewTokenCallback() {
                            @Override
                            public void onTokenRenewSuccess(String token) {
                                if(!isFetchingData) {
                                    loadWatchfaceHomepage();
                                }
                            }
                            @Override
                            public void onTokenRenewFailure(String error) {
                                Toast.makeText(getApplicationContext(), "Failed to renew token: " + error, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(getApplicationContext(), ZeppLoginActivity.class);
                                intent.putExtra("LOGIN_ONLY", true);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<StoreResponse> call, @NonNull Throwable t) {
                isFetchingData = false;
                binding.loadingIndicator.setVisibility(View.GONE);
                binding.emptyOrErrorText.setVisibility(View.VISIBLE);
                binding.emptyOrErrorText.setText(getString(R.string.error_store_load_failed_detail, t.getMessage() != null ? t.getMessage() : ""));
            }
        });
    }
}
