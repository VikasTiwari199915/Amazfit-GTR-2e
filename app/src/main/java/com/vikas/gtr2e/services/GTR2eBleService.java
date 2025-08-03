package com.vikas.gtr2e.services;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;

import static com.vikas.gtr2e.ble.HuamiService.COMMAND_DO_NOT_DISTURB_AUTOMATIC;
import static com.vikas.gtr2e.ble.HuamiService.MUSIC_FLAG_VOLUME;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.vikas.gtr2e.beans.DeviceInfo;
import com.vikas.gtr2e.HuamiBatteryInfo; // Assuming HuamiBatteryInfo is in this package or imported
import com.vikas.gtr2e.ble.BleNamesResolver;
import com.vikas.gtr2e.ble.Huami2021ChunkedDecoder;
import com.vikas.gtr2e.ble.Huami2021ChunkedEncoder;
import com.vikas.gtr2e.ble.Huami2021Handler;
import com.vikas.gtr2e.ble.HuamiService;
import com.vikas.gtr2e.ble.InfoHandler;
import com.vikas.gtr2e.utils.IncomingCallReceiver;
import com.vikas.gtr2e.utils.NotificationUtility;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

@SuppressLint("MissingPermission")
public class GTR2eBleService extends Service {

    private static final String TAG = "GTR2eBleService";
    HashMap<UUID, BluetoothGattCharacteristic> characteristicMap = new HashMap<>();

    // UUIDs for Huami protocol
    public static final UUID AUTH_SERVICE_UUID = UUID.fromString("0000fee1-0000-1000-8000-00805f9b34fb");
    private static final UUID AUTH_CHARACTERISTIC_UUID = UUID.fromString("00000009-0000-3512-2118-0009af100700");
    private static final UUID AUTH_CHARACTERISTIC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID HUAMI_SERVICE_UUID = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
    private static final UUID CHUNKED_READ_UUID = UUID.fromString("00000017-0000-3512-2118-0009af100700");

    private static final byte[] DEFAULT_AUTH_KEY = new byte[]{
            (byte) 0x6c, (byte) 0xd3, (byte) 0x1f, (byte) 0x60,
            (byte) 0x6d, (byte) 0xf4, (byte) 0xb7, (byte) 0x8d,
            (byte) 0x3a, (byte) 0xfd, (byte) 0xcc, (byte) 0x0f,
            (byte) 0x7b, (byte) 0x61, (byte) 0x74, (byte) 0x84
    };

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt gatt;
    private ConnectionCallback connectionCallback;

    private final DeviceInfo deviceInfo = new DeviceInfo();

    private Huami2021ChunkedEncoder huami2021ChunkedEncoder;
    private Huami2021ChunkedDecoder huami2021ChunkedDecoder;

    private BluetoothGattCharacteristic characteristicChunked2021Write;
    private BluetoothGattCharacteristic characteristicChunked2021Read;


    protected static final int MIN_MTU = 23;
    private int mMTU = MIN_MTU;

    private final Queue<Runnable> bleOperations = new LinkedList<>();
    private boolean isBleBusy = false;


    //#### Service related functions
    private final IBinder binder = new LocalBinder();

    // Binder class for clients to access this service
    public class LocalBinder extends Binder {
        public GTR2eBleService getService() {
            return GTR2eBleService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        this.context = getApplicationContext();
        initializeBluetooth();
        NotificationUtility.createNotificationChannel(context);
        NotificationUtility.startAsForegroundService(GTR2eBleService.this, deviceInfo.isConnected());
        deviceInfo.setDeviceName("Amazfit GTR 2e");
        registerCallReceiver();
    }

    private void initializeBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        huami2021ChunkedDecoder = new Huami2021ChunkedDecoder(huami2021Handler, false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        disconnect();
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public interface ConnectionCallback {
        void onDeviceConnected(BluetoothDevice device);

        void onDeviceDisconnected();

        void onAuthenticated();

        void onError(String error);

        void onBatteryDataReceived(HuamiBatteryInfo batteryInfo);

        void onHeartRateChanged(int heartRate);

        void onHeartRateMonitoringChanged(boolean enabled);

        void findPhoneStateChanged(boolean started);

        void pendingBleProcessChanged(int count);

        void onDeviceInfoChanged(DeviceInfo deviceInfo);
    }

    Huami2021Handler huami2021Handler = new Huami2021Handler() {
        @Override
        public void handle2021Payload(short type, byte[] payload) {
            handleChunkedRead(payload);
        }
    };

    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }

