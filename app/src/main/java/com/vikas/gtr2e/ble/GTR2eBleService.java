package com.vikas.gtr2e.ble;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;

import static com.vikas.gtr2e.ble.HuamiService.COMMAND_DO_NOT_DISTURB_AUTOMATIC;
import static com.vikas.gtr2e.ble.HuamiService.ENDPOINT_DISPLAY;
import static com.vikas.gtr2e.ble.HuamiService.MUSIC_FLAG_VOLUME;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
@SuppressLint("MissingPermission")
public class GTR2eBleService {
    private static final String TAG = "GTR2eBleService";
    HashMap<UUID, BluetoothGattCharacteristic> characteristicMap = new HashMap<>();

    // UUIDs for Huami protocol
    public static final UUID AUTH_SERVICE_UUID = UUID.fromString("0000fee1-0000-1000-8000-00805f9b34fb");
    private static final UUID AUTH_CHARACTERISTIC_UUID = UUID.fromString("00000009-0000-3512-2118-0009af100700");
    private static final UUID AUTH_CHARACTERISTIC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID HUAMI_SERVICE_UUID = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
    private static final UUID CHUNKED_READ_UUID = UUID.fromString("00000017-0000-3512-2118-0009af100700");

    private static final byte[] DEFAULT_AUTH_KEY = new byte[] {
        (byte)0x6c, (byte)0xd3, (byte)0x1f, (byte)0x60,
        (byte)0x6d, (byte)0xf4, (byte)0xb7, (byte)0x8d,
        (byte)0x3a, (byte)0xfd, (byte)0xcc, (byte)0x0f,
        (byte)0x7b, (byte)0x61, (byte)0x74, (byte)0x84
    };
    private boolean isAuthenticated = false;
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt gatt;
    private ConnectionCallback connectionCallback;

    private Huami2021ChunkedEncoder huami2021ChunkedEncoder;
    private Huami2021ChunkedDecoder huami2021ChunkedDecoder;

    private BluetoothGattCharacteristic characteristicChunked2021Write;
    private BluetoothGattCharacteristic characteristicChunked2021Read;


    protected static final int MIN_MTU = 23;
    private int mMTU = MIN_MTU;

    private final Queue<Runnable> bleOperations = new LinkedList<>();
    private boolean isBleBusy = false;

    public interface ConnectionCallback {
        void onDeviceConnected(BluetoothDevice device);
        void onDeviceDisconnected();
        void onAuthenticated();
        void onError(String error);
        void onBatteryDataReceived(byte[] batteryData);
    }

    Huami2021Handler huami2021Handler = new Huami2021Handler() {
        @Override
        public void handle2021Payload(short type, byte[] payload) {
            handleChunkedRead(payload);
        }
    };

