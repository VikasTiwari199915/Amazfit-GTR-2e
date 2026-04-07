package com.vikas.gtr2e.watchFeatureUtilities;

import android.util.Log;
import androidx.annotation.NonNull;

import com.vikas.gtr2e.enums.AlertCategory;
import com.vikas.gtr2e.utils.StringUtils;

import java.util.HashMap;

/**
 * Provides utility methods for Watch Notifications
 * @author Vikas Tiwari
 */
public class GTR2eNotificationUtil {
    public static final int NOTIFICATION_WRITE_TYPE = 0;

    public static byte[] getNotificationData(String packageName, String sourceName, String message) {
        byte customIconId = GTR2eNotificationIcon.getIcon(packageName);
        return getNotificationData(sourceName, message, customIconId);
    }

    private static byte[] getNotificationData(String sourceName, String message, Byte iconId) {
        return getNotificationData(sourceName, message, iconId, null);
    }
    //Logic extracted from Gadgetbridge project
    private static byte[] getNotificationData(String sourceName, String message, Byte iconId, AlertCategory category) {
        final int maxLength = 230;
        final int maxMessageLimit = 1000;
        final byte customIconId = iconId;
        AlertCategory alertCategory = getAlertCategory(category, customIconId);
        int prefixlength = 2;
        // We also need a (fake) source name for Mi Band 3 for SMS/EMAIL, else the message is not displayed
        byte[] rawMessage = "\0 \0".getBytes();
        int suffixlength = rawMessage.length;

        if (alertCategory == AlertCategory.CustomHuami) {
            int length = Math.min(message.length(), maxMessageLimit);
            if (length < message.length()) {
                message = message.substring(0, maxMessageLimit - 3);
                message += "...";
                Log.e("MESSAGE", "NEW Length" + message.length());
            }
            String messageBlock = "\0" + StringUtils.getFirstOf(message, "") + "\0";
            prefixlength = 3;

            rawMessage = messageBlock.getBytes();
            Log.e("MESSAGE", "Message Length : " + rawMessage.length);
            suffixlength = rawMessage.length;
        }
        prefixlength += 4;

        // final step: build command
        byte[] rawAppName = sourceName.getBytes();
        Log.e("MESSAGE", "App Name Length : " + rawAppName.length);
        int length = Math.min(rawAppName.length, maxLength - prefixlength);
        if (length < rawAppName.length) {
            length = StringUtils.utf8ByteLength(sourceName, length);
        }

        byte[] command = new byte[length + prefixlength + suffixlength];
        int pos = 0;
        command[pos++] = (byte) alertCategory.getId();
        command[pos++] = 0;
        command[pos++] = 0;
        command[pos++] = 0;
        command[pos++] = 0;
        command[pos++] = 1;
        if (alertCategory == AlertCategory.CustomHuami) {
            command[pos] = customIconId;
        }

        System.arraycopy(rawAppName, 0, command, prefixlength, length);
        System.arraycopy(rawMessage, 0, command, prefixlength + length, rawMessage.length);
        return command;
    }

    @NonNull
    private static AlertCategory getAlertCategory(AlertCategory category, byte customIconId) {
        AlertCategory alertCategory = AlertCategory.CustomHuami;
        if(category !=null) {
            alertCategory = category;
        }
        // The SMS icon for AlertCategory.SMS is unique and not available as iconId
        if (customIconId == GTR2eNotificationIcon.WECHAT) {
            alertCategory = AlertCategory.SMS;
        }
        // EMAIL icon does not work in FW 0.0.8.74, it did in 0.0.7.90
        else if (customIconId == GTR2eNotificationIcon.EMAIL) {
            alertCategory = AlertCategory.Email;
        }
        return alertCategory;
    }

    /**
     * Sends a alert on watch with custom screen and message
     * [name] nudged you
     * @param name name of the person
     * @return byte array of the command which needs to be written on device
     */
    public static byte[] getNudgedAlertData(String name) {
        return getNotificationData(name, "", GTR2eNotificationIcon.HR_NUDGED);
    }

    /**
     * Sends a weather alert on watch
     * @param weather Weather description string
     * @return byte array of the command which needs to be written on device
     */
    public static byte[] getWeatherAlertData(String weather, String weather2) {
        return getNotificationData(weather, weather2, GTR2eNotificationIcon.WEATHER_ALERT);
    }

    /**
     * Shows a alarm screen on the watch
     * There seems to be some issue which stops next notifications sent to device not appear on the watch, needs fix
     * @return byte array of the command which needs to be written on device
     */
    public static byte[] getAlarmAlertData() {
        return getNotificationData("","", GTR2eNotificationIcon.ALARM_CLOCK);
    }

    /**
     * create a Missed call alert on the watch
     * @param contactName contact name or phone number of the missed call
     * @return byte array of the command which needs to be written on device
     */
    public static byte[] getMissedCallAlertData(String contactName) {
        return getNotificationData(contactName,"", GTR2eNotificationIcon.APP_11, AlertCategory.MissedCall);
    }

    /**
     * create a Incoming call alert on the watch
     * @param contactName contact name or phone number of the missed call
     * @return byte array of the command which needs to be written on device
     */
    public static byte[] getIncomingCallAlertData(String contactName) {
        return getNotificationData(contactName,"", GTR2eNotificationIcon.APP_11, AlertCategory.IncomingCall);
    }

    public static boolean shouldUseSenderAsSource(String packageName, int channelId) {
        return senderAsSourceMap.containsKey(packageName);
    }

    private static final HashMap<String, Integer> senderAsSourceMap = new HashMap<>();
    static {
        senderAsSourceMap.put("com.whatsapp", 1);
        senderAsSourceMap.put("com.telegram.messenger", 2);
        senderAsSourceMap.put("jp.naver.line.android", 3);
        senderAsSourceMap.put("com.snapchat.android", 3);
        senderAsSourceMap.put("org.telegram.messenger.beta", 3);
        senderAsSourceMap.put("org.telegram.messenger.web", 3);
        senderAsSourceMap.put("org.telegram.plus", 3);
    }

}
