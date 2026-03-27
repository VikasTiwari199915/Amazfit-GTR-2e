package com.vikas.gtr2e.services;

import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

import com.vikas.gtr2e.utils.GTR2eManager;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InCallService implementation for GTR2e
 * Used to manage incoming and outgoing calls
 * @author Vikas Tiwari
 */
public class GTR2eCallService extends InCallService {
    private static final String TAG = "GTR2eCallService";
    private static final ConcurrentHashMap<String, Call> activeCalls = new ConcurrentHashMap<>();

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        if (call == null || call.getDetails() == null) return;
        
        String callId = String.valueOf(call.hashCode());
        
        Log.d(TAG, "onCallAdded: ID=" + callId + ", State=" + call.getDetails().getState());
        activeCalls.put(callId, call);

        if(call.getDetails().getState() == Call.STATE_RINGING && getGtr2eManager() != null && getBleService() != null) {
            Log.e(TAG, "onCallAdded: "+call.getDetails().getHandle().getSchemeSpecificPart());
            String callerName = call.getDetails().getCallerDisplayName();
            Log.d(TAG, "onCallAdded: Caller name=" + callerName);
            if(callerName==null || callerName.isEmpty()) {
                callerName = call.getDetails().getContactDisplayName();
            }
            if(callerName==null || callerName.isEmpty()){
                callerName = call.getDetails().getHandle().getSchemeSpecificPart();
            }
            getBleService().setCallStatus(GTR2eBleService.CALL_STATUS.INCOMING, callerName);
        }

        call.registerCallback(new Call.Callback() {
            @Override
            public void onStateChanged(Call call, int state) {
                super.onStateChanged(call, state);
                Log.d(TAG, "onStateChanged: " + state);
                if (state == Call.STATE_DISCONNECTED) {
                    activeCalls.remove(String.valueOf(call.hashCode()));
                    if (getGtr2eManager() != null && getBleService() != null) {
                        getBleService().setCallStatus(GTR2eBleService.CALL_STATUS.ENDED, call.getDetails().getCallerDisplayName());
                    }
                } else if (state == Call.STATE_ACTIVE) {
                    if (getGtr2eManager() != null && getBleService() != null) {
                        getBleService().setCallStatus(GTR2eBleService.CALL_STATUS.PICKED, call.getDetails().getCallerDisplayName());
                    }
                }
            }
        });
    }

    private GTR2eManager getGtr2eManager() {
        return GTR2eManager.getInstance(getApplicationContext());
    }

    private GTR2eBleService getBleService() {
        return getGtr2eManager().getBleService();
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        if (call != null && call.getDetails() != null) {
            String callId = String.valueOf(call.hashCode());
            Log.d(TAG, "onCallRemoved: " + callId);
            activeCalls.remove(callId);
        }
    }

    public static void rejectActiveCall() {
        Log.d(TAG, "rejectActiveCall: Attempting to reject all active calls. Count: " + activeCalls.size());
        if (activeCalls.isEmpty()) {
            Log.w(TAG, "rejectActiveCall: No active calls in map.");
        }

        for (Call call : new ArrayList<>(activeCalls.values())){
            int state = call.getDetails().getState();
            Log.d(TAG, "rejectActiveCall: Processing call state=" + state);
            if (state == Call.STATE_RINGING) {
                Log.d(TAG, "rejectActiveCall: Calling call.reject(false, null)");
                call.reject(false, null);
            } else {
                Log.d(TAG, "rejectActiveCall: Calling call.disconnect()");
                call.disconnect();
            }
        }
    }
}
