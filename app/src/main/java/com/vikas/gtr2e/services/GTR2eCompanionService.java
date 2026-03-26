package com.vikas.gtr2e.services;

import android.companion.CompanionDeviceService;
import android.companion.DevicePresenceEvent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.vikas.gtr2e.GTR2eManager;

public class GTR2eCompanionService extends CompanionDeviceService {

    public static final String TAG = "GTR2eCompanionService";

    @Override
    public void onDeviceAppeared(@NonNull String address) {
        Log.d("BLE", "Device back in range: " + address);
        reconnectWatch();
    }

    @Override
    public void onDeviceDisappeared(@NonNull String address) {
        Log.d("BLE", "Device out of range: "+address);
    }

    @Override
    public void onDevicePresenceEvent(DevicePresenceEvent event) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            int type = event.getEvent();
            switch (type) {
                case DevicePresenceEvent.EVENT_BLE_APPEARED:
                case DevicePresenceEvent.EVENT_BT_CONNECTED:
                    Log.e(TAG, "onDevicePresenceEvent:: DEVICE_APPEARED");
//                    handleDeviceAppeared(event.getAssociationId());
                    break;

                case DevicePresenceEvent.EVENT_BLE_DISAPPEARED:
                case DevicePresenceEvent.EVENT_BT_DISCONNECTED:
                    Log.e(TAG, "onDevicePresenceEvent:: DEVICE_DISAPPEARED");
//                    handleDeviceDisappeared(event.getAssociationId());
                    break;
            }

        } else {
            // fallback for older Android
            Log.d("BLE", "Presence API not supported, fallback needed");
        }
    }

    private void reconnectWatch(){
        getGtr2eManager().connect();
    }

    private GTR2eManager getGtr2eManager() {
        return GTR2eManager.getInstance(getApplicationContext());
    }

    private GTR2eBleService getBleService() {
        return getGtr2eManager().getBleService();
    }


}