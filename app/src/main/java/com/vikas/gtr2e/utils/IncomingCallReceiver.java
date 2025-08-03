package com.vikas.gtr2e.utils;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.vikas.gtr2e.services.GTR2eBleService;

public class IncomingCallReceiver extends BroadcastReceiver {

    private static final String TAG = "IncomingCallReceiver";
    private final GTR2eBleService bleService;
    private final Context context;
    private int mLastRingerMode = -1;
    private boolean needToRestoreRingerMode = false;

    public IncomingCallReceiver(GTR2eBleService bleService){
        Log.d(TAG, "IncomingCallReceiver constructor called.");
        this.bleService = bleService;
        this.context = bleService.getApplicationContext();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Get your BLE service instance from your manager
        Log.d(TAG, "onReceive called.");
        if (this.bleService == null) return;
        if(intent.getAction()!=null && intent.getAction().equals("com.vikas.gtr2e.MUTE_CALL")){
            setRingerModeToSilent();
        } else {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String callerNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                bleService.setCallStatus(GTR2eBleService.CALL_STATUS.INCOMING, formatCallerId(context, callerNumber));
                onCallStatusChanged();
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                bleService.setCallStatus(GTR2eBleService.CALL_STATUS.PICKED, formatCallerId(context, callerNumber));
                onCallStatusChanged();
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                bleService.setCallStatus(GTR2eBleService.CALL_STATUS.ENDED, "");
                onCallStatusChanged();
            }
        }
    }
    
    private String formatCallerId(Context context, String number) {
        if (number == null) return "Unknown";
        // Optional: Look up contact name if you have contacts permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return ContactHelper.getContactName(context, number);
        }
        return number;
    }

    private void onCallStatusChanged() {
        if (needToRestoreRingerMode) {
            restoreRingerMode();
        }
    }

    private void setRingerModeToSilent() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mLastRingerMode = audioManager.getRingerMode();
        try {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            needToRestoreRingerMode = true;
        } catch (SecurityException e) {
            Log.e(TAG,"SecurityException when trying to set ringer (no permission granted :/ ?), not setting it then.");
        }
    }
    private void restoreRingerMode() {
        if (!needToRestoreRingerMode) return;
        if (mLastRingerMode < 0) return;
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        try {
            audioManager.setRingerMode(mLastRingerMode);
            needToRestoreRingerMode = false;
        } catch (SecurityException e) {
            Log.e(TAG,"SecurityException when trying to set ringer (no permission granted :/ ?), not setting it then.");
        }
    }
}