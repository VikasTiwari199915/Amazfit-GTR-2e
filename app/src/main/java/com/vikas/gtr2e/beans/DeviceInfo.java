package com.vikas.gtr2e.beans;

import androidx.annotation.NonNull;

import com.vikas.gtr2e.HuamiBatteryInfo;

public class DeviceInfo {
    String deviceName;
    String deviceAddress;
    String serialNumber;
    String hardwareRevision;
    String softwareRevision;
    String systemId;
    String pnpId;
    int batteryPercentage;
    String batteryStatus;
    String chargingStatus;
    String lastChargedOn;
    int lastKnownChargeLevel;
    boolean isAuthenticated;
    boolean isConnected;

    // Constructor without parameters
    public DeviceInfo() {}

    // Constructor to initialize with device address
    public DeviceInfo(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    //Getters and setters for all fields
    public String getDeviceName() {
        return getNullSafeValue(deviceName);
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceAddress() {
        return getNullSafeValue(deviceAddress);
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public String getSerialNumber() {
        return getNullSafeValue(serialNumber);
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getHardwareRevision() {
        return getNullSafeValue(hardwareRevision);
    }

    public void setHardwareRevision(String hardwareRevision) {
        this.hardwareRevision = hardwareRevision;
    }

    public String getSoftwareRevision() {
        return getNullSafeValue(softwareRevision);
    }

    public void setSoftwareRevision(String softwareRevision) {
        this.softwareRevision = softwareRevision;
    }

    public String getSystemId() {
        return getNullSafeValue(systemId);
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getPnpId() {
        return getNullSafeValue(pnpId);
    }

    public void setPnpId(String pnpId) {
        this.pnpId = pnpId;
    }

    public int getBatteryPercentage() {
        return batteryPercentage;
    }

    public void setBatteryPercentage(int batteryPercentage) {
        this.batteryPercentage = batteryPercentage;
    }

    public String getBatteryStatus() {
        return getNullSafeValue(batteryStatus);
    }

    public void setBatteryStatus(String batteryStatus) {
        this.batteryStatus = batteryStatus;
    }

    public String getChargingStatus() {
        return getNullSafeValue(chargingStatus);
    }

    public void setChargingStatus(String chargingStatus) {
        this.chargingStatus = chargingStatus;
    }

    public String getLastChargedOn() {
        return getNullSafeValue(lastChargedOn);
    }

    public void setLastChargedOn(String lastChargedOn) {
        this.lastChargedOn = lastChargedOn;
    }

    public int getLastKnownChargeLevel() {
        return lastKnownChargeLevel;
    }

    public void setLastKnownChargeLevel(int lastKnownChargeLevel) {
        this.lastKnownChargeLevel = lastKnownChargeLevel;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    // toString method to provide a string representation of the object
    @NonNull
    @Override
    public String toString() {
        return "DeviceInfo{" +
                "deviceName='" + deviceName + '\'' +
                ", deviceAddress='" + deviceAddress + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", hardwareRevision='" + hardwareRevision + '\'' +
                ", softwareRevision='" + softwareRevision + '\'' +
                ", systemId='" + systemId + '\'' +
                ", pnpId='" + pnpId + '\'' +
                ", batteryPercentage=" + batteryPercentage +
                ", batteryStatus='" + batteryStatus + '\'' +
                ", chargingStatus='" + chargingStatus + '\'' +
                ", lastChargedOn='" + lastChargedOn + '\'' +
                ", lastKnownChargeLevel=" + lastKnownChargeLevel +
                ", isAuthenticated=" + isAuthenticated +
                ", isConnected=" + isConnected +
                '}';
    }

    public void updateBatteryInfo(HuamiBatteryInfo batteryInfo) {
        this.batteryPercentage = batteryInfo.getLevelInPercent();
        this.chargingStatus = batteryInfo.isCharging() ? "Charging" : "Not Charging";
        this.batteryStatus = batteryInfo.getStateString();
    }

    private String getNullSafeValue(String value) {
        return null != value ? value : "";
    }


}