    public GTR2eBleService(Context context) {
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        huami2021ChunkedDecoder = new Huami2021ChunkedDecoder(huami2021Handler, false);
    }

    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }

    @SuppressLint("MissingPermission")
    public void connect(BluetoothDevice device) {
//        Log.d(TAG, "Connecting to device: " + device.getAddress());
        isAuthenticated = false;
        BluetoothDevice device1 =  bluetoothAdapter.getRemoteDevice("D9:B6:53:69:F2:F2");
        gatt = device1.connectGatt(context, false, gattCallback);
    }

    public void disconnect() {
        Log.d(TAG, "Disconnecting from device");
        if (gatt != null) {
            try {
                gatt.disconnect();
                gatt.close();
            } catch (Exception e) {
                Log.e(TAG, "Error during disconnect/close", e);
            }
            gatt = null;
        }
        isAuthenticated = false;
        if (connectionCallback != null) {
            connectionCallback.onDeviceDisconnected();
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange: status=" + status + ", newState=" + newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to GATT server, requesting MTU 247");
                    gatt.requestMtu(247);
                    if (connectionCallback != null) {
                        connectionCallback.onDeviceConnected(gatt.getDevice());
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from GATT server");
                    if (connectionCallback != null) {
                        connectionCallback.onDeviceDisconnected();
                    }
                }
            } else {
                Log.e(TAG, "Connection state change failed: " + status);
                if (connectionCallback != null) {
                    connectionCallback.onError("Connection failed: " + status);
                }
            }
        }


        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d(TAG, "onMtuChanged: mtu=" + mtu + ", status=" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "MTU set (actual value: " + mtu + "), proceeding to service discovery");
                gatt.discoverServices();
            } else {
                Log.e(TAG, "Failed to set MTU, status=" + status);
            }
            if (huami2021ChunkedEncoder != null) {
                huami2021ChunkedEncoder.setMTU(mtu);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            Log.d(TAG, "onServicesDiscovered: status=" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered");
                if(isAuthenticated) {
                    handleDiscoveredServices(gatt);
                } else {
                    BluetoothGattService fee1Service = gatt.getService(AUTH_SERVICE_UUID);
                    if (fee1Service != null) {
                        BluetoothGattCharacteristic authChar = fee1Service.getCharacteristic(AUTH_CHARACTERISTIC_UUID);
                        if (authChar != null) {
                            startAuthChallenge1stStep();
                        } else {
                            Log.e(TAG, "AUTH characteristic not found");
                            if (connectionCallback != null) connectionCallback.onError("AUTH characteristic not found");
                        }
                    } else {
                        Log.e(TAG, "FEE1 service not found");
                        if (connectionCallback != null) connectionCallback.onError("FEE1 service not found");
                    }
                }
            } else {
                Log.e(TAG, "Service discovery failed: " + status);
                if (connectionCallback != null) connectionCallback.onError("Service discovery failed: " + status);
            }


            if(isAuthenticated) {
                onOperationComplete();
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite: uuid=" + descriptor.getUuid() + ", status=" + status +", value="+ Arrays.toString(descriptor.getValue()));
            if (descriptor.getCharacteristic().getUuid().equals(AUTH_CHARACTERISTIC_UUID)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    sendInitialAuthRequest2ndStep();
                } else {
                    Log.e(TAG, "Failed to enable notifications for AUTH characteristic!");
                    if (connectionCallback != null) connectionCallback.onError("Failed to enable notifications for AUTH characteristic");
                }
            }
            if(isAuthenticated) {
                onOperationComplete();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.e("ON_CHARACTERISTIC_CHANGED","Characteristic changed :: "+BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()));
            characteristicMap.put(characteristic.getUuid(), characteristic);
            if (characteristic.getUuid().equals(AUTH_CHARACTERISTIC_UUID)) {
                handleAuthNotification3rdStep(characteristic.getValue());
            } else {
                if(characteristic.getUuid().equals(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ)) {
                    handleChunkedRead(characteristic.getValue());
                } else if(characteristic.getUuid().equals(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER)) {
                    handleChunkedRead(characteristic.getValue());
                } else {
                    InfoHandler.onInfoReceived(characteristic, characteristic.getValue(), connectionCallback, GTR2eBleService.this);
                }
            }
            if(isAuthenticated) {
                onOperationComplete();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite: uuid=" + characteristic.getUuid() + ", status=" + status);
            if (characteristic.getUuid().equals(AUTH_CHARACTERISTIC_UUID)) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "Auth write failed, status: " + status);
                }
            }
            if(isAuthenticated) {
                onOperationComplete();
            }
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);
            Log.e("On characteristic read","Characteristic changed :: "+BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()));
            InfoHandler.onInfoReceived(characteristic, value, connectionCallback, GTR2eBleService.this);
            if(isAuthenticated) {
                onOperationComplete();
            }
        }
    };

    private void handleDiscoveredServices(BluetoothGatt gatt) {
        for (BluetoothGattService service : gatt.getServices()) {
            Log.d(TAG, "Service found: " + BleNamesResolver.resolveServiceName(service.getUuid().toString()));
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                if(characteristic.getUuid() == HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ) {
                    characteristicChunked2021Read = characteristic;
                    Log.e(TAG, "### characteristicChunked2021Read Set");
                    if (characteristicChunked2021Read != null && huami2021ChunkedDecoder == null) {
                        huami2021ChunkedDecoder = new Huami2021ChunkedDecoder(huami2021Handler, false);
                    } else if (huami2021ChunkedDecoder != null) {
                        huami2021ChunkedDecoder.reset();
                    }
                }
                if(characteristic.getUuid() == HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_WRITE) {
                    characteristicChunked2021Write = characteristic;
                    Log.e(TAG, "### characteristicChunked2021Write Set");
                    if(characteristicChunked2021Write != null && huami2021ChunkedEncoder == null) {
                        huami2021ChunkedEncoder = new Huami2021ChunkedEncoder(mMTU);
                    } else if (huami2021ChunkedDecoder != null) {
                        huami2021ChunkedEncoder.reset();
                    }
                }
                Log.d(TAG, "Characteristic found: " + BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()));
                characteristicMap.put(characteristic.getUuid(), characteristic);
                if (characteristic.getProperties() != 0) {
                    Log.d(TAG, "Trying to read characteristic, authenticated: " + isAuthenticated + ", name: " + BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()));
                    enableNotifications(characteristic);
                    enqueueReadCharacteristic(service.getUuid(),characteristic.getUuid());
                }
            }
        }
    }



    //region AUTH PROCESS
    private void startAuthChallenge1stStep() {
        Log.d(TAG, "[AUTH] Starting auth challenge (Gadgetbridge style)");
        BluetoothGattService fee1Service = gatt.getService(AUTH_SERVICE_UUID);
        if (fee1Service == null) {
            Log.e(TAG, "[AUTH] FEE1 service not found for auth challenge");
            return;
        }

        BluetoothGattCharacteristic authChar = fee1Service.getCharacteristic(AUTH_CHARACTERISTIC_UUID);
        if (authChar == null) {
            Log.e(TAG, "[AUTH] AUTH characteristic not found for auth challenge");
            return;
        }

        // First enable notifications BEFORE sending the auth request
        BluetoothGattDescriptor descriptor = authChar.getDescriptor(AUTH_CHARACTERISTIC_DESCRIPTOR_UUID);
        if (descriptor == null) {
            Log.e(TAG, "Notification descriptor not found");
            return;
        }

        enableNotifications(authChar);
    }

    private void sendInitialAuthRequest2ndStep() {
        BluetoothGattService fee1Service = gatt.getService(AUTH_SERVICE_UUID);
        BluetoothGattCharacteristic authChar = fee1Service.getCharacteristic(AUTH_CHARACTERISTIC_UUID);

        // Use the correct write type
        authChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        // GadgetBridge-style auth request (5 bytes)
        byte[] request = new byte[] { (byte)0x82, 0x00, 0x02, 0x01, 0x00 };
        authChar.setValue(request);

        if (!gatt.writeCharacteristic(authChar)) {
            Log.e(TAG, "Failed to write auth request");
        }
    }

    private void handleAuthNotification3rdStep(byte[] value) {
        if (value == null || value.length < 3) {
            Log.e(TAG, "Invalid auth notification");
            return;
        }

        Log.d(TAG, "Auth notification: " + bytesToHex(value));

        // Response format: 10 CMD STATUS DATA...
        if (value[0] != 0x10) {
            Log.e(TAG, "Invalid auth response header");
            return;
        }

        switch (value[1]) {
            case (byte)0x82: // Challenge response
                if (value[2] == 0x01 && value.length >= 19) {
                    byte[] random = Arrays.copyOfRange(value, 3, 19);
                    Log.d(TAG, "Received valid challenge: " + bytesToHex(random));

                    // Encrypt and send response
                    byte[] encrypted = encryptWithKey(random, DEFAULT_AUTH_KEY);
                    sendEncryptedResponse(encrypted);
                } else {
                    Log.e(TAG, "Invalid challenge response");
                }
                break;

            case (byte)0x83: // Auth result
                if (value[2] == 0x01) {
                    Log.d(TAG, "Authentication successful!");
                    isAuthenticated = true;
                    if (connectionCallback != null) {
                        connectionCallback.onAuthenticated();
                    }
                    startOperationsAfterAuth();
                } else {
                    Log.e(TAG, "Authentication failed");
                }
                break;

            default:
                Log.e(TAG, "Unknown auth response: " + bytesToHex(value));
        }
    }

    private void sendEncryptedResponse(byte[] encrypted) {
        BluetoothGattService fee1Service = gatt.getService(AUTH_SERVICE_UUID);
        if (fee1Service == null) return;

        BluetoothGattCharacteristic authChar = fee1Service.getCharacteristic(AUTH_CHARACTERISTIC_UUID);
        if (authChar == null) return;

        // Format: 0x83 0x00 + 16 encrypted bytes
        byte[] response = new byte[18];
        response[0] = (byte)0x83;
        response[1] = 0x00;
        System.arraycopy(encrypted, 0, response, 2, 16);

        authChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        authChar.setValue(response);
        if (!gatt.writeCharacteristic(authChar)) {
            Log.e(TAG, "Failed to send encrypted response");
        }
    }

    @SuppressWarnings("all")
    private byte[] encryptWithKey(byte[] data, byte[] key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return new byte[16]; // Return zeros if encryption fails
        }
    }

    // endregion

    private void startOperationsAfterAuth() {
        if(gatt == null) return;
        enqueueBleOperation(()->gatt.discoverServices(), "Discover Services");
        enableNotifications(gatt.getService(HUAMI_SERVICE_UUID).getCharacteristic(HuamiService.UUID_CHARACTERISTIC_6_BATTERY_INFO));
    }

    //region QUEUE OPERATIONS HELPER METHODS
    private void enqueueReadCharacteristic(UUID serviceUuid, UUID characteristicUuid) {
        if(gatt==null){
            Log.e(TAG,"Gatt is null");
            return;
        }
        if(gatt.getService(serviceUuid)==null){
            Log.e(TAG,"Service is null");
            return;
        }
        BluetoothGattService service = gatt.getService(serviceUuid);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
        if(characteristic==null){
            Log.e(TAG,"Characteristic is null");
            return;
        }
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
            enqueueBleOperation(() -> gatt.readCharacteristic(characteristic), "Read Characteristic");
        } else {
            Log.e(TAG, "Characteristic is not readable");
        }
    }

    private void enqueueWriteCharacteristic(UUID characteristicUuid, byte[] value, @Nullable String desc) {
        if(gatt==null){
            Log.e(TAG,"Gatt is null, Writing \""+desc+"\" Aborted");
            return;
        }
        BluetoothGattCharacteristic characteristic = characteristicMap.get(characteristicUuid);
        if(characteristic==null){
            Log.e(TAG,"Characteristic is null, Writing \""+desc+"\" Aborted");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            enqueueBleOperation(() -> gatt.writeCharacteristic(characteristic,value,characteristic.getWriteType()), desc!=null?desc:"Write Characteristic");
        } else {
            characteristic.setValue(value);
            enqueueBleOperation(() -> gatt.writeCharacteristic(characteristic), "Write Characteristic");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private void enqueueBleOperation(Runnable operation, @Nullable String desc) {
        synchronized (bleOperations) {
            bleOperations.add(operation);
            Log.e(TAG, MessageFormat.format("Added operation to queue[{0}], size: {1}",desc,bleOperations.size()));
            if (!isBleBusy) {
                processNextOperation();
            }
        }
    }

    private void processNextOperation() {
        synchronized (bleOperations) {
            if (!bleOperations.isEmpty()) {
                Log.d(TAG, "Processing next operation, remaining: " + bleOperations.size());
                isBleBusy = true;
                Runnable op = bleOperations.poll();
                if (op != null) {
                    op.run();
                } else {
                    Log.e(TAG,"Enqueued operation is null, skipping");
                    processNextOperation();
                }
            } else {
                isBleBusy = false;
                Log.d(TAG, "No more operations to process");
            }
        }
    }

    private void onOperationComplete() {
        processNextOperation(); // Trigger next in queue
    }

    private void enableNotifications(BluetoothGattCharacteristic characteristic) {
        int properties = characteristic.getProperties();
        gatt.setCharacteristicNotification(characteristic, true);
        for (BluetoothGattDescriptor descriptor :characteristic.getDescriptors()) {
            if ((properties & PROPERTY_NOTIFY) != 0) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                enqueueBleOperation(()->gatt.writeDescriptor(descriptor), "Write descriptor");
            } else if ((properties & PROPERTY_INDICATE) != 0) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                enqueueBleOperation(()->gatt.writeDescriptor(descriptor), "Write descriptor");
            } else {
                Log.e(TAG, "Characteristic does not support notifications or indications");
            }
        }
    }
    //endregion

    public void enableDoNotDisturb() {
        //public static final byte[] COMMAND_DO_NOT_DISTURB_AUTOMATIC = new byte[] { ENDPOINT_DND, (byte) 0x83 };
        byte[] cmd = COMMAND_DO_NOT_DISTURB_AUTOMATIC.clone();
        cmd[1] &= ~0x80;
        enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION, cmd, "Enable Do Not Disturb");
    }

    public void changeDateFormat(String dateFormat) {
        byte[] cmd = HuamiService.DATEFORMAT_DATE_MM_DD_YYYY;
        System.arraycopy(dateFormat.getBytes(), 0, cmd, 3, 10);
        enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION, cmd, "Change Date Format");
    }


    public static final byte COMMAND_SET_HR_SLEEP = 0x0;
    public static final byte COMMAND_SET__HR_CONTINUOUS = 0x1;
    public static final byte COMMAND_SET_HR_MANUAL = 0x2;
    private static final byte[] startHeartMeasurementManual = new byte[]{0x15, COMMAND_SET_HR_MANUAL, 1};
    private static final byte[] stopHeartMeasurementManual = new byte[]{0x15, COMMAND_SET_HR_MANUAL, 0};
    private static final byte[] startHeartMeasurementContinuous = new byte[]{0x15, COMMAND_SET__HR_CONTINUOUS, 1};
    private static final byte[] stopHeartMeasurementContinuous = new byte[]{0x15, COMMAND_SET__HR_CONTINUOUS, 0};


    //Manual stop required
    public void heartRateMonitoring(boolean enable) {
        if (enable) {
            enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT, stopHeartMeasurementContinuous, "Disabling Continuous Heart Rate Measurement");
            enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT, startHeartMeasurementManual,"Enabling Manual Heart Rate Measurement");
        } else {
            enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT, stopHeartMeasurementManual, "Disabling Manual Heart Rate Measurement");
        }
    }

    //Stops automatically if not wearing watch
    public void continuousHeartRateMonitoring(boolean enable) {
        if (enable) {
            enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT, stopHeartMeasurementManual, "Disabling Manual Heart Rate Measurement");
            enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT, startHeartMeasurementContinuous,"Enabling Continuous Heart Rate Measurement");
        } else {
            enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT, stopHeartMeasurementContinuous, "Disabling Continuous Heart Rate Measurement");
        }
    }

    public void liftWristToWake(boolean enable) {
        byte[] cmd = enable ? HuamiService.COMMAND_ENABLE_DISPLAY_ON_LIFT_WRIST : HuamiService.COMMAND_DISABLE_DISPLAY_ON_LIFT_WRIST;
        //for scheduled enabling, write last 4 bytes as start (byte)HH, (byte)MM, end (byte)HH, (byte)MM
        enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION, cmd, "Lift Wrist To Wake");
    }

    public void sendFindDeviceCommand(boolean start) {
        final UUID UUID_CHARACTERISTIC_ALERT_LEVEL = UUID.fromString((String.format(HuamiService.BASE_UUID, "2A06")));
        byte[] cmd = start ? new byte[] {3} : new byte[] {0};
        enqueueWriteCharacteristic(UUID_CHARACTERISTIC_ALERT_LEVEL, cmd, "Send Find Device Command");
    }

    public void enable24HrFormatTime(boolean enable) {
        byte[] cmd = enable ? HuamiService.DATEFORMAT_TIME_24_HOURS : HuamiService.DATEFORMAT_TIME_12_HOURS;
        enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION, cmd, "change Time Format");
    }

    public void onSetPhoneVolume(final float volume) {
        final byte[] volumeCommand = new byte[]{MUSIC_FLAG_VOLUME, (byte) Math.round(volume)};
        writeToChunkedOld(3, volumeCommand);
    }

    //Doesn't Work
    public void sendReboot() {
        enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_FIRMWARE_CONTROL, new byte[] { HuamiService.COMMAND_FIRMWARE_REBOOT}, "Sending Reboot Command");
    }

    private void handleChunkedRead(final byte[] value) {
        switch (value[0]) {
            case 0x03:
                if (huami2021ChunkedDecoder != null) {
                    final boolean needsAck = huami2021ChunkedDecoder.decode(value);
                    if (needsAck) {
                        sendChunkedAck();
                    }
                } else {
                    Log.w(TAG, "Got chunked payload, but decoder is null");
                }
                return;
            case 0x04:
                final byte handle = value[2];
                final byte count = value[4];
                Log.i(TAG, MessageFormat.format("Got chunked ack, handle={0}, count={1}", handle, count));
                // TODO: We should probably update the handle and count on the encoder
                return;
            default:
                Log.w(TAG, MessageFormat.format("Unhandled chunked payload of type {0}", value[0]));
        }
    }

    public void sendChunkedAck() {
        final byte handle = huami2021ChunkedDecoder.getLastHandle();
        final byte count = huami2021ChunkedDecoder.getLastCount();
        sendChunkedReadAcknowledgement(new byte[] {0x04, 0x00, handle, 0x01, count});
    }

    private void sendChunkedReadAcknowledgement(byte[] value) {
        if (characteristicMap.get(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ) == null) {
            Log.e(TAG, "Chunked read characteristic is null, can't write :: Send chunked ack");
            return;
        }
        try {
            enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ, value, "Send chunked ack");
        } catch (final Exception e) {
            Log.e(TAG, MessageFormat.format("Failed to send chunked ack, {0}", e));
        }
    }

    //Used to send large data
    protected void writeToChunkedOld(int type, byte[] data) {
        final int MAX_CHUNKLENGTH = mMTU - 6;
        int remaining = data.length;
        byte count = 0;
        while (remaining > 0) {
            int copybytes = Math.min(remaining, MAX_CHUNKLENGTH);
            byte[] chunk = new byte[copybytes + 3];

            byte flags = 0;
            if (remaining <= MAX_CHUNKLENGTH) {
                flags |= 0x80; // last chunk
                if (count == 0) {
                    flags |= 0x40; // weird but true
                }
            } else if (count > 0) {
                flags |= 0x40; // consecutive chunk
            }

            chunk[0] = 0;
            chunk[1] = (byte) (flags | type);
            chunk[2] = (byte) (count & 0xff);

            System.arraycopy(data, count++ * MAX_CHUNKLENGTH, chunk, 3, copybytes);
            enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER, chunk, "Writing chunked payload");
            remaining -= copybytes;
        }
    }

    public void onMusicAppOpenOnWatch(boolean opened){

    }
}