package com.vikas.gtr2e;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.vikas.gtr2e.beans.DeviceInfo;
import com.vikas.gtr2e.services.GTR2eBleService;
import com.vikas.gtr2e.utils.IncomingCallReceiver;
import com.vikas.gtr2e.utils.Prefs;

public class GTR2eManager {
    private static final String TAG = "GTR2eManager";

    private GTR2eBleService bleService;
    private ConnectionListener connectionListener;

    private boolean bound = false;
    private static GTR2eManager instance;
    private final Context applicationContext; // Renamed for clarity, always application context

    public void performAction(String actionCode, String... args) {
        if (bleService == null || !bound) {
            Log.e(TAG, "BLE Service not available for action: " + actionCode);
            if (connectionListener != null) {
                connectionListener.onError("Service not connected");
            }
            return;
        }
        if (actionCode == null) return;
        switch (actionCode) {
            case "DO_NOT_DISTURB_ON":
//                bleService.enableDoNotDisturb();
                bleService.sendReboot();
                break;
            case "LIFT_WRIST_TO_WAKE_ON":
                bleService.liftWristToWake(true);
                break;
            case "LIFT_WRIST_TO_WAKE_OFF":
                bleService.liftWristToWake(false);
                break;
            case "HEART_RATE_MONITORING_ON":
//                bleService.heartRateMonitoring(true);
                bleService.continuousHeartRateMonitoring(true);
                break;
            case "HEART_RATE_MONITORING_OFF":
//                bleService.heartRateMonitoring(false);
                bleService.continuousHeartRateMonitoring(false);
                break;
            case "FIND_WATCH_START":
                bleService.sendFindDeviceCommand(true);
                break;
            case "SET_PHONE_VOLUME":
                bleService.onSetPhoneVolume(Float.parseFloat(args[0]));
                break;
            case "TEST":
                bleService.setCallStatus(GTR2eBleService.CALL_STATUS.INCOMING, "Hello World!");
            default:
                break;
        }
    }

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
    }

    private GTR2eManager(Context context) {
        this.applicationContext = context.getApplicationContext(); // Ensure it's application context
        startAndBindService();
    }

    private void initBleServiceCallback() {
        if (bleService == null) {
            Log.e(TAG, "initBleServiceCallback called but bleService is null");
            return;
        }
        bleService.setConnectionCallback(new GTR2eBleService.ConnectionCallback() {
            @Override
            public void onDeviceConnected(BluetoothDevice device) {
                Log.d(TAG, "Connected to GTR 2e: " + device.getAddress());
                if (connectionListener != null) {
                    connectionListener.onConnectedChanged(true);
                }
            }

            @Override
            public void onDeviceDisconnected() {
                Log.d(TAG, "GTR2eManager: onDeviceDisconnected()");
                if (connectionListener != null) {
                    Log.d(TAG, "Calling connectionListener.onDisconnected()");
                    connectionListener.onConnectedChanged(false);
                }
            }

            @Override
            public void onBatteryDataReceived(HuamiBatteryInfo batteryInfo) {
                if (connectionListener != null) {
                    connectionListener.onBatteryInfoUpdated(batteryInfo);
                }
            }

            @Override
            public void onHeartRateChanged(int heartRate) {
                if (connectionListener != null) {
                    connectionListener.onHeartRateChanged(heartRate);
                }
            }

            @Override
            public void onHeartRateMonitoringChanged(boolean enabled) {
                if (connectionListener != null) {
                    connectionListener.onHeartRateMonitoringChanged(enabled);
                }
            }

            @Override
            public void findPhoneStateChanged(boolean started) {
                if (connectionListener != null) {
                    connectionListener.findPhoneStateChanged(started);
                }
            }

            @Override
            public void pendingBleProcessChanged(int count) {
                if (connectionListener != null) {
                    connectionListener.pendingBleProcessChanged(count);
                }
            }

            @Override
            public void onDeviceInfoChanged(DeviceInfo deviceInfo) {
                if (connectionListener != null) {
                    connectionListener.onDeviceInfoChanged(deviceInfo);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "BLE Error: " + error);
                if (connectionListener != null) {
                    connectionListener.onError(error);
                }
            }

            @Override
            public void onAuthenticated() {
                Log.d(TAG, "Authentication successful");
                if (connectionListener != null) {
                    connectionListener.onAuthenticated();
                }
            }
        });
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
        if (this.connectionListener != null) {
            this.connectionListener.onBackgroundServiceBound(bound);
        }
    }

    public void startScan() {
        if (bleService == null || !bound) {
             Log.e(TAG, "BLE Service not available for scan");
             if (connectionListener != null) connectionListener.onError("Service not ready for scan");
             return;
        }
        Log.d(TAG, "Starting scan for GTR 2e devices");
        connect(null);
    }

    public void connect(BluetoothDevice device) {
        if (!bound) {
            Log.w(TAG, "Not bound to service, attempting to bind before connect.");
            startAndBindService();
            if (connectionListener != null) connectionListener.onError("Service binding, try connecting shortly.");
            return;
        }
        if (bleService == null) {
             Log.e(TAG, "BLE Service not available for connect");
             if (connectionListener != null) connectionListener.onError("Service not ready to connect");
             return;
        }
        bleService.connect(device);
    }

    public void disconnect() {
        if (bleService == null || !bound) {
             Log.e(TAG, "BLE Service not available for disconnect");
             return;
        }
        Log.d(TAG, "Disconnecting from device");
        bleService.disconnect();
    }

    public boolean isConnected() {
        if (bleService != null && bound && bleService.getDeviceInfo() != null) {
            return bleService.getDeviceInfo().isConnected();
        }
        return false;
    }

    public boolean isAuthenticated() {
        if (bleService != null && bound && bleService.getDeviceInfo() != null) {
            return bleService.getDeviceInfo().isAuthenticated();
        }
        return false;
    }

    public DeviceInfo getDeviceInfo() {
        if (bleService != null && bound) {
            return bleService.getDeviceInfo();
        }
        return null;
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GTR2eBleService.LocalBinder binder = (GTR2eBleService.LocalBinder) service;
            bleService = binder.getService();
            bound = true;
            Log.i(TAG, "Service Connected");
            initBleServiceCallback();

            if (connectionListener != null) {
                connectionListener.onBackgroundServiceBound(true);
                DeviceInfo deviceInfo = bleService.getDeviceInfo();
                if (deviceInfo != null) {
                    connectionListener.onConnectedChanged(deviceInfo.isConnected());
                    if (deviceInfo.isConnected()) {
                        if (deviceInfo.isAuthenticated()) {
                            connectionListener.onAuthenticated();
                        }
                    }
                } else {
                    connectionListener.onConnectedChanged(false);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "Service Disconnected");
            bound = false;
            if (connectionListener != null) {
                connectionListener.onBackgroundServiceBound(false);
                connectionListener.onConnectedChanged(false); // Reflect that connection is lost
                connectionListener.onError("Service has been disconnected");
            }
        }
    };

    public static synchronized GTR2eManager getInstance(Context context) {
        if (instance == null) {
            instance = new GTR2eManager(context.getApplicationContext());
        } else {
            if (!instance.bound && instance.bleService == null) {
                Log.w(TAG, "GTR2eManager instance exists but not bound. Attempting to re-bind.");
                instance.startAndBindService();
            }
        }
        return instance;
    }

    private void startAndBindService() {
        if (bound && bleService != null) {
            Log.i(TAG, "Already bound to service.");
            if(connectionListener != null) {
                 connectionListener.onBackgroundServiceBound(true);
            }
            return;
        }
        Log.i(TAG, "Starting and binding service.");
        Intent serviceIntent = new Intent(applicationContext, GTR2eBleService.class);
        try {
            applicationContext.startForegroundService(serviceIntent);
            applicationContext.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Cannot start foreground service from background", e);
             if (connectionListener != null) {
                connectionListener.onError("Cannot start service from background");
            }
        }
    }

    public void unbindServiceIfNeeded() {
        if (bound && !Prefs.getKeepServiceRunningInBG(applicationContext)) {
            Log.i(TAG, "Unbinding service as per Prefs.");
            applicationContext.unbindService(serviceConnection);
            bound = false;
            bleService = null; // Clean up service instance
            if (connectionListener != null) {
                connectionListener.onBackgroundServiceBound(false);
            }
        } else if (bound) {
            Log.i(TAG, "Service bound and Prefs indicate to keep running. Not unbinding.");
        } else {
            Log.i(TAG, "Service not bound. Nothing to unbind.");
        }
    }


    public GTR2eBleService getBleService() {
        return bleService;
    }

    public void onMainActivityResumed() {
        if (!bound && bleService == null) {
            Log.w(TAG, "GTR2eManager onMainActivityResumed: Not bound. Attempting to re-bind.");
            startAndBindService();
        } else if (bound && bleService != null && connectionListener != null) {
             Log.i(TAG, "GTR2eManager onMainActivityResumed: Bound. Refreshing listener state.");
             connectionListener.onBackgroundServiceBound(true);
        }
    }
}
