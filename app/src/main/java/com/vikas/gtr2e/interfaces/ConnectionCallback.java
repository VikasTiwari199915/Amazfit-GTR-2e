package com.vikas.gtr2e.interfaces;

import android.bluetooth.BluetoothDevice;

import com.vikas.gtr2e.beans.DeviceInfo;
import com.vikas.gtr2e.beans.HuamiBatteryInfo;

import java.util.List;

/**
 * Interface for BLE service callbacks to GTR2eManager
 * @author Vikas Tiwari
 */
public interface ConnectionCallback {
    void onDeviceConnected(BluetoothDevice device);
    void onDeviceDisconnected();
    void onBatteryDataReceived(HuamiBatteryInfo batteryInfo);
    void onHeartRateChanged(int heartRate);
    void onHeartRateMonitoringChanged(boolean enabled);
    void findPhoneStateChanged(boolean started);
    void pendingBleProcessChanged(int count);
    void onDeviceInfoChanged(DeviceInfo deviceInfo);
    void onCallRejected();
    void setMtu(int mtu);
    void onWatchFaceSet(boolean success);
    void onError(String error);
    void onAuthenticated();
    default void onWatchFaceListReceived(List<Integer> watchFaceIds) {}
}