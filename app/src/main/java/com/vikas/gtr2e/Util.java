package com.vikas.gtr2e;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.BluetoothLeDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.companion.DeviceFilter;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.text.MessageFormat;

public class Util {
    public static final String TAG = "GTR_UTIL";
    public void startConnection(BluetoothDevice device, String authKey) {
        if(!validateAuthKey(authKey)) {
            Log.e(TAG,"Auth key is invalid");
        }

    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void bondAsCompanion(BluetoothDevice knownDevice, Context context) {
        final String macAddress = knownDevice.getAddress();
        final int type = knownDevice.getType();
        final DeviceFilter<?> deviceFilter;

        if (type == BluetoothDevice.DEVICE_TYPE_LE || type == BluetoothDevice.DEVICE_TYPE_DUAL) {
            Log.d(TAG,MessageFormat.format("companionDeviceManagerBond {0} type {1} - treat as LE", macAddress, type));
            ScanFilter scan = new ScanFilter.Builder().setDeviceAddress(macAddress).build();
            deviceFilter = new BluetoothLeDeviceFilter.Builder().setScanFilter(scan).build();
        } else {
            Log.d(TAG, MessageFormat.format("companionDeviceManagerBond {0} type {1} - treat as classic BT", macAddress, type));
            deviceFilter = new BluetoothDeviceFilter.Builder().setAddress(macAddress).build();
        }

        AssociationRequest pairingRequest = new AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .setSingleDevice(true)
                .build();

        CompanionDeviceManager manager = context.getSystemService(CompanionDeviceManager.class);

//        Log.d(TAG,String.format("Searching for %s associations", macAddress));
//        for (String association : manager.getAssociations()) {
//            Log.d(TAG,String.format("Already associated with: %s", association));
//            if (association.equals(macAddress)) {
//                StartObserving(bondingInterface.getContext(), macAddress);
//                Log.info("The device has already been bonded through CompanionDeviceManager, using regular");
//                // If it's already "associated", we should immediately pair
//                // because the callback is never called (AFAIK?)
//                BondingUtil.bluetoothBond(bondingInterface, device);
//                return;
//            }
//        }
//
//        Log.d(TAG,"Starting association request");
//        manager.associate(pairingRequest, getCompanionDeviceManagerCallback(bondingInterface), null);
    }

    public boolean validateAuthKey(final String authKey) {
        return !(authKey.getBytes().length < 34 || !authKey.startsWith("0x"));
    }
}
