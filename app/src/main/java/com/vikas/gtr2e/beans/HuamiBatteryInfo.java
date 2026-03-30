package com.vikas.gtr2e.beans;

import android.util.Log;

import androidx.annotation.NonNull;

import com.vikas.gtr2e.interfaces.ConnectionListener;
import com.vikas.gtr2e.utils.GTR2eManager;

/*
 * Parser for Huami device battery information
 * Based on the reference implementation from Gadgetbridge
 * Licensed under AGPLv3
 * Some changes might have been made to better fit the needs of this project.
 *
 * Modifications by Vikas Tiwari
 */
public class HuamiBatteryInfo {
    private static final String TAG = "HuamiBatteryInfoHelper";
    
    public static final byte DEVICE_BATTERY_NORMAL = 0;
    public static final byte DEVICE_BATTERY_CHARGING = 1;
    
    private final byte[] data;
    
    public HuamiBatteryInfo(byte[] data) {
        this.data = data;
    }
    
    public int getLevelInPercent() {
        Log.e("HuamiBatteryInfoHelper","Battery Get Percentage = " + (data.length >= 2 ? data[1] : "Unknown"));
        if (data.length >= 2) {
            return data[1];
        }
        return 0; // actually unknown
    }
    
    public boolean isCharging() {
        if (data.length >= 3) {
            int value = data[2];
            switch (value) {
                case DEVICE_BATTERY_NORMAL:
                    return false;   //BATTERY_NORMAL;
                case DEVICE_BATTERY_CHARGING:
                    return true;    //BATTERY_CHARGING;
            }
        }
        return false;
    }
    
    public String getStateString() {
        if (data.length >= 3) {
            int value = data[2] & 0xFF;
            return switch (value) {
                case DEVICE_BATTERY_NORMAL -> "Normal";
                case DEVICE_BATTERY_CHARGING -> "Charging";
                default -> "Unknown";
            };
        }
        return "Unknown";
    }
    
    public static HuamiBatteryInfo parseBatteryResponse(byte[] data) {
        if (data == null || data.length < 3) {
            Log.w(TAG, "Invalid battery data received");
            return null;
        }
        return new HuamiBatteryInfo(data);
    }
    
    @NonNull
    public String toString() {
        return "Battery: " + getLevelInPercent() + "% (" + getStateString() + ")";
    }

    public static void processReceivedBatteryData(byte[] data, ConnectionListener connectionListener) {
        if (data == null || data.length == 0) {
            return;
        }

        // Try to parse as battery info
        HuamiBatteryInfo batteryInfo = parseBatteryResponse(data);
        if (batteryInfo != null) {
            Log.d(TAG, "Battery info: " + batteryInfo);
            if (connectionListener != null) {
                connectionListener.onBatteryInfoUpdated(batteryInfo);
            }
        }
    }
} 