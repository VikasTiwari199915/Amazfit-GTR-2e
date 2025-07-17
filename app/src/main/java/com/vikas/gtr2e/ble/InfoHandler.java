package com.vikas.gtr2e.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.UUID;

public class InfoHandler {
    public static final String TAG = "InfoHandler";
    
    public static void onInfoReceived(BluetoothGattCharacteristic characteristic, byte[] value, GTR2eBleService.ConnectionCallback connectionCallback) {
        final UUID characteristicUUID = characteristic.getUuid();
        if (HuamiService.UUID_CHARACTERISTIC_6_BATTERY_INFO.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING BATTERY INFO");
            connectionCallback.onBatteryDataReceived(value);
            handleBatteryInfo(value, BluetoothGatt.GATT_SUCCESS);
        } else if (HuamiService.UUID_CHARACTERISTIC_REALTIME_STEPS.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING REALTIME STEPS INFO");
//            handleRealtimeSteps(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING HEART RATE INFO");
//            handleHeartrate(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_AUTH.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING AUTHENTICATION INFO");
            Log.i(TAG, "AUTHENTICATION?? " + characteristicUUID);
//            logMessageContent(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_DEVICEEVENT.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING DEVICE EVENT INFO");
//            handleDeviceEvent(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_WORKOUT.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING WORKOUT INFO");
//            handleDeviceWorkoutEvent(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_7_REALTIME_STEPS.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING 7CHARACTERISTIC REALTIME STEPS INFO");
            Log.i(TAG, MessageFormat.format("HANDLING 7CHARACTERISTIC REALTIME STEPS INFO :: {0} = {1}, byte={2}",
                    BleNamesResolver.resolveCharacteristicName(characteristicUUID.toString()),
                    new String(value), Arrays.toString(value)
            ));
            handleRealtimeSteps(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING CONFIGURATION INFO");
//            handleConfigurationInfo(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING CHUNKED TRANSFER INFO");
//            handleChunked(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_RAW_SENSOR_DATA.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING RAW SENSOR INFO");
//            handleRawSensorData(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_5_ACTIVITY_DATA.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING ACTIVITY DATA INFO");
//            fetcher.onActivityData(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_5_ACTIVITY_CONTROL.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING ACTIVITY CONTROL INFO");
//            fetcher.onActivityControl(value);
        } else {
            Log.i(TAG, "HANDLING UNHANDLED INFO");
            if(BleNamesResolver.mCharacteristics.containsKey(characteristicUUID.toString())) {
                Log.i(TAG, MessageFormat.format("Unhandled characteristic :: {0} = {1}, byte={2}",
                        BleNamesResolver.resolveCharacteristicName(characteristicUUID.toString()),
                        new String(value), Arrays.toString(value)
                        ));
            }
        }
    }

    private static void handleBatteryInfo(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.i(TAG,"HANDLING BATTERY INFO :: GATT_SUCCESS, value "+value);
            Log.e(TAG,"Battery Get Percentage = " + (value.length >= 2 ? value[1] : "Unknown"));
        }
    }
    private static void handleRealtimeSteps(byte[] value) {
        if (value == null) {
            Log.i(TAG,"realtime steps: value is null");
            return;
        }

        if (value.length == 13) {
            byte[] stepsValue = new byte[] {value[1], value[2]};
            int steps = toUint16(stepsValue);
            Log.e(TAG,"realtime steps: " + steps);
        } else {
            Log.w(TAG,"Unrecognized realtime steps value: " + Arrays.toString(value));
        }
    }
    public static int toUint16(byte... bytes) {
        return (bytes[0] & 0xff) | ((bytes[1] & 0xff) << 8);
    }
}
