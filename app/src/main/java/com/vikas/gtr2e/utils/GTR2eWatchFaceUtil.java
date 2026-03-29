package com.vikas.gtr2e.utils;

/**
 * Utility to deal with Watch Face related stuff
 * @author Vikas Tiwari
 */
public class GTR2eWatchFaceUtil {

    /**
     * Set the current watch face from one of the installed watchfaces on the device
     * @param index index (or id, not sure yet) of the watch face
     * @return byte array of the command
     */
    public static byte[] setWatchFaceAtIndex(int index) {
        byte[] cmd = new byte[]{(byte) 254, 0, 0, 1, 0, 0, 0};
        cmd[3] = (byte) index;
        return cmd;
    }
}
