package com.vikas.gtr2e.interfaces;

import android.bluetooth.BluetoothDevice;

import com.vikas.gtr2e.beans.DeviceInfo;
import com.vikas.gtr2e.beans.HuamiBatteryInfo;

/**
 * Interface for BLE connection callbacks
 * @author Vikas Tiwari
 */
public interface ConnectionCallback {
    void onDeviceConnected(BluetoothDevice device);

    void onDeviceDisconnected();

    void onAuthenticated();

    void onError(String error);

    void onBatteryDataReceived(HuamiBatteryInfo batteryInfo);

    void onHeartRateChanged(int heartRate);

    void onHeartRateMonitoringChanged(boolean enabled);

    void findPhoneStateChanged(boolean started);

    void pendingBleProcessChanged(int count);

    void onDeviceInfoChanged(DeviceInfo deviceInfo);

    void onCallRejected();

    void setMtu(int mtu);
}