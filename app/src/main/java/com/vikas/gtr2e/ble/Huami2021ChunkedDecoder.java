package com.vikas.gtr2e.ble;

import android.annotation.SuppressLint;
import android.util.Log;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.function.Function;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class Huami2021ChunkedDecoder {

    private Byte currentHandle;
    private int currentType;
    private int currentLength;
    ByteBuffer reassemblyBuffer;
    private static final String TAG = "Huami2021ChunkedDecoder";

    // Keep track of last handle and count for acks
    private byte lastHandle;
    private byte lastCount;

    private volatile byte[] sharedSessionKey;

    private Huami2021Handler huami2021Handler;
    private final boolean force2021Protocol;

    public Huami2021ChunkedDecoder(final Huami2021Handler huami2021Handler,
                                   final boolean force2021Protocol) {
        this.huami2021Handler = huami2021Handler;
        this.force2021Protocol = force2021Protocol;
    }

    public void setEncryptionParameters(final byte[] sharedSessionKey) {
        this.sharedSessionKey = sharedSessionKey;
    }

    public void setHuami2021Handler(final Huami2021Handler huami2021Handler) {
        this.huami2021Handler = huami2021Handler;
    }

    public byte getLastHandle() {
        return lastHandle;
    }

    public byte getLastCount() {
        return lastCount;
    }

    public boolean decode(final byte[] data) {
        int i = 0;
        if (data[i++] != 0x03) {
            Log.w(TAG,"Ignoring non-chunked payload");
            return false;
        }
        final byte flags = data[i++];
        final boolean encrypted = ((flags & 0x08) == 0x08);
        final boolean firstChunk = ((flags & 0x01) == 0x01);
        final boolean lastChunk = ((flags & 0x02) == 0x02);
        final boolean needsAck = ((flags & 0x04) == 0x04);

        if (force2021Protocol) {
            i++; // skip extended header
        }
        final byte handle = data[i++];
        if (currentHandle != null && currentHandle != handle) {
            Log.w(TAG,MessageFormat.format("ignoring handle {0}, expected {1}", handle, currentHandle));
            return false;
        }
        lastHandle = handle;
        lastCount = data[i++];
        if (firstChunk) { // beginning
            int full_length = (data[i++] & 0xff) | ((data[i++] & 0xff) << 8) | ((data[i++] & 0xff) << 16) | ((data[i++] & 0xff) << 24);
            currentLength = full_length;
            if (encrypted) {
                int encrypted_length = full_length + 8;
                int overflow = encrypted_length % 16;
                if (overflow > 0) {
                    encrypted_length += (16 - overflow);
                }
                full_length = encrypted_length;
            }
            reassemblyBuffer = ByteBuffer.allocate(full_length);
            currentType = (data[i++] & 0xff) | ((data[i++] & 0xff) << 8);
            currentHandle = handle;
        }
        reassemblyBuffer.put(data, i, data.length - i);
        if (lastChunk) { // end
            byte[] buf = reassemblyBuffer.array();
            if (encrypted) {
                if (sharedSessionKey == null) {
                    // Should never happen
                    Log.w(TAG,"Got encrypted message, but there's no shared session key");
                    reset();
                    return false;
                }

                byte[] messagekey = new byte[16];
                for (int j = 0; j < 16; j++) {
                    messagekey[j] = (byte) (sharedSessionKey[j] ^ handle);
                }
                try {
                    buf = decryptAES(buf, messagekey);
                    buf = subarray(buf, 0, currentLength);
                } catch (Exception e) {
                    Log.w(TAG,MessageFormat.format("error decrypting {}", e.getMessage()));
                    reset();
                    return false;
                }
            }
            Log.d(TAG,MessageFormat.format(
                    "{0} data {1}: {2}",
                    encrypted ? "Decrypted" : "Plaintext",
                    String.format("0x%04x", currentType),
                    hexdump(buf, 0, buf.length))
            );

            try {
                huami2021Handler.handle2021Payload((short) currentType, buf);
            } catch (final Exception e) {
                Log.e(TAG, MessageFormat.format("Failed to handle payload, {0}", e));
            }
            reset();
        }

        return needsAck;
    }

    public void reset() {
        currentHandle = null;
        currentType = 0;
    }
    public static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
    public static String hexdump(byte[] buffer, int offset, int length) {
        if (length == -1) {
            length = buffer.length - offset;
        }

        char[] hexChars = new char[length * 2];
        for (int i = 0; i < length; i++) {
            int v = buffer[i + offset] & 0xFF;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] decryptAES(byte[] value, byte[] secretKey) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        @SuppressLint("GetInstance") Cipher ecipher = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKeySpec newKey = new SecretKeySpec(secretKey, "AES");
        ecipher.init(Cipher.DECRYPT_MODE, newKey);
        return ecipher.doFinal(value);
    }

    public static final byte[] EMPTY_BYTE_ARRAY = {};
    public static byte[] subarray(final byte[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) {
            return null;
        }
        startIndexInclusive = max0(startIndexInclusive);
        endIndexExclusive = Math.min(endIndexExclusive, array.length);
        final int newSize = endIndexExclusive - startIndexInclusive;
        if (newSize <= 0) {
            return EMPTY_BYTE_ARRAY;
        }
        return arraycopy(array, startIndexInclusive, 0, newSize, byte[]::new);
    }
    private static int max0(int other) {
        return Math.max(0, other);
    }
    public static <T> T arraycopy(final T source, final int sourcePos, final int destPos, final int length, final Function<Integer, T> allocator) {
        return arraycopy(source, sourcePos, allocator.apply(length), destPos, length);
    }
    public static <T> T arraycopy(final T source, final int sourcePos, final T dest, final int destPos, final int length) {
        System.arraycopy(source, sourcePos, dest, destPos, length);
        return dest;
    }
}
