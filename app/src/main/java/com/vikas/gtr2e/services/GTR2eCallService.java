package com.vikas.gtr2e.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.service.notification.StatusBarNotification;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

import com.vikas.gtr2e.GTR2eApp;
import com.vikas.gtr2e.enums.CALL_STATUS;

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
    private static StatusBarNotification activeCallNotification = null;

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        if (call == null || call.getDetails() == null) return;
        
        String callId = String.valueOf(call.hashCode());
        
        Log.d(TAG, "onCallAdded: ID=" + callId + ", State=" + call.getDetails().getState());
        activeCalls.put(callId, call);

        if(call.getDetails().getState() == Call.STATE_RINGING && getBleService() != null) {
            Log.e(TAG, "onCallAdded: "+call.getDetails().getHandle().getSchemeSpecificPart());
            String callerName = call.getDetails().getCallerDisplayName();
            Log.d(TAG, "onCallAdded: Caller name=" + callerName);
            if(callerName==null || callerName.isEmpty()) {
                callerName = call.getDetails().getContactDisplayName();
            }
            if(callerName==null || callerName.isEmpty()){
                callerName = call.getDetails().getHandle().getSchemeSpecificPart();
            }
            getBleService().setCallStatus(CALL_STATUS.INCOMING, callerName);
        }

        call.registerCallback(new Call.Callback() {
            @Override
            public void onStateChanged(Call call, int state) {
                super.onStateChanged(call, state);
                Log.d(TAG, "onStateChanged: " + state);
                if (state == Call.STATE_DISCONNECTED) {
                    activeCalls.remove(String.valueOf(call.hashCode()));
                    if (getBleService() != null) {
                        getBleService().setCallStatus(CALL_STATUS.ENDED, call.getDetails().getCallerDisplayName());
                    }
                } else if (state == Call.STATE_ACTIVE) {
                    if (getBleService() != null) {
                        getBleService().setCallStatus(CALL_STATUS.PICKED, call.getDetails().getCallerDisplayName());
                    }
                }
            }
        });
    }


    private static GTR2eBleService getBleService() {
        if(GTR2eApp.getGTR2eManager()!=null) {
            return GTR2eApp.getGTR2eManager().getBleService();
        } else {
            return null;
        }
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
            if (state == Call.STATE_RINGING || state == Call.STATE_CONNECTING) {
                Log.d(TAG, "rejectActiveCall: Calling call.reject(false, null)");
                call.reject(false, null);
            } else {
                Log.d(TAG, "rejectActiveCall: Calling call.disconnect()");
                call.disconnect();
            }
        }

        if(activeCallNotification!=null) {
            Notification notification = activeCallNotification.getNotification();
            if (notification.actions != null) {
                for (Notification.Action action : notification.actions) {
                    String title = action.title.toString().toLowerCase();
                    if (title.contains("decline") || title.contains("reject") ||
                            title.contains("end") || title.contains("hang up")) {
                        try {
                            action.actionIntent.send();
                            break;
                        } catch (PendingIntent.CanceledException e) {
                            Log.e(TAG, "Error while sending active call action intent", e);
                        }
                    }
                }
            }
            activeCallNotification = null;
        }
    }

    public static void addNotificationForActiveCall(StatusBarNotification sbn) {
        activeCallNotification = sbn;
    }
    public static void removeNotificationForActiveCall(){
        activeCallNotification = null;
        if(getBleService()!=null) {
            getBleService().setCallStatus(CALL_STATUS.ENDED, "");
        }
    }
}
