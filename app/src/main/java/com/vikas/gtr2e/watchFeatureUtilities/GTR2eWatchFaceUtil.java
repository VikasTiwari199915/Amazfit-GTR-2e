package com.vikas.gtr2e.watchFeatureUtilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility to deal with Watch Face related stuff
 * @author Vikas Tiwari
 */
public class GTR2eWatchFaceUtil {

    /**
     * Set the current watch face from one of the installed watchfaces on the device
     * @param id id of the watch face
     * @return byte array of the command
     */
    public static byte[] setWatchFaceById(int id) {
//        byte[] cmd = new byte[]{(byte) 0xFE, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
//        cmd[3] = (byte) id;
//        return cmd;

        byte[] cmd = new byte[7];

        cmd[0] = (byte) 0xFE;
        cmd[1] = 0x00;

        // 3-byte little-endian (with leading 0)
        cmd[2] = 0x00;
        cmd[3] = (byte) (id & 0xFF);         // LSB
        cmd[4] = (byte) ((id >> 8) & 0xFF);  // MSB

        cmd[5] = 0x00;
        cmd[6] = 0x00;

        return cmd;
    }

    /**
     * Command watch to send a List of Watch Face IDs installed on watch
     * @return byte array of the command
     */
    public static byte[] getWatchFaceListCommand() {
        return new byte[]{
                (byte) 0xFF,
                0x03,
                0x00,
                0x00,
                0x00
        };
    }

    /**
     * Parse watch face ids from watch response
     * @param value watch response containing watch face ids
     * @return list of watch face ids
     */
    public static List<Integer> parseWatchFaceList(byte[] value) {
        List<Integer> ids = new ArrayList<>();
        if (value == null || value.length < 8) return ids;
        if ((value[0] & 0xFF) != 0x80) return ids;
        int offset = 8;
        for (int i = offset; i + 3 < value.length; i += 4) {
            int id = (value[i + 2] & 0xFF) | ((value[i + 3] & 0xFF) << 8);
            if (id != 0) {
                ids.add(id);
            }
        }
        return ids;
    }

    public static byte[] getCurrentWatchFace() {
        return new byte[]{ (byte) 0xFE, 0x00, 0x01 };
    }

    public static int parseCurrentWatchFaceId(byte[] value) {
        if (value == null || value.length < 7) return -1;
        // success check
        if (value[4] != 1) return -1;

        int lsb = value[5] & 0xFF;
        int msb = value[6] & 0xFF;

        return (msb << 8) | lsb;
    }
}
