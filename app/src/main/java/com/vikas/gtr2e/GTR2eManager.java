package com.vikas.gtr2e;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.vikas.gtr2e.ble.GTR2eBleService;

public class GTR2eManager {
    private static final String TAG = "GTR2eManager";

    private final GTR2eBleService bleService;
    private ConnectionListener connectionListener;
    
    private boolean isConnected = false;
    private boolean isAuthenticated = false;

    public void performAction(String actionCode, String... args) {
        if(actionCode==null) return;
        switch (actionCode) {
            case "DO_NOT_DISTURB_ON":
//                bleService.enableDoNotDisturb();
                bleService.sendReboot();
                break;
            case "HEART_RATE_MONITORING_ON":
//                bleService.heartRateMonitoring(true);
                bleService.continuousHeartRateMonitoring(true);
//                bleService.liftWristToWake(true);
                break;
            case "HEART_RATE_MONITORING_OFF":
//                bleService.heartRateMonitoring(false);
                bleService.continuousHeartRateMonitoring(false);
//                bleService.liftWristToWake(false);
                break;
            case "FIND_WATCH_START":
                bleService.sendFindDeviceCommand(true);
                break;
            case "SET_PHONE_VOLUME":
                bleService.onSetPhoneVolume(Float.parseFloat(args[0]));
                break;
            default:
                break;
        }
    }

    public interface ConnectionListener {
        void onConnectedChanged(boolean connected);
        void onAuthenticated();
        void onBatteryInfoUpdated(HuamiBatteryInfo batteryInfo);
        void onError(String error);
//        void onDeviceInfoUpdated(DeviceInfo info); // New callback for device info
    }
    
    public GTR2eManager(Context context) {
        this.bleService = new GTR2eBleService(context);
        initBleService();
    }
    
    private void initBleService() {
        bleService.setConnectionCallback(new GTR2eBleService.ConnectionCallback() {
            @Override
            public void onDeviceConnected(BluetoothDevice device) {
                Log.d(TAG, "Connected to GTR 2e: " + device.getAddress());
                isConnected = true;
                if (connectionListener != null) {
                    connectionListener.onConnectedChanged(true);
                }
            }
            
            @Override
            public void onDeviceDisconnected() {
                Log.d(TAG, "GTR2eManager: onDeviceDisconnected()");
                isConnected = false;
                isAuthenticated = false;
                if (connectionListener != null) {
                    Log.d(TAG, "Calling connectionListener.onDisconnected()");
                    connectionListener.onConnectedChanged(false);
                }
            }
            
            @Override
            public void onBatteryDataReceived(byte[] data) {
                HuamiBatteryInfo.processReceivedBatteryData(data, connectionListener);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "BLE Error: " + error);
                if (connectionListener != null) {
                    connectionListener.onError(error);
                }
            }

//            @Override
//            public void onServicesReady() {
//                Log.d(TAG, "Services and notifications ready");
//                // Authentication will be handled automatically by the BLE service
//                // Once authenticated, we can request battery info
//            }
            
            @Override
            public void onAuthenticated() {
                Log.d(TAG, "Authentication successful");
                isAuthenticated = true;
                if (connectionListener != null) {
                    connectionListener.onAuthenticated();
                }
            }

        });
    }
    
    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }
    
    public void startScan() {
        Log.d(TAG, "Starting scan for GTR 2e devices");
        connect(null);
    }
    
    public void connect(BluetoothDevice device) {
        bleService.connect(device);
    }
    
    public void disconnect() {
        Log.d(TAG, "Disconnecting from device");
        bleService.disconnect();
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public boolean isAuthenticated() {
        return isAuthenticated;
    }
    


} 