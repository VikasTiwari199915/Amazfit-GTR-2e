package com.vikas.gtr2e.listAdapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vikas.gtr2e.beans.ZeppCloudBeans.WatchFaceStoreSection;
import com.vikas.gtr2e.beans.ZeppCloudBeans.WatchfaceItem;
import com.vikas.gtr2e.databinding.ItemWatchFaceStoreCategoryBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Vertical list of store sections (New, rankings, categories); each row is a title + horizontal watch faces.
 */
public class WatchFaceStoreSectionAdapter extends RecyclerView.Adapter<WatchFaceStoreSectionAdapter.SectionVH> {

    private List<WatchFaceStoreSection> sections = new ArrayList<>();
    private StoreWatchfaceItemAdapter.OnItemClickListener itemClickListener;

    public void submitSections(List<WatchFaceStoreSection> sections) {
        this.sections = sections != null ? new ArrayList<>(sections) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addSection(WatchFaceStoreSection section) {
        if (section != null) {
            this.sections.add(section);
            notifyItemInserted(sections.size() - 1);
        }
    }

    public void setOnItemClickListener(StoreWatchfaceItemAdapter.OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    @NonNull
    @Override
    public SectionVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWatchFaceStoreCategoryBinding binding = ItemWatchFaceStoreCategoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SectionVH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SectionVH holder, int position) {
        WatchFaceStoreSection section = sections.get(position);
        holder.binding.categoryTitle.setText(section.title);

        List<WatchfaceItem> data = section.items;
        StoreWatchfaceItemAdapter inner = new StoreWatchfaceItemAdapter(data, itemClickListener);
        
        if ("All Watch Faces".equals(section.title)) {
            holder.binding.watchFacesRecyclerView.setLayoutManager(
                    new GridLayoutManager(holder.binding.getRoot().getContext(), 3));
        } else {
            holder.binding.watchFacesRecyclerView.setLayoutManager(
                    new LinearLayoutManager(holder.binding.getRoot().getContext(), LinearLayoutManager.HORIZONTAL, false));
        }

        holder.binding.watchFacesRecyclerView.setAdapter(inner);
        holder.binding.watchFacesRecyclerView.setNestedScrollingEnabled(false);
        holder.binding.watchFacesRecyclerView.setHasFixedSize(true);
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    static class SectionVH extends RecyclerView.ViewHolder {
        final ItemWatchFaceStoreCategoryBinding binding;

        SectionVH(ItemWatchFaceStoreCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
