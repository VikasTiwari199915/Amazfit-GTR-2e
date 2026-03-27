package com.vikas.gtr2e.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import com.vikas.gtr2e.GTR2eApp;
import com.vikas.gtr2e.utils.MediaUtil;

import java.util.Objects;

public class GTR2eVolumeChangeReceiver extends BroadcastReceiver {
    public static final String TAG = "GTR2eVolChangeReceiver";
    public static final String ANDROID_MEDIA_EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    public static final String ANDROID_MEDIA_EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "onReceive called: "+intent.getAction()+":"+ Objects.requireNonNull(intent.getExtras()));
        Bundle bundle = intent.getExtras();
        if(bundle!=null && bundle.getInt(ANDROID_MEDIA_EXTRA_VOLUME_STREAM_TYPE)== AudioManager.STREAM_MUSIC) {
            int currentVolume = bundle.getInt(ANDROID_MEDIA_EXTRA_VOLUME_STREAM_VALUE, -1);
            if(currentVolume!=-1 && getBleService()!=null) {
                getBleService().onSetPhoneVolume(MediaUtil.getPhoneVolume(getBleService().getApplicationContext()));

            }
        }
    }

    private GTR2eBleService getBleService() {
        if(GTR2eApp.getGTR2eManager()!=null) {
            return GTR2eApp.getGTR2eManager().getBleService();
        } else {
            return null;
        }
    }

}
