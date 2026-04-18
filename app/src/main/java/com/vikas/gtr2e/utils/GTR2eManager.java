package com.vikas.gtr2e.utils;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.vikas.gtr2e.GTR2eApp;
import com.vikas.gtr2e.beans.DeviceInfo;
import com.vikas.gtr2e.beans.HuamiBatteryInfo;
import com.vikas.gtr2e.interfaces.ConnectionCallback;
import com.vikas.gtr2e.interfaces.ConnectionListener;
import com.vikas.gtr2e.services.GTR2eBleService;
import com.vikas.gtr2e.services.GTR2eCallService;

import java.util.List;

import lombok.Getter;

/**
 * Singleton class for GTR 2e Manager
 * @author Vikas Tiwari
 */
public class GTR2eManager {
    private static final String TAG = "GTR2eManager";

    @Getter
    public GTR2eBleService bleService;
    private ConnectionListener connectionListener;

    private boolean bound = false;
    private static GTR2eManager instance;
    private final Context applicationContext;

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
            case "REQUEST_WATCHFACE_LIST":
                bleService.requestWatchFaceIdList();
                break;
            case "SET_CURRENT_WATCHFACE_ID":
                bleService.setWatchFaceWithId(Integer.parseInt(args[0].trim()));
                break;
            case "TEST":
//                bleService.setCallStatus(GTR2eBleService.CALL_STATUS.INCOMING, "Hello World!");
//                bleService.setTime();
//                bleService.requestWatchFaceIdList();
                bleService.testNotifications("", "","");
                break;
            default:
                break;
        }
    }


    private GTR2eManager(Context context) {
        this.applicationContext = context.getApplicationContext();
        startAndBindService();
    }

    private void initBleServiceCallback() {
        if (bleService == null) {
            Log.e(TAG, "initBleServiceCallback called but bleService is null");
            return;
        }
        bleService.setConnectionCallback(new ConnectionCallback() {
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
            public void onCallRejected() {
                Log.d(TAG, "onCallRejected: Received call reject event from watch. Using InCallService.");
                GTR2eCallService.rejectActiveCall();
            }

            @Override
            public void onWatchFaceSet(boolean success) {
                if (connectionListener != null) {
                    connectionListener.onWatchFaceSet(success);
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

            @Override
            public void onWatchFaceListReceived(List<Integer> watchFaceIds) {
                if (connectionListener != null) {
                    connectionListener.onWatchFaceListReceived(watchFaceIds);
                }
            }

            @Override
            public void onCurrentWatchFace(int id) {
                if (connectionListener != null) {
                    connectionListener.onCurrentWatchFace(id);
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

    public void connectWithBondedDevice() {
        if (bleService == null || !bound) {
             Log.e(TAG, "BLE Service not available for scan");
             if (connectionListener != null) connectionListener.onError("Service not ready for scan");
             return;
        }
        Log.d(TAG, "Starting scan for GTR 2e devices");

        connect();
    }

    public void connect() {
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
        bleService.connect();
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
        if (bleService != null && bound) {
            return bleService.getDeviceInfo().isConnected();
        }
        return false;
    }

    public boolean isAuthenticated() {
        if (bleService != null && bound) {
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
                connectionListener.onConnectedChanged(deviceInfo.isConnected());
                if (deviceInfo.isConnected()) {
                    if (deviceInfo.isAuthenticated()) {
                        connectionListener.onAuthenticated();
                    }
                } else {
                    connectWithBondedDevice();
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

    public static synchronized GTR2eManager getInstance(Activity activity) {
        if (instance == null) {
            Log.e(TAG, "### CREATING NEW INSTANCE ###");
            instance = new GTR2eManager(activity.getApplicationContext());
        } else {
            Log.e(TAG, "--- USING EXISTING INSTANCE ---");
            if (!instance.bound && instance.bleService == null) {
                Log.w(TAG, "GTR2eManager instance exists but not bound. Attempting to re-bind.");
                instance.startAndBindService();
            }
        }
        GTR2eApp.setGTR2eManager(instance);
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
