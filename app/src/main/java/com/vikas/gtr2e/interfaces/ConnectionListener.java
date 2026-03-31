package com.vikas.gtr2e.interfaces;

import com.vikas.gtr2e.beans.DeviceInfo;
import com.vikas.gtr2e.beans.HuamiBatteryInfo;

import java.util.List;

/**
 * Interface for GTR 2e Manager callbacks to GTR2eApp UI
 * @author Vikas Tiwari
 */
public interface ConnectionListener {
    void onBackgroundServiceBound(boolean bound);
    void onConnectedChanged(boolean connected);
    void onAuthenticated();
    void onBatteryInfoUpdated(HuamiBatteryInfo batteryInfo);
    void onError(String error);
    void onHeartRateChanged(int heartRate);
    void onHeartRateMonitoringChanged(boolean enabled);
    void findPhoneStateChanged(boolean started);
    void pendingBleProcessChanged(int count);
    void onDeviceInfoChanged(DeviceInfo deviceInfo);
    void onWatchFaceSet(boolean success);
    default void onWatchFaceListReceived(List<Integer> watchFaceIds) {}
}