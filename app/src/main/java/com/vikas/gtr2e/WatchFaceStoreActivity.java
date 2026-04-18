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
import androidx.recyclerview.widget.RecyclerView;

import com.vikas.gtr2e.beans.ZeppCloudBeans.PageableWatchFaceStoreResponse;
import com.vikas.gtr2e.beans.ZeppCloudBeans.StoreResponse;
import com.vikas.gtr2e.beans.ZeppCloudBeans.WatchFaceStoreSection;
import com.vikas.gtr2e.beans.ZeppCloudBeans.WatchfaceItem;
import com.vikas.gtr2e.databinding.ActivityWatchFaceStoreBinding;
import com.vikas.gtr2e.interfaces.RenewTokenCallback;
import com.vikas.gtr2e.listAdapters.WatchFaceStoreSectionAdapter;
import com.vikas.gtr2e.utils.AmazfitAuthUtil;
import com.vikas.gtr2e.utils.Prefs;
import com.vikas.gtr2e.utils.RetrofitClient;
import com.vikas.gtr2e.utils.WatchFaceStoreSections;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Zepp watch face store: lists New, rankings, and categories from {@code getWatchfaceHomepage}.
 * Also includes infinite scrolling for general watch face list.
 */
public class WatchFaceStoreActivity extends AppCompatActivity {

    private ActivityWatchFaceStoreBinding binding;
    private WatchFaceStoreSectionAdapter sectionAdapter;
    private boolean isFetchingHomepage = false;
    private boolean isFetchingPageable = false;

    private int currentPage = 1;
    private int totalPages = 1;
    private final int perPage = 20;
    private final List<WatchfaceItem> pageableItems = new ArrayList<>();
    private WatchFaceStoreSection pageableSection;

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

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        sectionAdapter = new WatchFaceStoreSectionAdapter();
        sectionAdapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(this, WatchFaceDetailActivity.class);
            intent.putExtra("watchface_item", item);
            startActivity(intent);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.categoriesRecyclerView.setLayoutManager(layoutManager);
        binding.categoriesRecyclerView.setAdapter(sectionAdapter);

        binding.categoriesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) { // Scrolling down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (!isFetchingPageable && currentPage < totalPages) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                            loadNextPage();
                        }
                    }
                }
            }
        });

        loadWatchfaceHomepage();
    }

    private void loadNextPage() {
        currentPage++;
        loadPageableWatchFaces();
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
        isFetchingHomepage = true;
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
                isFetchingHomepage = false;
                binding.loadingIndicator.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    StoreResponse body = response.body();
                    List<WatchFaceStoreSection> sections = WatchFaceStoreSections.fromResponse(WatchFaceStoreActivity.this, body);
                    sectionAdapter.submitSections(sections);
                    
                    // After homepage is loaded, start loading pageable content
                    loadPageableWatchFaces();
                    
                    if (sections.isEmpty()) {
                        binding.emptyOrErrorText.setVisibility(View.VISIBLE);
                        binding.emptyOrErrorText.setText(R.string.empty_store_categories);
                    } else {
                        binding.emptyOrErrorText.setVisibility(View.GONE);
                    }
                } else {
                    handleErrorResponse(response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<StoreResponse> call, @NonNull Throwable t) {
                isFetchingHomepage = false;
                binding.loadingIndicator.setVisibility(View.GONE);
                binding.emptyOrErrorText.setVisibility(View.VISIBLE);
                binding.emptyOrErrorText.setText(getString(R.string.error_store_load_failed_detail, t.getMessage() != null ? t.getMessage() : ""));
            }
        });
    }

    private void loadPageableWatchFaces() {
        isFetchingPageable = true;
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        String userId = Prefs.getZeppUserId(this);
        
        RetrofitClient.getZeppApiService(this).getPageableWatchFaces(String.valueOf(currentPage), String.valueOf(perPage), userId)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<PageableWatchFaceStoreResponse> call, @NonNull Response<PageableWatchFaceStoreResponse> response) {
                        isFetchingPageable = false;
                        binding.loadingIndicator.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            PageableWatchFaceStoreResponse body = response.body();
                            totalPages = body.getTotal_page();

                            List<WatchfaceItem> data = body.getData();
                            if (data != null) {
                                pageableItems.addAll(data);

                                if (pageableSection == null) {
                                    pageableSection = new WatchFaceStoreSection("All Watch Faces", pageableItems);
                                    sectionAdapter.addSection(pageableSection);
                                } else {
                                    sectionAdapter.notifyItemChanged(sectionAdapter.getItemCount() - 1);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PageableWatchFaceStoreResponse> call, @NonNull Throwable t) {
                        isFetchingPageable = false;
                        binding.loadingIndicator.setVisibility(View.GONE);
                    }
                });
    }

    private void handleErrorResponse(int code) {
        Toast.makeText(getApplicationContext(), "Failed to fetch watch face details", Toast.LENGTH_SHORT).show();
        if (code == 401) {
            AmazfitAuthUtil.refreshToken(getApplicationContext(), new RenewTokenCallback() {
                @Override
                public void onTokenRenewSuccess(String token) {
                    if(!isFetchingHomepage) {
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
