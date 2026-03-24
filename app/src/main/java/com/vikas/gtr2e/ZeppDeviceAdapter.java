package com.vikas.gtr2e;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.vikas.gtr2e.beans.zeppAuthBeans.ZeppDeviceItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ZeppDeviceAdapter extends RecyclerView.Adapter<ZeppDeviceAdapter.ViewHolder> {
    public interface OnDeviceSelectedListener {
        void onDeviceSelected(ZeppDeviceItem device);
    }

    private final List<ZeppDeviceItem> devices;
    private final OnDeviceSelectedListener listener;
    private int selectedPosition = -1;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public ZeppDeviceAdapter(List<ZeppDeviceItem> devices, OnDeviceSelectedListener listener) {
        this.devices = devices;
        this.listener = listener;
        if (devices.size() == 1) {
            selectedPosition = 0;
            if (listener != null) {
                listener.onDeviceSelected(devices.get(0));
            }
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_zepp_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ZeppDeviceItem device = devices.get(position);
        holder.deviceName.setText(device.getSn());
        holder.macValue.setText(device.getMacAddress());

        String lastActive = sdf.format(new Date(device.getLastActiveStatusUpdateTime() * 1000L));
        holder.activeValue.setText(lastActive);

        String authKey = "N/A";
        if (device.getAdditionalInfo() != null && device.getAdditionalInfo().getAuthKey() != null) {
            authKey = "0x" + device.getAdditionalInfo().getAuthKey();
        }
        holder.authKeyValue.setText(authKey);

        String finalAuthKey = authKey;
        holder.copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Auth Key", finalAuthKey);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(v.getContext(), "Auth Key copied to clipboard", Toast.LENGTH_SHORT).show();
        });
        // Selection handling
        if (selectedPosition == position) {
            holder.cardView.setStrokeWidth(4);
            holder.cardView.setCardBackgroundColor(com.google.android.material.R.attr.colorPrimaryContainer);
        } else {
            holder.cardView.setStrokeWidth(0);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onDeviceSelected(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public ZeppDeviceItem getSelectedDevice() {
        if (selectedPosition != -1 && selectedPosition < devices.size()) {
            return devices.get(selectedPosition);
        }
        return null;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView deviceName, macValue, activeValue, authKeyValue;
        MaterialButton copyButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView.findViewById(R.id.cardView);
            deviceName = itemView.findViewById(R.id.deviceName);
            macValue = itemView.findViewById(R.id.macValue);
            activeValue = itemView.findViewById(R.id.activeValue);
            authKeyValue = itemView.findViewById(R.id.authKeyValue);
            copyButton = itemView.findViewById(R.id.copyButton);
        }
    }
}