package com.vikas.gtr2e.utils;

import java.util.Calendar;
import java.util.GregorianCalendar;
/*
 * Based on code from Gadgetbridge:
 * https://codeberg.org/Freeyourgadget/Gadgetbridge
 * Licensed under AGPLv3
 *
 * Modifications by Vikas Tiwari
 */
public class ConversionUtil {

    public static String toHex(byte[] data) {
        if(data==null || data.length==0) return "[]";
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    public static byte[] fromUint16(int value) {
        return new byte[] {
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
        };
    }
    public static byte fromUint8(int value) {
        return (byte) (value & 0xff);
    }

    public static byte dayOfWeekToRawBytes(Calendar cal) {
        int calValue = cal.get(Calendar.DAY_OF_WEEK);
        if (calValue == Calendar.SUNDAY) {
            return 7;
        }
        return (byte) (calValue - 1);
    }
    public static GregorianCalendar createCalendar() {
        return new GregorianCalendar();
    }

    public static int toUint32(byte... bytes) {
        return (bytes[0] & 0xff) | ((bytes[1] & 0xff) << 8) | ((bytes[2] & 0xff) << 16) | ((bytes[3] & 0xff) << 24);
    }
    public static byte[] fromUint32(int value) {
        return new byte[] {
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) ((value >> 16) & 0xff),
                (byte) ((value >> 24) & 0xff),
        };
    }
}
