package com.vikas.gtr2e.ble;

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
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
@SuppressLint("MissingPermission")
public class GTR2eBleService {
    private static final String TAG = "GTR2eBleService";

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
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt gatt;
    private ConnectionCallback connectionCallback;
    private boolean isConnected = false;
    private boolean pendingStartAuth = false;

    public interface ConnectionCallback {
        void onDeviceConnected(BluetoothDevice device);
        void onDeviceDisconnected();
        void onAuthenticated();
        void onError(String error);
        void onBatteryDataReceived(byte[] batteryData);
    }

    public GTR2eBleService(Context context) {
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }

    @SuppressLint("MissingPermission")
    public void connect(BluetoothDevice device) {
//        Log.d(TAG, "Connecting to device: " + device.getAddress());
        isAuthenticated = false;
        isConnected = false;
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
        isConnected = false;
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
                    isConnected = true;
                    gatt.requestMtu(247);
                    if (connectionCallback != null) {
                        connectionCallback.onDeviceConnected(gatt.getDevice());
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from GATT server");
                    isConnected = false;
                    if (connectionCallback != null) {
                        connectionCallback.onDeviceDisconnected();
                    }
                }
            } else {
                Log.e(TAG, "Connection state change failed: " + status);
                isConnected = false;
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
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered: status=" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService fee1Service = gatt.getService(AUTH_SERVICE_UUID);
                if (fee1Service != null) {
                    BluetoothGattCharacteristic authChar = fee1Service.getCharacteristic(AUTH_CHARACTERISTIC_UUID);
                    if (authChar != null) {
                        logAllDescriptors(authChar);
                        logCharacteristicProperties(authChar);
                        enableAuthNotification();
                    } else {
                        Log.e(TAG, "AUTH characteristic not found");
                        if (connectionCallback != null) connectionCallback.onError("AUTH characteristic not found");
                    }
                } else {
                    Log.e(TAG, "FEE1 service not found");
                    if (connectionCallback != null) connectionCallback.onError("FEE1 service not found");
                }
            } else {
                Log.e(TAG, "Service discovery failed: " + status);
                if (connectionCallback != null) connectionCallback.onError("Service discovery failed: " + status);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite: uuid=" + descriptor.getUuid() + ", status=" + status +", value="+ Arrays.toString(descriptor.getValue()));
            if (descriptor.getCharacteristic().getUuid().equals(AUTH_CHARACTERISTIC_UUID)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    sendInitialAuthRequest();
                } else {
                    Log.e(TAG, "Failed to enable notifications for AUTH characteristic!");
                    if (connectionCallback != null) connectionCallback.onError("Failed to enable notifications for AUTH characteristic");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.e("ON_CHARACTERISTIC_CHANGED","Characteristic changed :: "+BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()));
            if (characteristic.getUuid().equals(AUTH_CHARACTERISTIC_UUID)) {
                handleAuthNotification(characteristic.getValue());
            } else {
                Log.e(TAG, "Unknown characteristic changed: " + BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()));
                InfoHandler.onInfoReceived(characteristic, characteristic.getValue(), connectionCallback);
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
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);
            Log.e("On characteristic read","Characteristic changed :: "+BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString()));
            InfoHandler.onInfoReceived(characteristic, value, connectionCallback);
        }
    };

    private void enableAuthNotification() {
        BluetoothGattService authServiceUuid = gatt.getService(AUTH_SERVICE_UUID);
        if (authServiceUuid == null) {
            Log.e(TAG, "FEE1 service not found!");
            return;
        }
        BluetoothGattCharacteristic authChar = authServiceUuid.getCharacteristic(AUTH_CHARACTERISTIC_UUID);
        if (authChar == null) {
            Log.e(TAG, "Auth characteristic not found!");
            return;
        }
        startAuthChallenge();
    }

    private void startAuthChallenge() {
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

        // Set up notification listener first
        if (!gatt.setCharacteristicNotification(authChar, true)) {
            Log.e(TAG, "Failed to set notification for auth characteristic");
            return;
        }

        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (!gatt.writeDescriptor(descriptor)) {
            Log.e(TAG, "Failed to write notification descriptor");
            return;
        }


        //Chunked notification
        BluetoothGattService huamiService = gatt.getService(HUAMI_SERVICE_UUID);
        if (huamiService == null) {
            Log.e(TAG, "[AUTH] huami service not found");
            return;
        }

        BluetoothGattCharacteristic chunkedReadChar = huamiService.getCharacteristic(CHUNKED_READ_UUID);
        if (chunkedReadChar == null) {
            Log.e(TAG, "[AUTH] chunkedReadChar characteristic not found for auth challenge");
            return;
        }

        BluetoothGattDescriptor ChunkedReadDescriptor = chunkedReadChar.getDescriptor(AUTH_CHARACTERISTIC_DESCRIPTOR_UUID);
        if (ChunkedReadDescriptor == null) {
            Log.e(TAG, "chunkedReadChar descriptor not found");
            return;
        }


        if (!gatt.setCharacteristicNotification(chunkedReadChar, true)) {
            Log.e(TAG, "Failed to set notification for chunkedReadChar characteristic");
            return;
        }

        pendingStartAuth = true;
    }

    private void sendInitialAuthRequest() {
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

    private void handleAuthNotification(byte[] value) {
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
                    enableDataNotifications();
                } else {
                    Log.e(TAG, "Authentication failed");
                }
                break;

            default:
                Log.e(TAG, "Unknown auth response: " + bytesToHex(value));
        }
    }

    private void enableDataNotifications() {
        gatt.readCharacteristic(gatt.getService(HUAMI_SERVICE_UUID).getCharacteristic(HuamiService.UUID_CHARACTERISTIC_6_BATTERY_INFO));
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

    private byte[] encryptWithKey(byte[] data, byte[] key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return new byte[16]; // Return zeros if encryption fails
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    // Utility: Log all descriptors for a characteristic
    private void logAllDescriptors(BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            Log.e(TAG, "[DEBUG] logAllDescriptors: characteristic is null");
            return;
        }
        for (BluetoothGattDescriptor desc : characteristic.getDescriptors()) {
            Log.d(TAG, "[DEBUG] Descriptor found: " + desc.getUuid());
        }
    }

    // Utility: Log characteristic properties
    private void logCharacteristicProperties(BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            Log.e(TAG, "[DEBUG] logCharacteristicProperties: characteristic is null");
            return;
        }
        Log.d(TAG, "[DEBUG] Characteristic properties: " + characteristic.getProperties());
    }
} 