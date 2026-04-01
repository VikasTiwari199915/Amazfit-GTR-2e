package com.vikas.gtr2e.interfaces;

import com.vikas.gtr2e.beans.DeviceInfo;
import com.vikas.gtr2e.beans.HuamiBatteryInfo;

import java.util.List;

/**
 * Interface for GTR 2e Manager callbacks to GTR2eApp UI
 * @author Vikas Tiwari
 */
public interface ConnectionListener {
    default void onBackgroundServiceBound(boolean bound) {}
    default void onConnectedChanged(boolean connected) {}
    default void onAuthenticated() {}
    default void onBatteryInfoUpdated(HuamiBatteryInfo batteryInfo) {}
    default void onError(String error) {}
    default void onHeartRateChanged(int heartRate) {}
    default void onHeartRateMonitoringChanged(boolean enabled) {}
    default void findPhoneStateChanged(boolean started) {}
    default void pendingBleProcessChanged(int count) {}
    default void onDeviceInfoChanged(DeviceInfo deviceInfo) {}
    default void onWatchFaceSet(boolean success) {}
    default void onWatchFaceListReceived(List<Integer> watchFaceIds) {}
    default void onCurrentWatchFace(int id){}
}