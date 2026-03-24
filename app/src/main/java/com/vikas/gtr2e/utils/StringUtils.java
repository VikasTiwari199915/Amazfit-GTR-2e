package com.vikas.gtr2e.utils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class StringUtils {

    @NonNull
    public static String truncate(String s, int maxLength){
        if (s == null) {
            return "";
        }

        int length = Math.min(s.length(), maxLength);
        if(length < 0) {
            return "";
        }

        return s.substring(0, length);
    }

    /**
     * Truncate a string to a certain maximum number of bytes, assuming UTF-8 encoding.
     * Does not include the null terminator. Due to multi-byte characters, it's possible
     * that the resulting array is smaller than len, but never larger.
     */
    public static byte[] truncateToBytes(final String s, final int len) {
        if (StringUtils.isNullOrEmpty(s)) {
            return new byte[]{};
        }

        int i = 0;
        while (++i < s.length()) {
            final String subString = s.substring(0, i + 1);
            if (subString.getBytes(StandardCharsets.UTF_8).length > len) {
                break;
            }
        }

        return s.substring(0, i).getBytes(StandardCharsets.UTF_8);
    }

    public static int utf8ByteLength(String string, int length) {
        if (string == null) {
            return 0;
        }
        ByteBuffer outBuf = ByteBuffer.allocate(length);
        CharBuffer inBuf = CharBuffer.wrap(string.toCharArray());
        StandardCharsets.UTF_8.newEncoder().encode(inBuf, outBuf, true);
        return outBuf.position();
    }

    public static String pad(String s, int length){
        return pad(s, length, ' ');
    }

    public static String pad(String s, int length, char padChar) {
        StringBuilder sBuilder = new StringBuilder(s);
        while (sBuilder.length() < length) {
            sBuilder.append(padChar);
        }
        s = sBuilder.toString();
        return s;
    }

    /**
     * Joins the given elements and adds a separator between each element in the resulting string.
     * There will be no separator at the start or end of the string. There will be no consecutive
     * separators (even in case an element is null or empty).
     * @param separator the separator string
     * @param elements the elements to concatenate to a new string
     * @return the joined strings, separated by the separator
     */
    @NonNull
    public static StringBuilder join(String separator, String... elements) {
        StringBuilder builder = new StringBuilder();
        if (elements == null) {
            return builder;
        }
        boolean hasAdded = false;
        for (String element : elements) {
            if (element != null && element.length() > 0) {
                if (hasAdded) {
                    builder.append(separator);
                }
                builder.append(element);
                hasAdded = true;
            }
        }
        return builder;
    }

    @NonNull
    public static String getFirstOf(String first, String second) {
        if (first != null && !first.isEmpty()) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return "";
    }

    public static boolean isNullOrEmpty(CharSequence string){
        return string == null || string.length() == 0;
    }

    public static boolean isEmpty(CharSequence string) {
        return string != null && string.length() == 0;
    }

    public static String ensureNotNull(String message) {
        if (message != null) {
            return message;
        }
        return "";
    }

    public static String terminateNull(String input) {
        if (input == null || input.length() == 0) {
            return new String(new byte[]{(byte) 0});
        }
        char lastChar = input.charAt(input.length() - 1);
        if (lastChar == 0) return input;

        byte[] newArray = new byte[input.getBytes().length + 1];
        System.arraycopy(input.getBytes(), 0, newArray, 0, input.getBytes().length);

        newArray[newArray.length - 1] = 0;

        return new String(newArray);
    }

    @Nullable
    public static String untilNullTerminator(final ByteBuffer buf) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        while (buf.position() < buf.limit()) {
            final byte b = buf.get();

            if (b == 0) {
                return baos.toString();
            }

            baos.write(b);
        }

        return null;
    }

    public static byte[] hexToBytes(String hexString) {
        if((hexString.length() % 2) == 1) {
            // pad with zero
            hexString = "0" + hexString;
        }
        byte[] bytes = new byte[hexString.length() / 2];
        for(int i = 0; i < bytes.length; i++) {
            String slice = hexString.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(slice, 16);
        }

        return bytes;
    }

}
