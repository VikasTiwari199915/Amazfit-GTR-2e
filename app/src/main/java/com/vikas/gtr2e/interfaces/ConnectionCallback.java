package com.vikas.gtr2e.interfaces;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.vikas.gtr2e.beans.DeviceInfo;
import com.vikas.gtr2e.beans.HuamiBatteryInfo;

import java.util.List;

/**
 * Interface for BLE service callbacks to GTR2eManager
 * @author Vikas Tiwari
 */
public interface ConnectionCallback {
    String TAG = "ConnectionCallback";
    default void onDeviceConnected(BluetoothDevice device){}
    default void onDeviceDisconnected(){ Log.d(TAG, "GTR2eManager: onDeviceDisconnected()");}
    default void onBatteryDataReceived(HuamiBatteryInfo batteryInfo){}
    default void onHeartRateChanged(int heartRate){Log.d(TAG, "onHeartRateChanged: " + heartRate);}
    default void onHeartRateMonitoringChanged(boolean enabled){}
    default void findPhoneStateChanged(boolean started){Log.d(TAG, "findPhoneStateChanged: " + started);}
    default void pendingBleProcessChanged(int count){}
    default void onDeviceInfoChanged(DeviceInfo deviceInfo){}
    default void onCallRejected(){}
    default void setMtu(int mtu){}
    default void onWatchFaceSet(boolean success){}
    default void onError(String error){}
    default void onAuthenticated(){}
    default void onWatchFaceListReceived(List<Integer> watchFaceIds) {}
    default void onCurrentWatchFace(int id){}
}