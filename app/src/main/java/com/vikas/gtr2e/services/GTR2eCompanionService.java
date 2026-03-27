package com.vikas.gtr2e.services;

import android.companion.CompanionDeviceService;
import android.companion.DevicePresenceEvent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.vikas.gtr2e.GTR2eApp;

/**
 * CompanionDeviceService implementation for GTR2e
 * Notifies when device is in range or out of range
 * @author Vikas Tiwari
 */
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
    public void onDevicePresenceEvent(@NonNull DevicePresenceEvent event) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            int type = event.getEvent();
            switch (type) {
                case DevicePresenceEvent.EVENT_BLE_APPEARED:
                case DevicePresenceEvent.EVENT_BT_CONNECTED:
                    Log.e(TAG, "onDevicePresenceEvent:: DEVICE_APPEARED");
                    reconnectWatch();
                    break;

                case DevicePresenceEvent.EVENT_BLE_DISAPPEARED:
                case DevicePresenceEvent.EVENT_BT_DISCONNECTED:
                    Log.e(TAG, "onDevicePresenceEvent:: DEVICE_DISAPPEARED");
                    break;
            }

        } else {
            // fallback for older Android
            Log.d("BLE", "Presence API not supported, fallback needed");
        }
    }

    private void reconnectWatch(){
        if(GTR2eApp.getGTR2eManager()!=null) {
            GTR2eApp.getGTR2eManager().connect();
        }
    }


}