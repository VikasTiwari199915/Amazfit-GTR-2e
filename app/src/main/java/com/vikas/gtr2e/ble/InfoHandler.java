package com.vikas.gtr2e.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.util.UUID;

public class InfoHandler {
    public static final String TAG = "InfoHandler";
    
    public static boolean onInfoReceived(BluetoothGattCharacteristic characteristic, byte[] value, GTR2eBleService.ConnectionCallback connectionCallback){
        final UUID characteristicUUID = characteristic.getUuid();
        if (HuamiService.UUID_CHARACTERISTIC_6_BATTERY_INFO.equals(characteristicUUID)) {
            Log.i(TAG,"HANDLING BATTERY INFO");
            connectionCallback.onBatteryDataReceived(value);
            handleBatteryInfo(value, BluetoothGatt.GATT_SUCCESS);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_REALTIME_STEPS.equals(characteristicUUID)) {
            Log.i(TAG,"HANDLING REALTIME STEPS INFO");
//            handleRealtimeSteps(value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT.equals(characteristicUUID)) {
            Log.i(TAG,"HANDLING HEART RATE INFO");
//            handleHeartrate(value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_AUTH.equals(characteristicUUID)) {
            Log.i(TAG,"HANDLING AUTHENTICATION INFO");
            Log.i(TAG,"AUTHENTICATION?? " + characteristicUUID);
//            logMessageContent(value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_DEVICEEVENT.equals(characteristicUUID)) {
            Log.i(TAG,"HANDLING DEVICE EVENT INFO");
//            handleDeviceEvent(value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_WORKOUT.equals(characteristicUUID)) {
            Log.i(TAG,"HANDLING WORKOUT INFO");
//            handleDeviceWorkoutEvent(value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_7_REALTIME_STEPS.equals(characteristicUUID)) {
            Log.i(TAG,"HANDLING 7CHARACTERISTIC REALTIME STEPS INFO");
//            handleRealtimeSteps(value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION.equals(characteristicUUID)) {
            Log.i(TAG,"HANDLING CONFIGURATION INFO");
//            handleConfigurationInfo(value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ.equals(characteristicUUID)) {
            Log.i(TAG,"HANDLING CHUNKED TRANSFER INFO");
//            handleChunked(value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_RAW_SENSOR_DATA.equals(characteristicUUID)) {
            Log.i(TAG,"HANDLING RAW SENSOR INFO");
//            handleRawSensorData(value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_5_ACTIVITY_DATA.equals(characteristicUUID)) {
            Log.i(TAG,"HANDLING ACTIVITY DATA INFO");
//            fetcher.onActivityData(value);
            return true;
        } else if (HuamiService.UUID_CHARACTERISTIC_5_ACTIVITY_CONTROL.equals(characteristicUUID)) {
            Log.i(TAG,"HANDLING ACTIVITY CONTROL INFO");
//            fetcher.onActivityControl(value);
            return true;
        }  else {
            Log.w(TAG,"Unhandled characteristic changed: "+ characteristicUUID.toString());
//            logMessageContent(value);
            return false;
        }
    }

    private static void handleBatteryInfo(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.i(TAG,"HANDLING BATTERY INFO :: GATT_SUCCESS, value "+value);
            Log.e(TAG,"Battery Get Percentage = " + (value.length >= 2 ? value[1] : "Unknown"));
//            if (value.length >= 2) {
//                return value[1];
//            }
//            return 50; // actually unknown
        }
    }
}
