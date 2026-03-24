package com.vikas.gtr2e.utils;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class ConversionUtil {
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
        switch (calValue) {
            case Calendar.SUNDAY:
                return 7;
            default:
                return (byte) (calValue - 1);
        }
    }
    public static GregorianCalendar createCalendar() {
        return new GregorianCalendar();
    }
}
