/*  Copyright (C) 2017-2022 Andreas Shimokawa, José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package com.vikas.gtr2e.ble;

import com.vikas.gtr2e.enums.HuamiFirmwareType;
import com.vikas.gtr2e.utils.ArrayUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import lombok.Getter;

public abstract class AbstractHuamiFirmwareInfo {
    private byte[] bytes;

    @Getter
    private final int crc16;
    @Getter
    private final int crc32;

    @Getter
    protected final HuamiFirmwareType firmwareType;


    //Constants
    private static final int FW_OFFSET = 3;

    private static final byte[] FW_HEADER = new byte[]{ 0x20, (byte) 0x99, 0x12, 0x01, 0x08 };
    private static final byte[] GPS_ALMANAC_HEADER = new byte[]{ (byte) 0xa0, (byte) 0x80, 0x08, 0x00, (byte) 0x8b, 0x07 };
    private static final byte[] GPS_CEP_HEADER = new byte[]{ 0x2a, 0x12, (byte) 0xa0, 0x02 };

    // gps detection is totally bogus, just the first 16 bytes
    private static final byte[][] GPS_HEADERS = {  new byte[]{ 0x73, 0x75, 0x68, (byte) 0xd0, 0x70, 0x73, (byte) 0xbb, 0x5a, 0x3e, (byte) 0xc3, (byte) 0xd3, 0x09, (byte) 0x9e, 0x1d, (byte) 0xd3, (byte) 0xc9 } };
    private static final byte[] RES_HEADER = new byte[]{ 0x48, 0x4d, 0x52, 0x45, 0x53 };
    private static final byte[] NEWRES_HEADER = new byte[]{ 0x4e, 0x45, 0x52, 0x45, 0x53 };
    private static final byte[] WATCHFACE_HEADER = new byte[]{ 0x48, 0x4d, 0x44, 0x49, 0x41, 0x4c };
    protected static final byte[] UIHH_HEADER = new byte[]{ 'U', 'I', 'H', 'H' };
    private static final byte[] AGPS_UIHH_HEADER = new byte[]{ 'U', 'I', 'H', 'H', 0x04 };
    private static final byte[] FT_HEADER = new byte[]{ 0x48, 0x4d, 0x5a, 0x4b };
    private static final byte[] NEWFT_HEADER = new byte[]{ 0x4e, 0x45, 0x5a, 0x4b };
    private static final int FONT_TYPE_OFFSET = 0x9;
    private static final int COMPRESSED_RES_HEADER_OFFSET = 0x9;
    private static final int COMPRESSED_RES_HEADER_OFFSET_NEW = 0xd;




    public AbstractHuamiFirmwareInfo(byte[] bytes) {
        this.bytes = bytes;
        this.crc16 = CheckSums.getCRC16(bytes);
        this.crc32 = CheckSums.getCRC32(bytes);
        this.firmwareType = determineFirmwareType(bytes);
    }

    public boolean isHeaderValid() {
        return getFirmwareType() != HuamiFirmwareType.INVALID;
    }

    public void checkValid() throws IllegalArgumentException {
    }

    public int[] getWhitelistedVersions() {
        return ArrayUtils.toIntArray(getCrcMap().keySet());
    }

    /**
     * @return the size of the firmware in number of bytes.
     */
    public int getFwSize() {
        return bytes.length;
    }

    public byte[] getFwBytes() {
        return bytes;
    }

    public int getFirmwareVersion() {
        return getCrc16(); // HACK until we know how to determine the version from the fw bytes
    }

    public void unsetFwBytes() {
        this.bytes = null;
    }


    public abstract String toVersion(int crc16);

    public abstract boolean isGenerallyCompatibleWith(String deviceName);

    protected abstract Map<Integer, String> getCrcMap();

    public HuamiFirmwareType determineFirmwareType(byte[] bytes) {
        if (ArrayUtils.equals(bytes, NEWRES_HEADER, COMPRESSED_RES_HEADER_OFFSET_NEW)) {
            return HuamiFirmwareType.RES_COMPRESSED;
        }
        if (ArrayUtils.equals(bytes, FW_HEADER, FW_OFFSET)) {
            if (searchString32BitAligned(bytes, "Amazfit GTR 2e")) {
                return HuamiFirmwareType.FIRMWARE;
            }
            return HuamiFirmwareType.INVALID;
        }
        if ((ArrayUtils.startsWith(bytes, UIHH_HEADER) && (bytes[4] == 1 || bytes[4] == 2))
                || ArrayUtils.startsWith(bytes, WATCHFACE_HEADER)
                || ArrayUtils.equals(bytes, WATCHFACE_HEADER, COMPRESSED_RES_HEADER_OFFSET_NEW)
                || ArrayUtils.equals(bytes, WATCHFACE_HEADER, COMPRESSED_RES_HEADER_OFFSET)) {
            return HuamiFirmwareType.WATCHFACE;
        }
        if (ArrayUtils.startsWith(bytes, NEWFT_HEADER)) {
            if (bytes[10] == 0x01) {
                return HuamiFirmwareType.FONT;
            } else if (bytes[10] == 0x02) {
                return HuamiFirmwareType.FONT_LATIN;
            }
        }

        if (ArrayUtils.startsWith(bytes, GPS_ALMANAC_HEADER)) {
            return HuamiFirmwareType.GPS_ALMANAC;
        }
        if (ArrayUtils.startsWith(bytes, GPS_CEP_HEADER)) {
            return HuamiFirmwareType.GPS_CEP;
        }

        if (ArrayUtils.startsWith(bytes, AGPS_UIHH_HEADER)) {
            return HuamiFirmwareType.AGPS_UIHH;
        }

        for (byte[] gpsHeader : GPS_HEADERS) {
            if (ArrayUtils.startsWith(bytes, gpsHeader)) {
                return HuamiFirmwareType.GPS;
            }
        }

        return HuamiFirmwareType.INVALID;
    }

    public static boolean searchString32BitAligned(byte[] fwbytes, String findString) {
        ByteBuffer stringBuf = ByteBuffer.wrap((findString + "\0").getBytes());
        stringBuf.order(ByteOrder.BIG_ENDIAN);
        int[] findArray = new int[stringBuf.remaining() / 4];
        for (int i = 0; i < findArray.length; i++) {
            findArray[i] = stringBuf.getInt();
        }

        ByteBuffer buf = ByteBuffer.wrap(fwbytes);
        buf.order(ByteOrder.BIG_ENDIAN);
        while (buf.remaining() > 3) {
            int arrayPos = 0;
            while (arrayPos < findArray.length && buf.remaining() > 3 && (buf.getInt() == findArray[arrayPos])) {
                arrayPos++;
            }
            if (arrayPos == findArray.length) {
                return true;
            }
        }
        return false;
    }
}
