package com.vikas.gtr2e.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.UUID;

public class InfoHandler {
    public static final String TAG = "InfoHandler";
    
    public static void onInfoReceived(BluetoothGattCharacteristic characteristic, byte[] value,
                                      GTR2eBleService.ConnectionCallback connectionCallback, GTR2eBleService bleService) {
        final UUID characteristicUUID = characteristic.getUuid();
        if (HuamiService.UUID_CHARACTERISTIC_6_BATTERY_INFO.equals(characteristicUUID)) {
            //Log.i(TAG, "HANDLING BATTERY INFO");
            connectionCallback.onBatteryDataReceived(value);
            handleBatteryInfo(value, BluetoothGatt.GATT_SUCCESS);
        } else if (HuamiService.UUID_CHARACTERISTIC_REALTIME_STEPS.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING REALTIME STEPS INFO");
//            handleRealtimeSteps(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING HEART RATE INFO :: "+Arrays.toString(value));
            if(value.length>1 && value[0]==0 && value[1]!=0) {
                connectionCallback.onHeartRateMonitoringChanged(true);
                connectionCallback.onHeartRateChanged(value[1]);
            } else if(value.length > 1 && value[0] == 0){
                connectionCallback.onHeartRateMonitoringChanged(false);
            }
            //HANDLING HEART RATE INFO :: [0, 80] -- 0=success, 80=heart rate
//            handleHeartrate(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_AUTH.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING AUTHENTICATION INFO :: "+Arrays.toString(value));
            Log.i(TAG, "AUTHENTICATION?? " + characteristicUUID);
//            logMessageContent(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_DEVICEEVENT.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING DEVICE EVENT INFO :: "+Arrays.toString(value));
            //Log.i(TAG, "DEVICE EVENT : " + Arrays.toString(value));
            handleDeviceEvent(value, connectionCallback, bleService);
        } else if (HuamiService.UUID_CHARACTERISTIC_WORKOUT.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING WORKOUT INFO :: "+Arrays.toString(value));
//            handleDeviceWorkoutEvent(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_7_REALTIME_STEPS.equals(characteristicUUID)) {
//            Log.i(TAG, "HANDLING 7CHARACTERISTIC REALTIME STEPS INFO");
//            Log.i(TAG, MessageFormat.format("HANDLING 7CHARACTERISTIC REALTIME STEPS INFO :: {0} = {1}, byte={2}",
//                    BleNamesResolver.resolveCharacteristicName(characteristicUUID.toString()),
//                    new String(value), Arrays.toString(value)
//            ));
            handleRealtimeSteps(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING CONFIGURATION INFO :: "+Arrays.toString(value));
//            handleConfigurationInfo(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING CHUNKED TRANSFER INFO :: "+Arrays.toString(value));
            Log.e(TAG,"CHUNKED TRANSFER INFO String Value :: "+new String(value));
//            handleChunked(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_RAW_SENSOR_DATA.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING RAW SENSOR INFO :: "+Arrays.toString(value));
//            handleRawSensorData(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_5_ACTIVITY_DATA.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING ACTIVITY DATA INFO :: "+Arrays.toString(value));
//            fetcher.onActivityData(value);
        } else if (HuamiService.UUID_CHARACTERISTIC_5_ACTIVITY_CONTROL.equals(characteristicUUID)) {
            Log.i(TAG, "HANDLING ACTIVITY CONTROL INFO :: "+Arrays.toString(value));
//            fetcher.onActivityControl(value);
        } else {
            Log.i(TAG, "HANDLING UNHANDLED INFO");
            if(BleNamesResolver.mCharacteristics.containsKey(characteristicUUID.toString())) {
                Log.i(TAG, MessageFormat.format("Unhandled characteristic :: {0} = {1}, byte={2}",
                        BleNamesResolver.resolveCharacteristicName(characteristicUUID.toString()),
                        new String(value), Arrays.toString(value)
                        ));
            }
        }
    }

    private static void handleBatteryInfo(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.i(TAG,"HANDLING BATTERY INFO :: GATT_SUCCESS, value "+value);
            Log.e(TAG,"Battery Get Percentage = " + (value.length >= 2 ? value[1] : "Unknown"));
        }
    }
    private static void handleRealtimeSteps(byte[] value) {
        if (value == null) {
            Log.i(TAG,"realtime steps: value is null");
            return;
        }

        if (value.length == 13) {
            byte[] stepsValue = new byte[] {value[1], value[2]};
            int steps = toUint16(stepsValue);
            Log.e(TAG,"realtime steps: " + steps);
        } else {
            Log.w(TAG,"Unrecognized realtime steps value: " + Arrays.toString(value));
        }
    }

