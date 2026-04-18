package com.vikas.gtr2e.watchFeatureUtilities;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.vikas.gtr2e.ble.AbstractHuamiFirmwareInfo;
import com.vikas.gtr2e.ble.HuamiService;
import com.vikas.gtr2e.ble.UriHelper;
import com.vikas.gtr2e.enums.HuamiFirmwareType;
import com.vikas.gtr2e.interfaces.FirmwareUpdateListener;
import com.vikas.gtr2e.services.GTR2eBleService;
import com.vikas.gtr2e.utils.ArrayUtils;
import com.vikas.gtr2e.utils.ConversionUtil;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import lombok.Setter;

@Setter
public class GTR2eFirmwareUtil extends AbstractHuamiFirmwareInfo {
    private static final String TAG = "GTR2E_FIRMWARE_UTIL";

    public static final byte COMMAND_REQUEST_PARAMETERS = (byte) 0xd0;
    public static final byte COMMAND_UNKNOWN_D1 = (byte) 0xd1;
    public static final byte COMMAND_SEND_FIRMWARE_INFO = (byte) 0xd2;
    public static final byte COMMAND_START_TRANSFER = (byte) 0xd3;
    public static final byte REPLY_UPDATE_PROGRESS = (byte) 0xd4;
    public static final byte COMMAND_COMPLETE_TRANSFER = (byte) 0xd5;
    public static final byte COMMAND_FINALIZE_UPDATE = (byte) 0xd6;

    public static final UUID UUID_CHAR_FIRMWARE_CONTROL =  HuamiService.UUID_CHARACTERISTIC_FIRMWARE_CONTROL;
    public static final UUID UUID_CHAR_FIRMWARE_DATA_CONTROL = HuamiService.UUID_CHARACTERISTIC_FIRMWARE_DATA;

    private static int mChunkLength = -1;

    private FirmwareUpdateListener listener;

    public GTR2eFirmwareUtil(byte[] bytes) {
        super(bytes);
    }

    public static GTR2eFirmwareUtil getFirmwareUtil(Uri uri, Context context) {
        try {
            UriHelper uriHelper = UriHelper.get(uri, context);
            if (uriHelper.getFileSize() > getMaxExpectedFileSize()) {
                throw new IOException("Firmware size is larger than the maximum expected file size of " + getMaxExpectedFileSize());
            }
            try (InputStream in = uriHelper.openInputStream()) {
                byte[] fw = readAllBytes(in);
                Log.e(TAG, Arrays.toString(fw));
                return new GTR2eFirmwareUtil(fw);
            } catch (Exception e) {
                throw new IOException("Error reading firmware file: " + uri, e);
            }

        } catch (IOException e) {
            Log.e(TAG, "Failed to handle the URI", e);
        }
        return null;
    }

    @Override
    public String toVersion(int crc16) {
        return "";
    }

    @Override
    public boolean isGenerallyCompatibleWith(String deviceName) {
        return false;
    }

    @Override
    protected Map<Integer, String> getCrcMap() {
        return Collections.emptyMap();
    }

