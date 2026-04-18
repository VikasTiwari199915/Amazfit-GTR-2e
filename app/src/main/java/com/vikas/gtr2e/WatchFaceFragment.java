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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vikas.gtr2e.beans.ZeppCloudBeans.BuiltInWatchFace;
import com.vikas.gtr2e.databinding.FragmentWatchFaceBinding;
import com.vikas.gtr2e.interfaces.ConnectionListener;
import com.vikas.gtr2e.interfaces.RenewTokenCallback;
import com.vikas.gtr2e.listAdapters.WatchFaceAdapter;
import com.vikas.gtr2e.utils.AmazfitAuthUtil;
import com.vikas.gtr2e.utils.GTR2eManager;
import com.vikas.gtr2e.utils.Prefs;
import com.vikas.gtr2e.utils.RetrofitClient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WatchFaceFragment extends Fragment {
    private static final String TAG = "WatchFaceFragment";
    FragmentWatchFaceBinding binding;
    private GTR2eManager gtr2eManager;
    private WatchFaceAdapter adapter;
    private List<BuiltInWatchFace> watchFaceList = new ArrayList<>();
    private boolean installedWatchFacesLoaded = false;
    private boolean fetchingWatchFaceDetails = false;
    private boolean watchFaceListRequested = false;

    public WatchFaceFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.e(TAG, "onCreateView :: WatchFaceFragment");
        binding = FragmentWatchFaceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.e(TAG, "onViewCreated :: WatchFaceFragment");
        watchFaceListRequested = false;
        initRecyclerView();
        initGTR2eManager();
        binding.floatingActionButton.setOnClickListener(v-> startActivity(new Intent(requireActivity(), WatchFaceStoreActivity.class)));
    }

    private void initRecyclerView() {
        // Clicking on an item will call a function implemented later
        watchFaceList = loadWatchFaces();
        adapter = new WatchFaceAdapter(watchFaceList, this::onWatchFaceSelected);
        binding.watchFaceRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.watchFaceRecyclerView.setAdapter(adapter);
        int lastSelectedWatchFaceId = Prefs.getLastSelectedWatchFace(requireContext());
        if(lastSelectedWatchFaceId != -1) {
            binding.watchFaceRecyclerView.post(() -> adapter.setSelectedWatchFaceId(requireContext(), lastSelectedWatchFaceId));
        }
    }

    private void initGTR2eManager() {
        gtr2eManager = GTR2eManager.getInstance(requireActivity());
        gtr2eManager.setConnectionListener(new ConnectionListener() {

            @Override
            public void pendingBleProcessChanged(int count) {
                if(!watchFaceListRequested && gtr2eManager.isConnected() && gtr2eManager.isAuthenticated() && count == 0) {
                    watchFaceListRequested = true;
                    requireActivity().runOnUiThread(()->fetchWatchFaceListFromDevice());
                }
            }

            @Override
            public void onWatchFaceListReceived(List<Integer> watchFaceIds) {
                if (!fetchingWatchFaceDetails) {
                    fetchingWatchFaceDetails = true;
                    requireActivity().runOnUiThread(() -> fetchWatchFaceDetails(watchFaceIds));
                }
            }

            @Override
            public void onCurrentWatchFace(int id) {
                binding.watchFaceRecyclerView.post(() -> adapter.setSelectedWatchFaceId(requireContext(), id));
            }
        });

        if (!watchFaceListRequested && gtr2eManager.isConnected() && gtr2eManager.isAuthenticated()) {
            watchFaceListRequested = true;
            fetchWatchFaceListFromDevice();
        }
    }

    private void fetchWatchFaceListFromDevice() {
        if(installedWatchFacesLoaded || fetchingWatchFaceDetails) return;
        // Show progress bar while fetching)
        requireActivity().runOnUiThread(() -> binding.progressBar.setVisibility(View.VISIBLE));
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
                        if(binding != null) {
                            binding.progressBar.setVisibility(View.GONE);
                        }
                        if (response.isSuccessful() && response.body() != null) {
                            watchFaceList.clear();
                            List<BuiltInWatchFace> installedWatchFaces = new ArrayList<>();

                            // Create map from API response for fast lookup
                            Map<Integer, BuiltInWatchFace> apiMap = new HashMap<>();
                            for (BuiltInWatchFace face : response.body()) {
                                apiMap.put(face.getBuiltin_id(), face);
                            }

                            // Preserve order from device (watchFaceIds)
                            for (Integer id : watchFaceIds) {
                                BuiltInWatchFace face = apiMap.get(id);

                                if (face != null) {
                                    installedWatchFaces.add(face);
                                } else {
                                    // Create placeholder
                                    BuiltInWatchFace placeholder = new BuiltInWatchFace();
                                    placeholder.setBuiltin_id(id);
                                    placeholder.setName("Unknown");
                                    installedWatchFaces.add(placeholder);
                                }
                            }

                            // Update UI list
                            watchFaceList.clear();
                            watchFaceList.addAll(installedWatchFaces);
                            saveWatchFaces(installedWatchFaces);
                            adapter.notifyDataSetChanged();
                            installedWatchFacesLoaded = true;
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
                        if(binding!=null) {
                            binding.progressBar.setVisibility(View.GONE);
                        }
                        Log.e(TAG, "API Failure: " + t.getMessage());
                        Toast.makeText(getContext(), "API Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onWatchFaceSelected(BuiltInWatchFace watchFace, int position) {
        // Function to be implemented later by the user
        Toast.makeText(getContext(), "Selected: " + watchFace.getName(), Toast.LENGTH_SHORT).show();
        adapter.setSelectedWatchFaceId(requireContext(), watchFace.getBuiltin_id());
        Log.e(TAG, "Selected: " + watchFace.getBuiltin_id());
        gtr2eManager.performAction("SET_CURRENT_WATCHFACE_ID", String.valueOf(watchFace.getBuiltin_id()));
    }

    public void saveWatchFaces(List<BuiltInWatchFace> list) {
        String json = new Gson().toJson(list);
        Prefs.saveWatchFaces(requireContext(), json);
    }

    public List<BuiltInWatchFace> loadWatchFaces() {
        try {
            String json = Prefs.loadWatchFaces(requireContext());
            if (json == null) return new ArrayList<>();
            Type type = new TypeToken<List<BuiltInWatchFace>>() {}.getType();
            return new Gson().fromJson(json, type);
        } catch (Exception e) {
            Log.e(TAG, "Error loading watch faces: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}