    private static void handleDeviceEvent(byte[] value, GTR2eBleService.ConnectionCallback connectionCallback, GTR2eBleService bleService) {
        if (value == null || value.length == 0) {
            return;
        }
//        GBDeviceEventCallControl callCmd = new GBDeviceEventCallControl();

        switch (value[0]) {
            case HuamiDeviceEvent.CALL_REJECT:
                Log.i(TAG,"call rejected");
//                callCmd.event = GBDeviceEventCallControl.Event.REJECT;
//                evaluateGBDeviceEvent(callCmd);
                break;
            case HuamiDeviceEvent.CALL_IGNORE:
                Log.i(TAG,"call ignored");
//                callCmd.event = GBDeviceEventCallControl.Event.IGNORE;
//                evaluateGBDeviceEvent(callCmd);
                break;
            case HuamiDeviceEvent.BUTTON_PRESSED:
                Log.i(TAG,"button pressed");
//                handleButtonEvent();
                break;
            case HuamiDeviceEvent.BUTTON_PRESSED_LONG:
                Log.i(TAG,"button long-pressed ");
//                handleLongButtonEvent();
                break;
            case HuamiDeviceEvent.START_NONWEAR:
                Log.i(TAG,"non-wear start detected");
//                evaluateGBDeviceEvent(new GBDeviceEventWearState(WearingState.NOT_WEARING));
                break;
            case HuamiDeviceEvent.ALARM_TOGGLED:
            case HuamiDeviceEvent.ALARM_CHANGED:
                Log.i(TAG,"An alarm was toggled or changed");
//                TransactionBuilder builder = new TransactionBuilder("requestAlarms");
//                requestAlarms(builder);
//                builder.queue(getQueue());
                break;
            case HuamiDeviceEvent.FELL_ASLEEP:
                Log.i(TAG,"Fell asleep");
//                evaluateGBDeviceEvent(new GBDeviceEventSleepStateDetection(SleepState.ASLEEP));
                break;
            case HuamiDeviceEvent.WOKE_UP:
                Log.i(TAG,"Woke up");
//                evaluateGBDeviceEvent(new GBDeviceEventSleepStateDetection(SleepState.AWAKE));
                break;
            case HuamiDeviceEvent.STEPSGOAL_REACHED:
                Log.i(TAG,"Steps goal reached");
                break;
            case HuamiDeviceEvent.TICK_30MIN:
                Log.i(TAG,"Tick 30 min (?)");
                break;
            case HuamiDeviceEvent.FIND_PHONE_START:
                Log.i(TAG,"find phone started");
                connectionCallback.findPhoneStateChanged(true);
                break;
            case HuamiDeviceEvent.FIND_PHONE_STOP:
                Log.i(TAG,"find phone stopped");
                connectionCallback.findPhoneStateChanged(false);
                break;
            case HuamiDeviceEvent.SILENT_MODE:
                final boolean silentModeEnabled = value[1] == 1;
                Log.i(TAG,MessageFormat.format("silent mode = {0}", silentModeEnabled));
//                sendPhoneSilentMode(silentModeEnabled);
//                evaluateGBDeviceEvent(new GBDeviceEventSilentMode(silentModeEnabled));
                break;
            case HuamiDeviceEvent.MUSIC_CONTROL:
                Log.i(TAG,"got music control");
//                GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();

                switch (value[1]) {
                    case 0:
                        Log.i(TAG,"Music control play");
//                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAY;
                        break;
                    case 1:
                        Log.i(TAG,"Music control pause");
//                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PAUSE;
                        break;
                    case 3:
                        Log.i(TAG,"Music control next");
//                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                        break;
                    case 4:
                        Log.i(TAG,"Music control previous");
//                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                        break;
                    case 5:
                        Log.i(TAG,"Music control volume up");
//                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                        break;
                    case 6:
                        Log.i(TAG,"Music control volume down");
//                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                        break;
                    case (byte) 224:
                        Log.i(TAG,"Music control Music app opened");
                        bleService.onMusicAppOpenOnWatch(true);
                        break;
                    case (byte) 225:
                        Log.i(TAG,"Music control Music app closed");
                        bleService.onMusicAppOpenOnWatch(false);
                        break;
                    default:
                        Log.i(TAG,"unhandled music control event " + value[1]);
                        return;
                }
//                evaluateGBDeviceEvent(deviceEventMusicControl);
                break;
            case HuamiDeviceEvent.MTU_REQUEST:
                int mtu = (value[2] & 0xff) << 8 | value[1] & 0xff;
                Log.i(TAG,"device announced MTU of " + mtu);
//                setMtu(mtu);
                /*
                 * not really sure if this would make sense, is this event already a proof of a successful MTU
                 * negotiation initiated by the Huami device, and acknowledged by the phone? do we really have to
                 * requestMTU() from our side after receiving this?
                 * /
                if (mMTU != mtu) {
                    requestMTU(mtu);
                }
                */
                break;
            case HuamiDeviceEvent.WORKOUT_STARTING:
                Log.i(TAG, "Workout Started");
//                final HuamiWorkoutTrackActivityType activityType = HuamiWorkoutTrackActivityType.fromCode(value[3]);
//                final ActivityKind activityKind;

//                if (activityType == null) {
//                    LOG.warn("Unknown workout activity type {}", String.format("0x%02x", value[3]));
//                    activityKind = ActivityKind.UNKNOWN;
//                } else {
//                    activityKind = activityType.toActivityKind();
//                }

                final boolean needsGps = value[2] == 1;

//                Log.i(TAG,"Workout starting on band: {}, needs gps = {}", activityType, needsGps);

//                onWorkoutOpen(needsGps, activityKind);

                break;
            default:
                Log.w("unhandled event {}", String.format("0x%02x", value[0]));
        }
    }



    public static int toUint16(byte... bytes) {
        return (bytes[0] & 0xff) | ((bytes[1] & 0xff) << 8);
    }
}
