package com.vikas.gtr2e.beans

import com.vikas.gtr2e.beans.ZeppCloudBeans.BuiltInWatchFace

/**
 * Stores data for the ble device
 * @author Vikas Tiwari
 */
class DeviceInfo {
    var deviceName: String? = null
    var deviceAddress: String? = null
    var serialNumber: String? = null
    var hardwareRevision: String? = null
    var softwareRevision: String? = null
    var systemId: String? = null
    var pnpId: String? = null
    var heartRate: Int = 0
    var steps: Int = 0
    var batteryPercentage: Int = 0
    var batteryStatus: String? = null
    var chargingStatus: String? = null
    var charging: Boolean = false
    var lastChargedOn: String? = null
    var lastKnownChargeLevel: Int = 0
    var authenticated: Boolean = false
    var connected: Boolean = false
    var forceDisconnected: Boolean = false
    var watchFaceList: MutableList<BuiltInWatchFace?>? = null
    var currentWatchFace: BuiltInWatchFace? = null


    fun updateBatteryInfo(batteryInfo: HuamiBatteryInfo?) {
        if (batteryInfo != null) {
            this.batteryPercentage = batteryInfo.getLevelInPercent()
            this.chargingStatus = if (batteryInfo.isCharging()) "Charging" else "Not Charging"
            this.batteryStatus = batteryInfo.getStateString()
            this.charging = batteryInfo.isCharging()
        } else {
            this.batteryStatus = "N/A"
            this.batteryPercentage = 0
            this.chargingStatus = "N/A"
            this.charging = false
        }
    }

    private fun getNullSafeValue(value: String?): String {
        return value ?: ""
    }
}
