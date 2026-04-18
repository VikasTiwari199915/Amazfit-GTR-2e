package com.vikas.gtr2e.utils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.Queue;

public class GTR2eBleWriteManager {
    private static final String TAG = "GTR2eBleWriteManager";
    private final BluetoothGatt gatt;
    private final Queue<WriteRequest> queue = new LinkedList<>();
    private boolean isWriting = false;

    public GTR2eBleWriteManager(BluetoothGatt gatt) {
        this.gatt = gatt;
    }

    // 🔥 Public method to enqueue writes
    public synchronized void enqueueWrite(BluetoothGattCharacteristic characteristic, byte[] value, int writeType, String desc) {
        queue.add(new WriteRequest(characteristic, value, writeType, desc));
        processNext();
    }

    public synchronized void enqueueRead(BluetoothGattCharacteristic characteristic, String desc) {
        queue.add(new ReadRequest(characteristic, desc));
        processNext();
    }

    public synchronized void enqueueWriteDescriptor(BluetoothGattDescriptor descriptor, String desc) {
        queue.add(new WriteDescriptorRequest(descriptor, desc));
        processNext();
    }

    public synchronized void enqueueDiscoverServices() {
        queue.add(new DiscoverServicesRequest());
        processNext();
    }

    @SuppressLint("MissingPermission")
    private synchronized void processNext() {
        if (isWriting) return;
        if (queue.isEmpty()) return;

        WriteRequest request = queue.peek();
        boolean started = false;
        if (request == null) {
            Log.e(TAG, "Write Request is null, dropping!");
            queue.poll();
            processNext();
            return;
        }
        Log.d(TAG, "Executing: " + request.desc);
        if (request instanceof ReadRequest) {
            started = gatt.readCharacteristic(request.characteristic);
            Log.d(TAG, "Reading characteristic :: " + request.characteristic.getUuid().toString() + ", Started : " + started);
        } else if (request instanceof WriteDescriptorRequest) {
            started = gatt.writeDescriptor(request.descriptor);
            Log.d(TAG, "Writing descriptor :: " + request.descriptor.getUuid().toString() + ", Started : " + started);
        } else if (request instanceof DiscoverServicesRequest) {
            started = gatt.discoverServices();
            Log.d(TAG, "Discovering services :: " + started);
        } else {
            request.characteristic.setWriteType(request.writeType);
            request.characteristic.setValue(request.value);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                int result = gatt.writeCharacteristic(request.characteristic, request.value, request.writeType);
                started = (result == BluetoothGatt.GATT_SUCCESS);
            } else {
                started = gatt.writeCharacteristic(request.characteristic);
            }
            Log.d(TAG, MessageFormat.format("Wrote {0} to characteristic {1}, result: {2}", ConversionUtil.toHex(request.value), request.characteristic.getUuid(), started));
        }

        if (started) {
            isWriting = true;
        } else {
            // ❌ failed immediately → drop and try next
            Log.e("Write Manager", "Failed to write immediately, dropping current and moving on : " + ConversionUtil.toHex(request.value));
            queue.poll();
            processNext();
        }
    }

    // 🔥 Call this from your BluetoothGattCallback
    public synchronized void onWriteComplete() {
//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            synchronized (this) {
                isWriting = false;
                queue.poll();
                processNext();
            }
//        }, 30); // 🔥 tweak this (50–120ms)
    }

    public synchronized void clear() {
        queue.clear();
        isWriting = false;
    }

    public static class WriteRequest {
        BluetoothGattCharacteristic characteristic;
        BluetoothGattDescriptor descriptor;
        byte[] value;
        int writeType;
        String desc;

        public WriteRequest(BluetoothGattCharacteristic characteristic, byte[] value, int writeType, String desc) {
            this.characteristic = characteristic;
            this.value = value;
            this.writeType = writeType;
            this.desc = desc;
        }

        public WriteRequest(BluetoothGattDescriptor descriptor, byte[] value, int writeType, String desc) {
            this.descriptor = descriptor;
            this.value = value;
            this.writeType = writeType;
            this.desc = desc;
        }
    }

    public static class ReadRequest extends WriteRequest {
        public ReadRequest(BluetoothGattCharacteristic characteristic, String desc) {
            super(characteristic, null, -999, desc);
        }
    }

    public static class WriteDescriptorRequest extends WriteRequest {
        public WriteDescriptorRequest(BluetoothGattDescriptor descriptor, String desc) {
            super(descriptor, null, -999, desc);
        }
    }

    public static class DiscoverServicesRequest extends WriteRequest {
        public DiscoverServicesRequest() {
            super((BluetoothGattCharacteristic) null, null, -999, "Discover Services");
        }
    }
}