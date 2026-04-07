package com.vikas.gtr2e.beans;

import com.vikas.gtr2e.watchFeatureUtilities.GTR2eChargeAnalyzer;

import lombok.Data;

/**
 * Stores data for the ble device
 * @author Vikas Tiwari
 */
@Data
public class DeviceInfo {
    String deviceName;
    String deviceAddress;
    String serialNumber;
    String hardwareRevision;
    String softwareRevision;
    String systemId;
    String pnpId;
    int heartRate;
    int steps;
    int batteryPercentage;
    String batteryStatus;
    String chargingStatus;
    boolean charging;
    String lastChargedOn;
    int lastKnownChargeLevel;
    boolean authenticated;
    boolean connected;
    boolean forceDisconnected;
    long etaChargeMinutes;
    GTR2eChargeAnalyzer.Result chargeAnalysisResult;

    public void updateBatteryInfo(HuamiBatteryInfo batteryInfo, GTR2eChargeAnalyzer.Result chargeAnalysisResult) {
        if(batteryInfo!=null) {
            this.batteryPercentage = batteryInfo.getLevelInPercent();
            this.chargingStatus = batteryInfo.isCharging() ? "Charging" : "Not Charging";
            this.batteryStatus = batteryInfo.getStateString();
            this.charging = batteryInfo.isCharging();
            this.etaChargeMinutes = batteryInfo.etaChargeMinutes;
        } else {
            this.batteryStatus = "N/A";
            this.batteryPercentage = 0;
            this.chargingStatus = "N/A";
            this.charging = false;
            this.etaChargeMinutes = -1;
        }
        if(chargeAnalysisResult!=null) {
            this.chargeAnalysisResult = chargeAnalysisResult;
        }
    }

    private String getNullSafeValue(String value) {
        return null != value ? value : "";
    }


}
