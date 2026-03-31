package com.vikas.gtr2e.listAdapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vikas.gtr2e.beans.ZeppCloudBeans.BuiltInWatchFace;
import com.vikas.gtr2e.databinding.ItemWatchFaceBinding;

import java.util.List;

public class WatchFaceAdapter extends RecyclerView.Adapter<WatchFaceAdapter.ViewHolder> {

    public interface OnWatchFaceClickListener {
        void onWatchFaceClick(BuiltInWatchFace watchFace);
    }

    private final List<BuiltInWatchFace> watchFaces;
    private final OnWatchFaceClickListener listener;
    private int selectedWatchFaceId = -1;

    public WatchFaceAdapter(List<BuiltInWatchFace> watchFaces, OnWatchFaceClickListener listener) {
        this.watchFaces = watchFaces;
        this.listener = listener;
    }

    public void setSelectedWatchFaceId(int id) {
        this.selectedWatchFaceId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWatchFaceBinding binding = ItemWatchFaceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BuiltInWatchFace watchFace = watchFaces.get(position);
        holder.binding.watchFaceName.setText(watchFace.getName());

        Glide.with(holder.binding.imagePreview.getContext())
                .load(watchFace.getImage())
                .into(holder.binding.imagePreview);

        boolean isSelected = watchFace.getBuiltin_id() == selectedWatchFaceId;
        holder.binding.selectedIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.binding.cardView.setStrokeWidth(isSelected ? 4 : 0);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onWatchFaceClick(watchFace);
            }
        });
    }

    @Override
    public int getItemCount() {
        return watchFaces.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemWatchFaceBinding binding;

        public ViewHolder(@NonNull ItemWatchFaceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}