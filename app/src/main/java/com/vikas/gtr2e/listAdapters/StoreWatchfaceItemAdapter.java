package com.vikas.gtr2e.listAdapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.vikas.gtr2e.R;
import com.vikas.gtr2e.beans.ZeppCloudBeans.WatchfaceItem;
import com.vikas.gtr2e.databinding.ItemStoreWatchfaceBinding;

import java.util.Collections;
import java.util.List;

/**
 * Horizontal list of store watch faces (preview + name).
 */
public class StoreWatchfaceItemAdapter extends RecyclerView.Adapter<StoreWatchfaceItemAdapter.VH> {

    private final List<WatchfaceItem> items;

    public StoreWatchfaceItemAdapter(List<WatchfaceItem> items) {
        this.items = items != null ? items : Collections.emptyList();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStoreWatchfaceBinding binding = ItemStoreWatchfaceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        WatchfaceItem item = items.get(position);
        holder.binding.watchFaceName.setText(item.getName() != null ? item.getName() : "");
        Glide.with(holder.binding.imagePreview.getContext())
                .load(item.getImage())
                .fallback(R.drawable.rounded_question_mark_24)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.binding.imagePreview);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemStoreWatchfaceBinding binding;

        VH(ItemStoreWatchfaceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