    private static String searchFirmwareVersion(byte[] fwbytes) {
        ByteBuffer buf = ByteBuffer.wrap(fwbytes);
        buf.order(ByteOrder.BIG_ENDIAN);
        while (buf.remaining() > 3) {
            int word = buf.getInt();
            if (word == 0x5625642e) {
                word = buf.getInt();
                if (word == 0x25642e25) {
                    word = buf.getInt();
                    if (word == 0x642e2564) {
                        word = buf.getInt();
                        if (word == 0x00000000) {
                            byte[] version = new byte[8];
                            buf.get(version);
                            return new String(version);
                        }
                    }
                }
            }
        }
        return null;
    }


    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = in.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toByteArray();
    }

    /**
     * The maximum expected file size, in bytes. Files larger than this are assumed to be invalid.
     */
    public static long getMaxExpectedFileSize() {
        return 1024 * 1024 * 16; // 16.0MB
    }


    //Step 1 : Request Update Parameters
    public void startInstallation(@NotNull GTR2eBleService bleService) {
        bleService.setFlashingFirmware(true);
        byte[] bytes = new byte[]{COMMAND_REQUEST_PARAMETERS};
        bleService.enqueueWriteCharacteristic(UUID_CHAR_FIRMWARE_CONTROL, bytes, "Get update capabilities");
    }

    //Step 2 : Send FW Info
    public void sendFwInfo(@NotNull GTR2eBleService bleService) {
        bleService.setFlashingFirmware(true);
        byte[] bytes = buildFirmwareInfoCommand();
        bleService.enqueueWriteCharacteristic(UUID_CHAR_FIRMWARE_CONTROL, bytes, "Sending firmware info");
    }

    private void sendFirmwareSize(@NonNull GTR2eBleService bleService) {
        int fwSize = getFwSize();
        byte[] sizeBytes = ConversionUtil.fromUint32(fwSize);
        if (getFirmwareType() == HuamiFirmwareType.WATCHFACE) {
            byte[] fwBytes = getFwBytes();
            if (ArrayUtils.startsWith(fwBytes, UIHH_HEADER)) {
                bleService.enqueueWriteConfiguration(
                        new byte[]{0x39, 0x00,
                                sizeBytes[0], //fwBytes[22] can work too i guess(vikas tiwari)
                                sizeBytes[1], //fwBytes[23]
                                sizeBytes[2], //fwBytes[24]
                                sizeBytes[3], //fwBytes[25]
                                fwBytes[18],
                                fwBytes[19],
                                fwBytes[20],
                                fwBytes[21]
                        }, "Send firmware size to watch");
            }
        }
    }

    //Step 2.5 : Senf Info [D2]
    private byte[] buildFirmwareInfoCommand() {
        int fwSize = getFwSize();
        byte[] sizeBytes = ConversionUtil.fromUint32(fwSize);
        int crc32 = getCrc32();
        byte[] chunkSizeBytes = ConversionUtil.fromUint16(mChunkLength);
        byte[] crcBytes = ConversionUtil.fromUint32(crc32);
        return new byte[]{
                COMMAND_SEND_FIRMWARE_INFO,
                getFirmwareType().getValue(),
                sizeBytes[0],
                sizeBytes[1],
                sizeBytes[2],
                sizeBytes[3],
                crcBytes[0],
                crcBytes[1],
                crcBytes[2],
                crcBytes[3],
                chunkSizeBytes[0],
                chunkSizeBytes[1],
                0x00, // 0 to update in foreground, 1 for background
                0x00, // index
                0x01, // count
                sizeBytes[0], // total size? right now it is equal to the size above
                sizeBytes[1],
                sizeBytes[2],
                sizeBytes[3]
        };
    }

    //Step 3 : Send Transfer Start Command [D3]
    protected void sendTransferStart(@NotNull GTR2eBleService bleService) {
        byte[] bytes = new byte[]{ COMMAND_START_TRANSFER, 1};
        bleService.enqueueWriteCharacteristic(UUID_CHAR_FIRMWARE_CONTROL, bytes, "Transfer start command");
    }

    /// the maximum payload length supported for one write action
    @IntRange(from = 20L, to = 512L)
    public static int calcMaxWriteChunk(int mtu) {
        // the minimum MTU is 23 (Bluetooth spec)
        int safeMtu = Math.max(23, mtu);

        // GATT_MAX_ATTR_LEN: no larger than 512 (Bluetooth spec)
        // MTU: overhead of simple write must be supported. Some other operations like
        // ATT_PREPARE_WRITE_REQ have even larger overhead so the max BLE MTU is larger than 512+3
        return Math.min(512, safeMtu - 3);
    }

    //Step 4: Send Data Chunk
    private void sendFirmwareDataChunk(@NotNull GTR2eBleService bleService , AbstractHuamiFirmwareInfo info, int offset) {
        byte[] fwbytes = info.getFwBytes();
        int len = fwbytes.length;
        int remaining = len - offset;
        final int packetLength = calcMaxWriteChunk(bleService.getMMTU());

        int chunkLength = mChunkLength;
        if (remaining < mChunkLength) {
            chunkLength = remaining;
        }

        int packets = chunkLength / packetLength;
        int chunkProgress = 0;

        if (remaining <= 0) {
            sendTransferComplete(bleService);
            return;
        }

        for (int i = 0; i < packets; i++) {
            byte[] fwChunk = Arrays.copyOfRange(fwbytes, offset + i * packetLength, offset + i * packetLength + packetLength);
            bleService.enqueueWriteCharacteristic(UUID_CHAR_FIRMWARE_DATA_CONTROL, fwChunk, "Sending Firmware Packets : "+(i+1));
            chunkProgress += packetLength;
        }

        if (chunkProgress < chunkLength) {
            Log.e(TAG,"Writing any remaining data smaller than the packet length");
            byte[] lastChunk = Arrays.copyOfRange(fwbytes, offset + packets * packetLength, offset + packets * packetLength + (chunkLength - chunkProgress));
            bleService.enqueueWriteCharacteristic(UUID_CHAR_FIRMWARE_DATA_CONTROL, lastChunk, "Sending Last Firmware Packets.");
        } else {
            Log.e(TAG, chunkProgress+"/"+chunkLength+" bytes sent");
        }

        int progressPercent = (int) ((((float) (offset + chunkLength)) / len) * 100);
        Log.e(TAG, "Progress : "+progressPercent+"%");
        if (listener != null) {
            listener.onProgress(progressPercent);
        }
    }

    //Step 5 : Send Transfer Complete
    protected void sendTransferComplete(@NotNull GTR2eBleService bleService) {
        byte[] bytes = new byte[]{ COMMAND_COMPLETE_TRANSFER,};
        bleService.enqueueWriteCharacteristic(UUID_CHAR_FIRMWARE_CONTROL, bytes, "Transfer Complete");
    }

    protected void sendFinalize(@NotNull GTR2eBleService bleService) {
        byte[] bytes = new byte[]{ COMMAND_FINALIZE_UPDATE, };
        bleService.enqueueWriteCharacteristic(UUID_CHAR_FIRMWARE_CONTROL, bytes, "Finalise Firmware");
    }

    protected void sendChecksum(@NotNull GTR2eBleService bleService) {
        int crc16 = getCrc16();
        byte[] bytes = ConversionUtil.fromUint16(crc16);
        byte[] value = new byte[]{ HuamiService.COMMAND_FIRMWARE_CHECKSUM, bytes[0], bytes[1], };
        bleService.enqueueWriteCharacteristic(UUID_CHAR_FIRMWARE_CONTROL, value, "Sending FW Checksum");
    }

    public void handleFirmwareRelatedNotifications(byte[] value, @NotNull GTR2eBleService bleService, @NotNull GTR2eFirmwareUtil fwUtil) {
        if (value.length != 3 && value.length != 6 && value.length != 7 && value.length != 11) {
            Log.e(TAG, "Notifications should be 3, 6, 7 or 11 bytes long.");
            Log.e(TAG, ConversionUtil.toHex(value));
            done(bleService);
            return;
        }
        boolean success = (value[2] == HuamiService.SUCCESS) || ((value[1] == REPLY_UPDATE_PROGRESS) && value.length >= 6); // ugly
        if (value[0] == HuamiService.RESPONSE && !success) {
            Log.e(TAG, "Response from watch is not successful");
            Log.e(TAG, ConversionUtil.toHex(value));
            done(bleService);
            return;
        }
        switch (value[1]) {
            case COMMAND_REQUEST_PARAMETERS: {
                mChunkLength = (value[4] & 0xff) | ((value[5] & 0xff) << 8);
                Log.e(TAG, "Got chunk length of " + mChunkLength);
                // It Fails the process somehow
                fwUtil.sendFirmwareSize(bleService);
                break;
            }
            case 0x39:
                Log.e(TAG, "Got Firmware Info notification");
                fwUtil.sendFwInfo(bleService);
                break;
            case COMMAND_SEND_FIRMWARE_INFO: {
                Log.e(TAG, "D2 - Got Firmware INFO notification");
                boolean isSuccessCmd = (value[2] == HuamiService.SUCCESS);
                if(isSuccessCmd) {
                    Log.e(TAG, "Firmware info Acknowledged by watch, Sending transfer start command");
                    sendTransferStart(bleService);
                } else {
                    handleSendFwInfoError(value, bleService);
                }
                break;
            }
            case COMMAND_START_TRANSFER: {
                Log.e(TAG, "D3 - Got Firmware Transfer Start notification");
                boolean isSuccessCmd = (value[2] == HuamiService.SUCCESS);
                if (isSuccessCmd) {
                    Log.e(TAG, "Sending transfer start command Acknowledged by watch, Sendind firmware data chunk");
                    sendFirmwareDataChunk(bleService, this, 0);
                } else {
                    handleSendFwInfoError(value, bleService);
                }
                break;
            }
            case HuamiService.COMMAND_FIRMWARE_START_DATA:
                sendChecksum(bleService);
                break;
            case REPLY_UPDATE_PROGRESS: {
                Log.d(TAG, "Got Firmware Progress notification");
                int offset = (value[2] & 0xff) | ((value[3] & 0xff) << 8) | ((value[4] & 0xff) << 16) | ((value[5] & 0xff) << 24);
                Log.d(TAG,"update progress " + offset + " bytes");
                sendFirmwareDataChunk(bleService,this, offset);
                break;
            }
            case COMMAND_COMPLETE_TRANSFER:
                sendFinalize(bleService);
                if (listener != null) {
                    listener.onEnablingFw();
                }
                break;
            case COMMAND_FINALIZE_UPDATE: {
                Log.d(TAG, "Sending reboot command");
                bleService.sendReboot();
                done(bleService);
                break;
            }
            case HuamiService.COMMAND_FIRMWARE_REBOOT: {
                Log.d(TAG, "Reboot command successfully sent.");
                done(bleService);
                break;
            }
            default: {
                Log.e(TAG, "Unhandled FW notification: " + ConversionUtil.toHex(value));
                done(bleService);
            }
        }
    }

    private void handleSendFwInfoError(byte[] value, @NotNull GTR2eBleService bleService) {
        String errorMsg = "Firmware update failed";
        if(value.length>2 && value[2]==0x0A) {
            errorMsg = "Watch Aborted Firmware Update, Probably Timeout/Invalid State/Aborted By Watch";
        } else {
            errorMsg = "Firmware update failed, Uknown failure : "+ ConversionUtil.toHex(value);
        }
        Log.e(TAG, errorMsg);
        if (listener != null) {
            listener.onError(errorMsg);
        }
        done(bleService);
    }

    private void done(@NotNull GTR2eBleService bleService) {
        bleService.setFlashingFirmware(false);
        bleService.setFwUtil(null);
        if (listener != null) {
            listener.onFinish();
        }
    }

}
