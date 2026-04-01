package com.vikas.gtr2e.listAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.vikas.gtr2e.R;
import com.vikas.gtr2e.beans.ZeppCloudBeans.BuiltInWatchFace;
import com.vikas.gtr2e.databinding.ItemWatchFaceBinding;
import com.vikas.gtr2e.interfaces.OnWatchFaceClickListener;
import com.vikas.gtr2e.utils.Prefs;

import java.util.List;
import java.util.Optional;

public class WatchFaceAdapter extends RecyclerView.Adapter<WatchFaceAdapter.ViewHolder> {

    private final List<BuiltInWatchFace> watchFaces;
    private final OnWatchFaceClickListener listener;
    private int selectedWatchFaceId = -1;
    private int selectedPosition = -1;


    public WatchFaceAdapter(List<BuiltInWatchFace> watchFaces, OnWatchFaceClickListener listener) {
        this.watchFaces = watchFaces;
        this.listener = listener;
    }

    public void setSelectedWatchFaceId(Context context, int id) {
        this.selectedWatchFaceId = id;
        if(this.selectedPosition!=-1) {
            notifyItemChanged(this.selectedPosition);
        }
        Optional<BuiltInWatchFace> watchFace = watchFaces.stream().filter(itm -> itm.getBuiltin_id() == id).findFirst();
        watchFace.ifPresent(builtInWatchFace -> {
            this.selectedPosition = watchFaces.indexOf(builtInWatchFace);
            Prefs.setLastSelectedWatchFace(context, id);
            Prefs.setLastSelectedWatchFaceImageUrl(context, builtInWatchFace.getImage());
        });
        notifyItemChanged(selectedPosition);
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
                .fallback(R.drawable.rounded_question_mark_24)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.binding.imagePreview);

        boolean isSelected = watchFace.getBuiltin_id() == selectedWatchFaceId;
        holder.binding.selectedIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.binding.cardView.setStrokeWidth(isSelected ? 4 : 0);
//        holder.binding.thirdPartyIndicator.setVisibility(watchFace.isOfficial_builtin() ? View.VISIBLE : View.GONE);


        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onWatchFaceClick(watchFace, position);
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