    @SuppressLint("MissingPermission")
    public void connect(BluetoothDevice device) {
        deviceInfo.setAuthenticated(false);
        BluetoothDevice device1 = bluetoothAdapter.getRemoteDevice("D9:B6:53:69:F2:F2");
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
        deviceInfo.setAuthenticated(false);
        deviceInfo.setConnected(false);
        deviceInfo.updateBatteryInfo(null);

        updateConnectionState();
        if (connectionCallback != null) {
            connectionCallback.onDeviceDisconnected();
        }
        bleOperations.clear();
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange: status=" + status + ", newState=" + newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to GATT server, requesting MTU 247");
                    deviceInfo.setConnected(true);
                    if (gatt.getDevice() != null && gatt.getDevice().getName() != null) {
                         deviceInfo.setDeviceName(gatt.getDevice().getName());
                    }
                    gatt.requestMtu(247);
                    if (connectionCallback != null) {
                        connectionCallback.onDeviceConnected(gatt.getDevice());
                    }
                    updateConnectionState();
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from GATT server");
                    deviceInfo.setConnected(false);
                    deviceInfo.setAuthenticated(false);
                    deviceInfo.updateBatteryInfo(null);
                    if (connectionCallback != null) {
                        connectionCallback.onDeviceDisconnected();
                    }
                    bleOperations.clear();
                    updateConnectionState();
                }
            } else {
                Log.e(TAG, "Connection state change failed: " + status);
                deviceInfo.setConnected(false);
                deviceInfo.setAuthenticated(false);
                deviceInfo.updateBatteryInfo(null);
                if (connectionCallback != null) {
                    connectionCallback.onError("Connection failed: " + status);
                }
                disconnect();
                updateConnectionState();
                bleOperations.clear();
            }
        }


        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d(TAG, "onMtuChanged: mtu=" + mtu + ", status=" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "MTU set (actual value: " + mtu + "), proceeding to service discovery");
                mMTU = mtu;
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
                if (deviceInfo.isAuthenticated()) {
                    handleDiscoveredServices(gatt);
                } else {
                    BluetoothGattService fee1Service = gatt.getService(AUTH_SERVICE_UUID);
                    if (fee1Service != null) {
                        BluetoothGattCharacteristic authChar = fee1Service.getCharacteristic(AUTH_CHARACTERISTIC_UUID);
                        if (authChar != null) {
                            startAuthChallenge1stStep();
                        } else {
                            Log.e(TAG, "AUTH characteristic not found");
                            if (connectionCallback != null)
                                connectionCallback.onError("AUTH characteristic not found");
                        }
                    } else {
                        Log.e(TAG, "FEE1 service not found");
                        if (connectionCallback != null) connectionCallback.onError("FEE1 service not found");
                    }
                }
            } else {
                Log.e(TAG, "Service discovery failed: " + status);
                if (connectionCallback != null)
                    connectionCallback.onError("Service discovery failed: " + status);
            }

            if (deviceInfo.isAuthenticated()) {
                onOperationComplete();
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite: uuid=" + descriptor.getUuid() + ", status=" + status + ", value=" + Arrays.toString(descriptor.getValue()));
            if (descriptor.getCharacteristic().getUuid().equals(AUTH_CHARACTERISTIC_UUID)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    sendInitialAuthRequest2ndStep();
                } else {
                    Log.e(TAG, "Failed to enable notifications for AUTH characteristic!");
                    if (connectionCallback != null)
                        connectionCallback.onError("Failed to enable notifications for AUTH characteristic");
                }
            }
            if (deviceInfo.isAuthenticated()) {
                onOperationComplete();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.e("ON_CHARACTERISTIC_CHANGED", "Characteristic changed :: " + BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()));
            characteristicMap.put(characteristic.getUuid(), characteristic);
            if (characteristic.getUuid().equals(AUTH_CHARACTERISTIC_UUID)) {
                handleAuthNotification3rdStep(characteristic.getValue());
            } else {
                if (characteristic.getUuid().equals(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ)) {
                    Log.e(TAG, "### 2021 read characteristic changed [NOT IMPLEMENTED]");
                    handleChunkedRead(characteristic.getValue());
                } else if (characteristic.getUuid().equals(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER)) {
                    handleChunkedRead(characteristic.getValue());
                } else {
                    // Pass deviceInfo to InfoHandler or let InfoHandler get it from the service
                    InfoHandler.onInfoReceived(characteristic, characteristic.getValue(), connectionCallback, GTR2eBleService.this, deviceInfo);
                }
            }
            if (deviceInfo.isAuthenticated()) {
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
            if (deviceInfo.isAuthenticated()) {
                onOperationComplete();
            }
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);
            Log.e("On characteristic read", "Characteristic changed :: " + BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()));
            // Pass deviceInfo to InfoHandler or let InfoHandler get it from the service
            InfoHandler.onInfoReceived(characteristic, value, connectionCallback, GTR2eBleService.this, deviceInfo);
            if (deviceInfo.isAuthenticated()) {
                onOperationComplete();
            }
        }
    };

    private void handleDiscoveredServices(BluetoothGatt gatt) {
        for (BluetoothGattService service : gatt.getServices()) {
            Log.d(TAG, "Service found: " + BleNamesResolver.resolveServiceName(service.getUuid().toString()));
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                if (characteristic.getUuid().equals(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ)) {
                    characteristicChunked2021Read = characteristic;
                    Log.e(TAG, "### characteristicChunked2021Read Set");
                    if (characteristicChunked2021Read != null && huami2021ChunkedDecoder == null) {
                        huami2021ChunkedDecoder = new Huami2021ChunkedDecoder(huami2021Handler, false);
                    } else if (huami2021ChunkedDecoder != null) {
                        huami2021ChunkedDecoder.reset();
                    }
                }
                if (characteristic.getUuid().equals(HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_WRITE)) {
                    characteristicChunked2021Write = characteristic;
                    Log.e(TAG, "### characteristicChunked2021Write Set");
                    if (characteristicChunked2021Write != null && huami2021ChunkedEncoder == null) {
                        huami2021ChunkedEncoder = new Huami2021ChunkedEncoder(mMTU);
                    } else if (huami2021ChunkedEncoder != null) { // Corrected from huami2021ChunkedDecoder
                        huami2021ChunkedEncoder.reset();
                    }
                }
                Log.d(TAG, "Characteristic found: " + BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()));
                characteristicMap.put(characteristic.getUuid(), characteristic);
                if (characteristic.getProperties() != 0) {
                    Log.d(TAG, "Trying to read characteristic, authenticated: " + deviceInfo.isAuthenticated() + ", name: " + BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()));
                    enableNotifications(characteristic);
                    enqueueReadCharacteristic(service.getUuid(), characteristic.getUuid());
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

        authChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        byte[] request = new byte[]{(byte) 0x82, 0x00, 0x02, 0x01, 0x00};
        authChar.setValue(request);

        if (!gatt.writeCharacteristic(authChar)) {
            Log.e(TAG, "Failed to write auth request");
        }
    }

    private void handleAuthNotification3rdStep(byte[] value) {
        if (value == null || value.length < 3) {
            Log.e(TAG, "Invalid auth notification");
            deviceInfo.setAuthenticated(false);
            return;
        }

        Log.d(TAG, "Auth notification: " + bytesToHex(value));

        if (value[0] != 0x10) {
            Log.e(TAG, "Invalid auth response header");
            deviceInfo.setAuthenticated(false);
            return;
        }

        switch (value[1]) {
            case (byte) 0x82: // Challenge response
                if (value[2] == 0x01 && value.length >= 19) {
                    byte[] random = Arrays.copyOfRange(value, 3, 19);
                    Log.d(TAG, "Received valid challenge: " + bytesToHex(random));
                    byte[] encrypted = encryptWithKey(random, DEFAULT_AUTH_KEY);
                    sendEncryptedResponse(encrypted);
                } else {
                    Log.e(TAG, "Invalid challenge response");
                    deviceInfo.setAuthenticated(false);
                }
                break;

            case (byte) 0x83: // Auth result
                if (value[2] == 0x01) {
                    Log.d(TAG, "Authentication successful!");
                    deviceInfo.setAuthenticated(true);
                    if (connectionCallback != null) {
                        connectionCallback.onAuthenticated();
                    }
                    startOperationsAfterAuth();
                } else {
                    Log.e(TAG, "Authentication failed");
                    deviceInfo.setAuthenticated(false);
                }
                break;

            default:
                Log.e(TAG, "Unknown auth response: " + bytesToHex(value));
                deviceInfo.setAuthenticated(false);
        }
    }

    private void sendEncryptedResponse(byte[] encrypted) {
        BluetoothGattService fee1Service = gatt.getService(AUTH_SERVICE_UUID);
        if (fee1Service == null) return;

        BluetoothGattCharacteristic authChar = fee1Service.getCharacteristic(AUTH_CHARACTERISTIC_UUID);
        if (authChar == null) return;

        byte[] response = new byte[18];
        response[0] = (byte) 0x83;
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
            return new byte[16]; 
        }
    }

    // endregion

    private void startOperationsAfterAuth() {
        if (gatt == null) return;
        enqueueBleOperation(() -> gatt.discoverServices(), "Discover Services");
        BluetoothGattService huamiService = gatt.getService(HUAMI_SERVICE_UUID);
        if (huamiService != null) {
            BluetoothGattCharacteristic batteryChar = huamiService.getCharacteristic(HuamiService.UUID_CHARACTERISTIC_6_BATTERY_INFO);
            if (batteryChar != null) {
                enableNotifications(batteryChar);
            } else {
                Log.e(TAG, "Battery characteristic not found after auth.");
            }
        } else {
             Log.e(TAG, "Huami service not found after auth.");
        }
    }

    //region QUEUE OPERATIONS HELPER METHODS
    private void enqueueReadCharacteristic(UUID serviceUuid, UUID characteristicUuid) {
        if (gatt == null) {
            Log.e(TAG, "Gatt is null");
            return;
        }
        BluetoothGattService service = gatt.getService(serviceUuid);
        if (service == null) {
            Log.e(TAG, "Service is null for UUID: " + serviceUuid);
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
        if (characteristic == null) {
            Log.e(TAG, "Characteristic is null for UUID: " + characteristicUuid);
            return;
        }
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
            enqueueBleOperation(() -> gatt.readCharacteristic(characteristic), "Read Characteristic: " + characteristic.getUuid().toString());
        } else {
            Log.e(TAG, "Characteristic is not readable: " + characteristic.getUuid().toString());
        }
    }

    private void enqueueWriteCharacteristic(UUID characteristicUuid, byte[] value, @Nullable String desc) {
        if (gatt == null) {
            Log.e(TAG, "Gatt is null, Writing \"" + desc + "\" Aborted");
            return;
        }
        BluetoothGattCharacteristic characteristic = characteristicMap.get(characteristicUuid);
        if (characteristic == null) {
             // Fallback to find characteristic if not in map (e.g. for auth char before full discovery)
            if (characteristicUuid.equals(AUTH_CHARACTERISTIC_UUID) && gatt.getService(AUTH_SERVICE_UUID) != null) {
                characteristic = gatt.getService(AUTH_SERVICE_UUID).getCharacteristic(AUTH_CHARACTERISTIC_UUID);
            }
            if (characteristic == null) {
                 Log.e(TAG, "Characteristic is null for UUID " + characteristicUuid + ", Writing \"" + desc + "\" Aborted");
                 onOperationComplete(); // Prevent queue stall
                 return;
            }
        }
        
        final BluetoothGattCharacteristic finalCharacteristic = characteristic;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            enqueueBleOperation(() -> {
                int result = gatt.writeCharacteristic(finalCharacteristic, value, finalCharacteristic.getWriteType());
                 Log.d(TAG, MessageFormat.format("Wrote {0} to characteristic {1}, result: {2}", Arrays.toString(value), finalCharacteristic.getUuid(), result));
            }, Optional.ofNullable(desc).orElseGet(() -> "Write Characteristic: " + characteristicUuid.toString()));
        } else {
            finalCharacteristic.setValue(value);
            enqueueBleOperation(() -> {
                boolean result = gatt.writeCharacteristic(finalCharacteristic);
                 Log.d(TAG, MessageFormat.format("LEGACY :: Wrote {0} to characteristic {1}, result: {2}", Arrays.toString(value), finalCharacteristic.getUuid(), result));
            }, desc != null ? desc : "Write Characteristic: " + characteristicUuid.toString());
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
            Log.d(TAG, MessageFormat.format("Added operation to queue[{0}], size: {1}", desc, bleOperations.size()));
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
                    Log.e(TAG, "Enqueued operation is null, skipping");
                    isBleBusy = false; // Ensure not stuck
                    processNextOperation();
                }
            } else {
                isBleBusy = false;
                Log.d(TAG, "No more operations to process");
            }
            if (connectionCallback != null)
                connectionCallback.pendingBleProcessChanged(bleOperations.size());
        }
    }

    // Call this after a BLE operation completes (write, read, descriptor write)
    // This method is now consistently called from the gattCallback methods for authenticated operations
    public void onOperationComplete() { // Made public to be called by InfoHandler if needed
        isBleBusy = false; // Mark as not busy before processing next
        processNextOperation(); 
    }

    private void enableNotifications(BluetoothGattCharacteristic characteristic) {
        int properties = characteristic.getProperties();
        gatt.setCharacteristicNotification(characteristic, true); // Enable locally first

        // Find the CCCD (Client Characteristic Configuration Descriptor)
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(AUTH_CHARACTERISTIC_DESCRIPTOR_UUID); // Using the common CCCD UUID
        if (descriptor == null) {
            Log.e(TAG, "CCCD not found for characteristic: " + characteristic.getUuid());
            // onOperationComplete(); // If we consider this op failed, to unblock queue
            return;
        }
        
        byte[] valueToSet;
        if ((properties & PROPERTY_NOTIFY) != 0) {
            valueToSet = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
        } else if ((properties & PROPERTY_INDICATE) != 0) {
            valueToSet = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
        } else {
            Log.e(TAG, "Characteristic does not support notifications or indications: " + characteristic.getUuid());
            // onOperationComplete(); // If we consider this op failed
            return;
        }
        
        descriptor.setValue(valueToSet);
        enqueueBleOperation(() -> {
            boolean success = gatt.writeDescriptor(descriptor);
            if (!success) {
                Log.e(TAG, "Failed to write descriptor for " + characteristic.getUuid());
                // Consider calling onOperationComplete() here if this failure should unblock the queue
            }
        }, "Write CCCD for " + characteristic.getUuid());
    }
    //endregion

    public void enableDoNotDisturb() {
        byte[] cmd = COMMAND_DO_NOT_DISTURB_AUTOMATIC.clone();
        cmd[1] &= ~0x80;
        enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION, cmd, "Enable Do Not Disturb");
    }

    public void changeDateFormat(String dateFormat) {
        byte[] cmd = HuamiService.DATEFORMAT_DATE_MM_DD_YYYY; // Ensure this is correctly defined
        // Ensure dateFormat is not too long for the cmd array starting at index 3
        byte[] dateFormatBytes = dateFormat.getBytes();
        int len = Math.min(dateFormatBytes.length, cmd.length -3);
        System.arraycopy(dateFormatBytes, 0, cmd, 3, len);
        enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION, cmd, "Change Date Format");
    }


    public static final byte COMMAND_SET_HR_SLEEP = 0x0;
    public static final byte COMMAND_SET__HR_CONTINUOUS = 0x1;
    public static final byte COMMAND_SET_HR_MANUAL = 0x2;
    private static final byte[] startHeartMeasurementManual = new byte[]{0x15, COMMAND_SET_HR_MANUAL, 1};
    private static final byte[] stopHeartMeasurementManual = new byte[]{0x15, COMMAND_SET_HR_MANUAL, 0};
    private static final byte[] startHeartMeasurementContinuous = new byte[]{0x15, COMMAND_SET__HR_CONTINUOUS, 1};
    private static final byte[] stopHeartMeasurementContinuous = new byte[]{0x15, COMMAND_SET__HR_CONTINUOUS, 0};


    public void heartRateMonitoring(boolean enable) {
        if (enable) {
            enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT, stopHeartMeasurementContinuous, "Disabling Continuous Heart Rate Measurement");
            enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT, startHeartMeasurementManual, "Enabling Manual Heart Rate Measurement");
        } else {
            enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT, stopHeartMeasurementManual, "Disabling Manual Heart Rate Measurement");
        }
    }

    public void continuousHeartRateMonitoring(boolean enable) {
        if (enable) {
            enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT, stopHeartMeasurementManual, "Disabling Manual Heart Rate Measurement");
            enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT, startHeartMeasurementContinuous, "Enabling Continuous Heart Rate Measurement");
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
        byte[] cmd = start ? new byte[]{3} : new byte[]{0}; // Amazfit uses 3 to start, 0 to stop
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
        enqueueWriteCharacteristic(HuamiService.UUID_CHARACTERISTIC_FIRMWARE_CONTROL, new byte[]{HuamiService.COMMAND_FIRMWARE_REBOOT}, "Sending Reboot Command");
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
        sendChunkedReadAcknowledgement(new byte[]{0x04, 0x00, handle, 0x01, count});
    }

    private void sendChunkedReadAcknowledgement(byte[] value) {
        UUID chunkedReadCharUUID = HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ; // Assuming this is correct
        if (characteristicMap.get(chunkedReadCharUUID) == null && characteristicChunked2021Read == null) {
             Log.e(TAG, "Chunked read characteristic (2021) is null, can't write :: Send chunked ack");
             onOperationComplete(); // Unblock queue
             return;
        }
        // Prefer the explicitly stored characteristic if available
        UUID targetCharUUID = (characteristicChunked2021Read != null) ? characteristicChunked2021Read.getUuid() : chunkedReadCharUUID;
        
        try {
            // Note: Writing to a "read" characteristic is unusual, ensure this is the correct behavior for ack
            enqueueWriteCharacteristic(targetCharUUID, value, "Send chunked ack");
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


    public void onMusicAppOpenOnWatch(boolean opened) {
        // Implementation based on how your watch communicates this
    }

    public enum CALL_STATUS {INCOMING, PICKED, ENDED}

    public void setCallStatus(CALL_STATUS callStatus, String caller) {
        if (callStatus == CALL_STATUS.INCOMING) {
            if (caller == null || caller.trim().isEmpty()) {
                caller = "Unknown";
            }
            byte[] message = caller.getBytes(StandardCharsets.UTF_8);
            int length = 10 + message.length;
            ByteBuffer buf = ByteBuffer.allocate(length);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.put(new byte[]{3, 0, 0, 0, 0, 0}); // Prefix - for call status info
            buf.put(message);
            buf.put(new byte[]{0, 0, 0, 2}); // Suffix - end of message
            writeToChunkedOld(0, buf.array());

        } else if ((callStatus == CALL_STATUS.PICKED) || (callStatus == CALL_STATUS.ENDED)) {
            writeToChunkedOld(0, new byte[]{3, 3, 0, 0, 0, 0});
        }
    }

    public void updateConnectionState() {
        NotificationUtility.updateNotification(context, deviceInfo.isConnected(), GTR2eBleService.this);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Optional: Restart service when user removes app from recent tasks
        // Intent restartService = new Intent(this, GTR2eBleService.class);
        // restartService.setPackage(getPackageName());
        // startService(restartService);
        super.onTaskRemoved(rootIntent);
    }

    private void registerCallReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        filter.addAction("com.vikas.gtr2e.MUTE_CALL");
        try {
            ContextCompat.registerReceiver(context, new IncomingCallReceiver(GTR2eBleService.this), filter, ContextCompat.RECEIVER_EXPORTED);
        } catch (Exception e) {
            Log.w(TAG, "Call receiver already registered or error: " + e.getMessage());
        }
    }

    //mute call
    public void muteCall() {
        Intent intent = new Intent("com.vikas.gtr2e.MUTE_CALL");
        sendBroadcast(intent);
    }
}
