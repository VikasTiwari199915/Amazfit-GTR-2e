package com.vikas.gtr2e;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.vikas.gtr2e.beans.DeviceInfo;
import com.vikas.gtr2e.beans.HuamiBatteryInfo;
import com.vikas.gtr2e.beans.ZeppCloudBeans.BuiltInWatchFace;
import com.vikas.gtr2e.databinding.FragmentWatchFaceBinding;
import com.vikas.gtr2e.interfaces.ConnectionListener;
import com.vikas.gtr2e.interfaces.RenewTokenCallback;
import com.vikas.gtr2e.listAdapters.WatchFaceAdapter;
import com.vikas.gtr2e.utils.AmazfitAuthUtil;
import com.vikas.gtr2e.utils.GTR2eManager;
import com.vikas.gtr2e.utils.Prefs;
import com.vikas.gtr2e.utils.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WatchFaceFragment extends Fragment {
    private static final String TAG = "WatchFaceFragment";
    FragmentWatchFaceBinding binding;
    private GTR2eManager gtr2eManager;
    private WatchFaceAdapter adapter;
    private final List<BuiltInWatchFace> watchFaceList = new ArrayList<>();
    private boolean installedWatchFacesLoaded = false;
    private boolean fetchingWatchFaceDetails = false;

    public WatchFaceFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWatchFaceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initRecyclerView();
        initGTR2eManager();
    }

    private void initRecyclerView() {
        adapter = new WatchFaceAdapter(watchFaceList, watchFace -> {
            // Clicking on an item will call a function implemented later
            onWatchFaceSelected(watchFace);
        });
        binding.watchFaceRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.watchFaceRecyclerView.setAdapter(adapter);
    }

    private void initGTR2eManager() {
        gtr2eManager = GTR2eManager.getInstance(requireContext());
        gtr2eManager.setConnectionListener(new ConnectionListener() {
            @Override
            public void onBackgroundServiceBound(boolean bound) {
                if (bound && gtr2eManager.isAuthenticated()) {
                    fetchWatchFaceListFromDevice();
                }
            }

            @Override
            public void onConnectedChanged(boolean connected) {
            }

            @Override
            public void onAuthenticated() {
                fetchWatchFaceListFromDevice();
            }

            @Override
            public void onBatteryInfoUpdated(HuamiBatteryInfo batteryInfo) {
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error: " + error);
            }

            @Override
            public void onHeartRateChanged(int heartRate) {
            }

            @Override
            public void onHeartRateMonitoringChanged(boolean enabled) {
            }

            @Override
            public void findPhoneStateChanged(boolean started) {
            }

            @Override
            public void pendingBleProcessChanged(int count) {
            }

            @Override
            public void onDeviceInfoChanged(DeviceInfo deviceInfo) {
            }

            @Override
            public void onWatchFaceSet(boolean success) {
            }

            @Override
            public void onWatchFaceListReceived(List<Integer> watchFaceIds) {
                if (getActivity() != null && !fetchingWatchFaceDetails) {
                    fetchingWatchFaceDetails = true;
                    getActivity().runOnUiThread(() -> fetchWatchFaceDetails(watchFaceIds));
                }
            }
        });

        if (gtr2eManager.isAuthenticated()) {
            fetchWatchFaceListFromDevice();
        }
    }

    private void fetchWatchFaceListFromDevice() {
        if(installedWatchFacesLoaded) return;
        // Show progress bar while fetching)
        binding.progressBar.setVisibility(View.VISIBLE);
        gtr2eManager.performAction("REQUEST_WATCHFACE_LIST");
    }

    private void fetchWatchFaceDetails(List<Integer> watchFaceIds) {
        if (watchFaceIds == null || watchFaceIds.isEmpty()) {
            binding.progressBar.setVisibility(View.GONE);
            return;
        }

        String idsString = watchFaceIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        String userId = Prefs.getZeppUserId(requireContext());

        RetrofitClient.getZeppApiService(requireContext())
                .getBuiltinWatchfaces(idsString, userId)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<List<BuiltInWatchFace>> call, @NonNull Response<List<BuiltInWatchFace>> response) {
                        fetchingWatchFaceDetails = false;
                        binding.progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            watchFaceList.clear();
                            watchFaceList.addAll(response.body());
                            adapter.notifyDataSetChanged();
                            installedWatchFacesLoaded = true;

                            // For now, let's assume the first one is selected or we can get currently selected ID if available
                            // Implementation for selected indicator can be refined later
                        } else {
                            Toast.makeText(getContext(), "Failed to fetch watch face details", Toast.LENGTH_SHORT).show();
                            if (response.code() == 401) {
                                AmazfitAuthUtil.refreshToken(requireContext(), new RenewTokenCallback() {
                                    @Override
                                    public void onTokenRenewSuccess(String token) {
                                        if(!fetchingWatchFaceDetails) {
                                            fetchWatchFaceDetails(watchFaceIds);
                                        }
                                    }
                                    @Override
                                    public void onTokenRenewFailure(String error) {
                                        Toast.makeText(getContext(), "Failed to renew token: " + error, Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(getContext(), ZeppLoginActivity.class);
                                        intent.putExtra("LOGIN_ONLY", true);
                                        requireActivity().startActivity(intent);
                                        requireActivity().finish();
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<BuiltInWatchFace>> call, @NonNull Throwable t) {
                        fetchingWatchFaceDetails = false;
                        binding.progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "API Failure: " + t.getMessage());
                        Toast.makeText(getContext(), "API Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onWatchFaceSelected(BuiltInWatchFace watchFace) {
        // Function to be implemented later by the user
        Toast.makeText(getContext(), "Selected: " + watchFace.getName(), Toast.LENGTH_SHORT).show();
        adapter.setSelectedWatchFaceId(watchFace.getBuiltin_id());
        Log.e(TAG, "Selected: " + watchFace.getBuiltin_id());
        gtr2eManager.performAction("SET_CURRENT_WATCHFACE_ID", String.valueOf(watchFace.getBuiltin_id()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}