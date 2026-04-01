package com.vikas.gtr2e.services

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.companion.CompanionDeviceManager
import android.companion.ObservingDevicePresenceRequest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.vikas.gtr2e.beans.DeviceInfo
import com.vikas.gtr2e.beans.MusicBean
import com.vikas.gtr2e.beans.MusicStateBean
import com.vikas.gtr2e.ble.BleNamesResolver
import com.vikas.gtr2e.ble.Huami2021ChunkedDecoder
import com.vikas.gtr2e.ble.Huami2021ChunkedEncoder
import com.vikas.gtr2e.ble.Huami2021Handler
import com.vikas.gtr2e.ble.HuamiService
import com.vikas.gtr2e.ble.InfoHandler
import com.vikas.gtr2e.enums.CallStatus
import com.vikas.gtr2e.enums.MusicControl
import com.vikas.gtr2e.interfaces.ConnectionCallback
import com.vikas.gtr2e.stateFlow.BleEvent
import com.vikas.gtr2e.stateFlow.BleState
import com.vikas.gtr2e.utils.ConversionUtil
import com.vikas.gtr2e.utils.GTR2eNotificationUtil
import com.vikas.gtr2e.utils.GTR2eWatchFaceUtil
import com.vikas.gtr2e.utils.MediaUtil
import com.vikas.gtr2e.utils.NotificationUtility
import com.vikas.gtr2e.utils.Prefs
import java.text.MessageFormat
import java.util.Calendar
import java.util.LinkedList
import java.util.Optional
import java.util.Queue
import java.util.UUID
import java.util.function.Supplier
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Foreground Service for GTR2e, performs all ble operations
 * registers event listener services, handles auth and device connection
 * @author Vikas Tiwari
 */
@SuppressLint("MissingPermission")
class GTR2eBleService : Service() {

    private val _state = MutableStateFlow(BleState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<BleEvent>()
    val events = _events.asSharedFlow()

    private fun updateState(block: BleState.() -> BleState) {
        _state.value = _state.value.block()
    }


    var deviceInfo = DeviceInfo()
    val deviceInfoLiveData = MutableLiveData<DeviceInfo?>()
    fun getDeviceInfoLiveData(): LiveData<DeviceInfo?> {
        return deviceInfoLiveData
    }

    private val bleOperations: Queue<Runnable?> = LinkedList<Runnable?>()

    //#### Service related functions
    private val binder: IBinder = LocalBinder()
    var characteristicMap: HashMap<UUID?, BluetoothGattCharacteristic?> = HashMap()
    private var context: Context? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var gatt: BluetoothGatt? = null

    var connectionCallback: ConnectionCallback? = null
//    fun setConnectionCallback(connectionCallback: ConnectionCallback?){
//        this.connectionCallback = connectionCallback
//    }
    private var huami2021ChunkedEncoder: Huami2021ChunkedEncoder? = null
    private var huami2021ChunkedDecoder: Huami2021ChunkedDecoder? = null
    private var characteristicChunked2021Write: BluetoothGattCharacteristic? = null
    private var characteristicChunked2021Read: BluetoothGattCharacteristic? = null
    var huami2021Handler: Huami2021Handler =
        Huami2021Handler { type: Short, payload: ByteArray? -> handleChunkedRead(payload!!) }
    private var mMTU: Int = MIN_MTU

    private var isBleBusy = false


    //watch status
    private var isMusicAppOpen = false

    private var currentController: MediaController? = null
    private var mediaCallback: MediaController.Callback? = null

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange: status=$status, newState=$newState")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to GATT server, requesting MTU 247")
                    deviceInfo.connected = true
                    deviceInfoLiveData.postValue(deviceInfo)
                    updateState { copy(isConnected = true) }
                    if (gatt.device != null) {
                        if (gatt.device.getName() != null) {
                            deviceInfo.deviceName = gatt.device.getName()
                        }
                        // Save the successful MAC address
                        Prefs.setLastDeviceMac(context, gatt.device.getAddress())
                    }
                    gatt.requestMtu(247)
                    connectionCallback?.onDeviceConnected(gatt.device)
                    updateConnectionState()
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from GATT server")
                    deviceInfo.connected = false
                    deviceInfo.authenticated = false
                    deviceInfo.updateBatteryInfo(null)
                    deviceInfoLiveData.postValue(deviceInfo)
                    updateState { copy(isConnected = false, isAuthenticated = false) }
                    connectionCallback?.onDeviceDisconnected()
                    synchronized(bleOperations) {
                        bleOperations.clear()
                        isBleBusy = false
                    }
                    updateConnectionState()
                }
            } else {
                Log.e(TAG, "Connection state change failed: $status")
                deviceInfo.connected = false
                deviceInfo.authenticated = false
                deviceInfo.updateBatteryInfo(null)
                deviceInfoLiveData.postValue(deviceInfo)
                updateState { copy(isConnected = false, isAuthenticated = false) }
                connectionCallback?.onError("Connection failed: $status")
                synchronized(bleOperations) {
                    bleOperations.clear()
                    isBleBusy = false
                }
                disconnect()
                updateConnectionState()
            }
        }


        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d(TAG, "onMtuChanged: mtu=$mtu, status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "MTU set (actual value: $mtu), proceeding to service discovery")
                mMTU = mtu
                gatt.discoverServices()
            } else {
                Log.e(TAG, "Failed to set MTU, status=$status")
            }
            if (huami2021ChunkedEncoder != null) {
                huami2021ChunkedEncoder!!.setMTU(mtu)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d(TAG, "onServicesDiscovered: status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
                if (deviceInfo.authenticated) {
                    handleDiscoveredServices(gatt)
                } else {
                    val fee1Service = gatt.getService(AUTH_SERVICE_UUID)
                    if (fee1Service != null) {
                        val authChar = fee1Service.getCharacteristic(AUTH_CHARACTERISTIC_UUID)
                        if (authChar != null) {
                            startAuthChallenge1stStep()
                        } else {
                            Log.e(TAG, "AUTH characteristic not found")
                            connectionCallback?.onError("AUTH characteristic not found")
                        }
                    } else {
                        Log.e(TAG, "FEE1 service not found")
                        connectionCallback?.onError("FEE1 service not found")
                    }
                }
            } else {
                Log.e(TAG, "Service discovery failed: $status")
                connectionCallback?.onError("Service discovery failed: $status")
            }

            if (deviceInfo.authenticated) {
                onOperationComplete()
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Log.d(
                TAG,
                "onDescriptorWrite: uuid=" + descriptor.uuid + ", status=" + status + ", value=" + descriptor.value.contentToString())
            if (descriptor.characteristic.uuid == AUTH_CHARACTERISTIC_UUID) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    sendInitialAuthRequest2ndStep()
                } else {
                    Log.e(TAG, "Failed to enable notifications for AUTH characteristic!")
                    connectionCallback?.onError("Failed to enable notifications for AUTH characteristic")
                }
            }
            if (deviceInfo.authenticated) {
                onOperationComplete()
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.e(
                "ON_CHARACTERISTIC_CHANGED",
                "Characteristic changed :: " + BleNamesResolver.resolveCharacteristicName(
                    characteristic.uuid.toString()
                )
            )
            characteristicMap[characteristic.uuid] = characteristic
            if (characteristic.uuid == AUTH_CHARACTERISTIC_UUID) {
                handleAuthNotification3rdStep(characteristic.value)
            } else {
                if (characteristic.uuid == HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ) {
                    Log.e(TAG, "### 2021 read characteristic changed [NOT IMPLEMENTED]")
                    handleChunkedRead(characteristic.value)
                } else if (characteristic.uuid == HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER) {
                    handleChunkedRead(characteristic.value)
                } else {
                    // Pass deviceInfo to InfoHandler or let InfoHandler get it from the service
                    InfoHandler.onInfoReceived(
                        characteristic,
                        characteristic.value,
                        connectionCallback,
                        this@GTR2eBleService,
                        deviceInfo,
                        deviceInfoLiveData
                    )
                }
            }
            if (deviceInfo.authenticated) {
                onOperationComplete()
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.d(
                TAG,
                "onCharacteristicWrite: uuid=" + characteristic.uuid + ", status=" + status
            )
            if (characteristic.uuid == AUTH_CHARACTERISTIC_UUID) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "Auth write failed, status: $status")
                }
            }
            if (deviceInfo.authenticated) {
                onOperationComplete()
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            Log.e(
                "On characteristic read",
                "Characteristic changed :: " + BleNamesResolver.resolveCharacteristicName(
                    characteristic.uuid.toString()
                )
            )
            // Pass deviceInfo to InfoHandler or let InfoHandler get it from the service
            InfoHandler.onInfoReceived(
                characteristic,
                value,
                connectionCallback,
                this@GTR2eBleService,
                deviceInfo,
                deviceInfoLiveData
            )
            if (deviceInfo.authenticated) {
                onOperationComplete()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        this.context = applicationContext
        initializeBluetooth()
        NotificationUtility.createNotificationChannel(context)
        NotificationUtility.startAsForegroundService(this@GTR2eBleService, deviceInfo.connected)
        deviceInfo.deviceName = "Amazfit GTR 2e"
        deviceInfoLiveData.postValue(deviceInfo)
        updateState { copy(isConnected = false, isAuthenticated = false) }
        if (Prefs.getDeviceAdded(context)) {
            registerDevicePresence()
            registerVolumeListener()
        }
    }


    private fun initializeBluetooth() {
        val bluetoothManager = context!!.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager?
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.adapter
        }
        huami2021ChunkedDecoder = Huami2021ChunkedDecoder(huami2021Handler, false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        disconnect()
    }

    @SuppressLint("MissingPermission")
    fun connect() {
        if (deviceInfo.connected || deviceInfo.forceDisconnected) {
            //No need to reconnect
            Log.e(TAG, "device already connected/Force disconnected, skipping...")
            return
        }
        if (gatt != null) {
            Log.w(TAG, "Existing GATT found, closing...")
            gatt!!.disconnect()
            gatt!!.close()
            gatt = null
            // Small delay to allow stack to clean up
            try {
                Thread.sleep(200)
            } catch (_: InterruptedException) {
            }
        }

        var targetDevice: BluetoothDevice? = null
        val lastMac = Prefs.getLastDeviceMac(context)
        if (lastMac != null && bluetoothAdapter != null) {
            targetDevice = bluetoothAdapter!!.getRemoteDevice(lastMac)
        }

        if (targetDevice == null) {
            Log.e(TAG, "Connect called with null device and no saved MAC address.")
            connectionCallback?.onError("No device to connect to")
            return
        }

        Log.d(TAG, "Connecting to: " + targetDevice.getAddress())
        deviceInfo.authenticated = false
        deviceInfoLiveData.postValue(deviceInfo)
        updateState { copy(isAuthenticated = false) }
        if (targetDevice.getBondState() == BluetoothDevice.BOND_NONE) {
            targetDevice.createBond()
        }
        gatt = targetDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting from device")
        if (gatt != null) {
            try {
                gatt!!.disconnect()
                gatt!!.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error during disconnect/close", e)
            }
            gatt = null
        }
        deviceInfo.authenticated = false
        deviceInfo.connected = false
        deviceInfo.updateBatteryInfo(null)
        deviceInfoLiveData.postValue(deviceInfo)
        updateState { copy(isConnected = false, isAuthenticated = false) }

        updateConnectionState()
        connectionCallback?.onDeviceDisconnected()
        synchronized(bleOperations) {
            bleOperations.clear()
            isBleBusy = false
        }
    }

    private fun handleDiscoveredServices(gatt: BluetoothGatt) {
        for (service in gatt.getServices()) {
            Log.d(
                TAG,
                "Service found: " + BleNamesResolver.resolveServiceName(
                    service.uuid.toString()
                )
            )
            for (characteristic in service.characteristics) {
                if (characteristic.uuid == HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ) {
                    characteristicChunked2021Read = characteristic
                    Log.e(TAG, "### characteristicChunked2021Read Set")
                    if (characteristicChunked2021Read != null && huami2021ChunkedDecoder == null) {
                        huami2021ChunkedDecoder = Huami2021ChunkedDecoder(huami2021Handler, false)
                    } else if (huami2021ChunkedDecoder != null) {
                        huami2021ChunkedDecoder!!.reset()
                    }
                }
                if (characteristic.uuid == HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_WRITE) {
                    characteristicChunked2021Write = characteristic
                    Log.e(TAG, "### characteristicChunked2021Write Set")
                    if (characteristicChunked2021Write != null && huami2021ChunkedEncoder == null) {
                        huami2021ChunkedEncoder = Huami2021ChunkedEncoder(mMTU)
                    } else if (huami2021ChunkedEncoder != null) { // Corrected from huami2021ChunkedDecoder
                        huami2021ChunkedEncoder!!.reset()
                        huami2021ChunkedEncoder!!.setMTU(mMTU)
                    }
                }
                Log.d(
                    TAG,
                    "Characteristic found: " + BleNamesResolver.resolveCharacteristicName(
                        characteristic.uuid.toString()
                    )
                )
                characteristicMap[characteristic.uuid] = characteristic
                if (characteristic.properties != 0) {
                    Log.d(
                        TAG,
                        "Trying to read characteristic, authenticated: " + deviceInfo.authenticated + ", name: " + BleNamesResolver.resolveCharacteristicName(
                            characteristic.uuid.toString()
                        )
                    )
                    enableNotifications(characteristic)
                    enqueueReadCharacteristic(service.uuid, characteristic.uuid)
                }
            }
        }
    }


    //region AUTH PROCESS
    private fun startAuthChallenge1stStep() {
        Log.d(TAG, "[AUTH] Starting auth challenge (Gadgetbridge style)")
        val fee1Service = gatt!!.getService(AUTH_SERVICE_UUID)
        if (fee1Service == null) {
            Log.e(TAG, "[AUTH] FEE1 service not found for auth challenge")
            return
        }

        val authChar = fee1Service.getCharacteristic(AUTH_CHARACTERISTIC_UUID)
        if (authChar == null) {
            Log.e(TAG, "[AUTH] AUTH characteristic not found for auth challenge")
            return
        }

        val descriptor = authChar.getDescriptor(AUTH_CHARACTERISTIC_DESCRIPTOR_UUID)
        if (descriptor == null) {
            Log.e(TAG, "Notification descriptor not found")
            return
        }

        enableNotifications(authChar)
    }

    private fun sendInitialAuthRequest2ndStep() {
        val fee1Service = gatt!!.getService(AUTH_SERVICE_UUID)
        val authChar = fee1Service.getCharacteristic(AUTH_CHARACTERISTIC_UUID)

        authChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

        val request = byteArrayOf(0x82.toByte(), 0x00, 0x02, 0x01, 0x00)
        authChar.value = request
        var writtenAuthRequest = false
        writtenAuthRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            BluetoothStatusCodes.SUCCESS == (gatt!!.writeCharacteristic(authChar, request, authChar.writeType))
        } else {
            gatt!!.writeCharacteristic(authChar)
        }
        if(!writtenAuthRequest) {
            Log.e(TAG, "Failed to write auth request")
        }
    }

    private fun handleAuthNotification3rdStep(value: ByteArray?) {
        if (value == null || value.size < 3) {
            Log.e(TAG, "Invalid auth notification")
            deviceInfo.authenticated = false
            deviceInfoLiveData.postValue(deviceInfo)
            updateState { copy(isAuthenticated = false) }
            return
        }

        Log.d(TAG, "Auth notification: " + bytesToHex(value))

        if (value[0].toInt() != 0x10) {
            Log.e(TAG, "Invalid auth response header")
            deviceInfo.authenticated = false
            deviceInfoLiveData.postValue(deviceInfo)
            updateState { copy(isAuthenticated = false) }
            return
        }

        when (value[1]) {
            0x82.toByte() -> if (value[2].toInt() == 0x01 && value.size >= 19) {
                val random = value.copyOfRange(3, 19)
                Log.d(TAG, "Received valid challenge: " + bytesToHex(random))
                var authKey: ByteArray = DEFAULT_AUTH_KEY
                if (Prefs.getAuthKey(applicationContext) != null) {
                    authKey = getBytesFromHex(Prefs.getAuthKey(applicationContext))
                }
                val encrypted = encryptWithKey(random, authKey)
                sendEncryptedResponse(encrypted)
            } else {
                Log.e(TAG, "Invalid challenge response")
                deviceInfo.authenticated = false
                deviceInfoLiveData.postValue(deviceInfo)
                updateState { copy(isAuthenticated = false) }
            }

            0x83.toByte() -> if (value[2].toInt() == 0x01) {
                Log.d(TAG, "Authentication successful!")
                deviceInfo.authenticated = true
                deviceInfoLiveData.postValue(deviceInfo)
                updateState { copy(isAuthenticated = true) }
                connectionCallback?.onAuthenticated()
                startOperationsAfterAuth()
            } else {
                Log.e(TAG, "Authentication failed")
                deviceInfo.authenticated = false
                deviceInfoLiveData.postValue(deviceInfo)
                updateState { copy(isAuthenticated = false) }
                connectionCallback!!.onError("Failed to authenticate with the watch, please try again or clear app data and reconnect.")
            }

            else -> {
                Log.e(TAG, "Unknown auth response: " + bytesToHex(value))
                deviceInfo.authenticated = false
                deviceInfoLiveData.postValue(deviceInfo)
                updateState { copy(isAuthenticated = false) }
            }
        }
    }

    private fun sendEncryptedResponse(encrypted: ByteArray) {
        val fee1Service = gatt!!.getService(AUTH_SERVICE_UUID) ?: return

        val authChar = fee1Service.getCharacteristic(AUTH_CHARACTERISTIC_UUID) ?: return

        val response = ByteArray(18)
        response[0] = 0x83.toByte()
        response[1] = 0x00
        System.arraycopy(encrypted, 0, response, 2, 16)

        authChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        authChar.setValue(response)
        if (!gatt!!.writeCharacteristic(authChar)) {
            Log.e(TAG, "Failed to send encrypted response")
        }
    }

    private fun encryptWithKey(data: ByteArray?, key: ByteArray?): ByteArray {
        try {
            val secretKey = SecretKeySpec(key, "AES")
            @SuppressLint("GetInstance") val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            return cipher.doFinal(data)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            return ByteArray(16)
        }
    }

    private fun startOperationsAfterAuth() {
        if (gatt == null) return
        enqueueBleOperation({ gatt!!.discoverServices() }, "Discover Services")
        val huamiService = gatt!!.getService(HUAMI_SERVICE_UUID)
        if (huamiService != null) {
            val batteryChar =
                huamiService.getCharacteristic(HuamiService.UUID_CHARACTERISTIC_6_BATTERY_INFO)
            if (batteryChar != null) {
                enableNotifications(batteryChar)
            } else {
                Log.e(TAG, "Battery characteristic not found after auth.")
            }
        } else {
            Log.e(TAG, "Huami service not found after auth.")
        }
        updateMediaController()
    }

    // endregion
    //region QUEUE OPERATIONS HELPER METHODS
    private fun enqueueReadCharacteristic(serviceUuid: UUID?, characteristicUuid: UUID?) {
        if (gatt == null) {
            Log.e(TAG, "GATT is null")
            return
        }
        val service = gatt!!.getService(serviceUuid)
        if (service == null) {
            Log.e(TAG, "Service is null for UUID: $serviceUuid")
            return
        }
        val characteristic = service.getCharacteristic(characteristicUuid)
        if (characteristic == null) {
            Log.e(TAG, "Characteristic is null for UUID: $characteristicUuid")
            return
        }
        if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
            enqueueBleOperation(
                Runnable { gatt!!.readCharacteristic(characteristic) },
                "Read Characteristic: " + characteristic.uuid.toString()
            )
        } else {
            Log.e(TAG, "Characteristic is not readable: " + characteristic.uuid.toString())
        }
    }

    fun enqueueWriteCharacteristic(characteristicUuid: UUID, value: ByteArray, desc: String?) {
        if (gatt == null) {
            Log.e(TAG, "GATT is null, Writing \"$desc\" Aborted")
            return
        }
        var characteristic = characteristicMap[characteristicUuid]
        if (characteristic == null) {
            // Fallback to find characteristic if not in map (e.g. for auth char before full discovery)
            if (characteristicUuid == AUTH_CHARACTERISTIC_UUID && gatt!!.getService(
                    AUTH_SERVICE_UUID
                ) != null
            ) {
                characteristic = gatt!!.getService(AUTH_SERVICE_UUID).getCharacteristic(
                    AUTH_CHARACTERISTIC_UUID
                )
            }
            if (characteristic == null) {
                Log.e(
                    TAG,
                    "Characteristic is null for UUID $characteristicUuid, Writing \"$desc\" Aborted"
                )
                onOperationComplete() // Prevent queue stall
                return
            }
        }

        val finalCharacteristic: BluetoothGattCharacteristic = characteristic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            enqueueBleOperation(
                {
                    val result = gatt!!.writeCharacteristic(
                        finalCharacteristic,
                        value,
                        finalCharacteristic.writeType
                    )
                    Log.d(
                        TAG,
                        MessageFormat.format(
                            "Wrote {0} to characteristic {1}, result: {2}",
                            value.contentToString(),
                            finalCharacteristic.uuid,
                            result
                        )
                    )
                },
                Optional.ofNullable<String?>(desc)
                    .orElseGet(Supplier { "Write Characteristic: $characteristicUuid" })
            )
        } else {
            finalCharacteristic.value = value
            enqueueBleOperation({
                val result = gatt!!.writeCharacteristic(finalCharacteristic)
                Log.d(
                    TAG,
                    MessageFormat.format(
                        "LEGACY :: Wrote {0} to characteristic {1}, result: {2}",
                        value.contentToString(),
                        finalCharacteristic.uuid,
                        result
                    )
                )
            }, desc ?: ("Write Characteristic: $characteristicUuid"))
        }
    }

    //endregion
    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02X ", b))
        }
        return sb.toString().trim { it <= ' ' }
    }

    private fun enqueueBleOperation(operation: Runnable?, desc: String?) {
        synchronized(bleOperations) {
            bleOperations.add(operation)
            Log.d(
                TAG,
                MessageFormat.format(
                    "Added operation to queue[{0}], size: {1}",
                    desc,
                    bleOperations.size
                )
            )
            if (!isBleBusy) {
                processNextOperation()
            }
        }
    }

    private fun processNextOperation() {
        synchronized(bleOperations) {
            if (!bleOperations.isEmpty()) {
                Log.d(TAG, "Processing next operation, remaining: " + bleOperations.size)
                isBleBusy = true
                val op = bleOperations.poll()
                if (op != null) {
                    op.run()
                } else {
                    Log.e(TAG, "Enqueued operation is null, skipping")
                    isBleBusy = false // Ensure not stuck
                    processNextOperation()
                }
            } else {
                isBleBusy = false
                Log.d(TAG, "No more operations to process")
            }
            connectionCallback?.pendingBleProcessChanged(
                bleOperations.size
            )
        }
    }

    // Call this after a BLE operation completes (write, read, descriptor write)
    // This method is now consistently called from the gattCallback methods for authenticated operations
    fun onOperationComplete() { // Made public to be called by InfoHandler if needed
        isBleBusy = false // Mark as not busy before processing next
        processNextOperation()
    }

    private fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val properties = characteristic.properties
        gatt!!.setCharacteristicNotification(characteristic, true) // Enable locally first

        // Find the CCCD (Client Characteristic Configuration Descriptor)
        val descriptor =
            characteristic.getDescriptor(AUTH_CHARACTERISTIC_DESCRIPTOR_UUID) // Using the common CCCD UUID
        if (descriptor == null) {
            Log.e(TAG, "CCCD not found for characteristic: " + characteristic.uuid)
            // onOperationComplete(); // If we consider this op failed, to unblock queue
            return
        }

        val valueToSet: ByteArray?
        if ((properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            valueToSet = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else if ((properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
            valueToSet = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            Log.e(TAG, "Characteristic does not support notifications or indications: " + characteristic.uuid)
            // onOperationComplete(); // If we consider this op failed
            return
        }

        descriptor.value = valueToSet
        enqueueBleOperation({
            val success = gatt!!.writeDescriptor(descriptor)
            if (!success) {
                Log.e(TAG, "Failed to write descriptor for " + characteristic.uuid)
                // Consider calling onOperationComplete() here if this failure should unblock the queue
            }
        }, "Write CCCD for " + characteristic.uuid)
    }

    fun enableDoNotDisturb() {
        val cmd: ByteArray = HuamiService.COMMAND_DO_NOT_DISTURB_AUTOMATIC.clone()
        cmd[1] = (cmd[1].toInt() and 0x80.inv()).toByte()
        enqueueWriteCharacteristic(
            HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION,
            cmd,
            "Enable Do Not Disturb"
        )
    }

    fun changeDateFormat(dateFormat: String) {
        val cmd = HuamiService.DATEFORMAT_DATE_MM_DD_YYYY // Ensure this is correctly defined
        // Ensure dateFormat is not too long for the cmd array starting at index 3
        val dateFormatBytes = dateFormat.toByteArray()
        val len = min(dateFormatBytes.size, cmd.size - 3)
        System.arraycopy(dateFormatBytes, 0, cmd, 3, len)
        enqueueWriteCharacteristic(
            HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION,
            cmd,
            "Change Date Format"
        )
    }

    fun changeTimeFormat(is24HourFormat: Boolean) {
        if (is24HourFormat) {
            enqueueWriteCharacteristic(
                HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION,
                HuamiService.DATEFORMAT_TIME_24_HOURS,
                "Change Date Format"
            )
        } else {
            enqueueWriteCharacteristic(
                HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION,
                HuamiService.DATEFORMAT_TIME_12_HOURS,
                "Change Date Format"
            )
        }
    }

    fun heartRateMonitoring(enable: Boolean) {
        if (enable) {
            enqueueWriteCharacteristic(
                HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT,
                stopHeartMeasurementContinuous,
                "Disabling Continuous Heart Rate Measurement"
            )
            enqueueWriteCharacteristic(
                HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT,
                startHeartMeasurementManual,
                "Enabling Manual Heart Rate Measurement"
            )
        } else {
            enqueueWriteCharacteristic(
                HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT,
                stopHeartMeasurementManual,
                "Disabling Manual Heart Rate Measurement"
            )
        }
    }

    fun continuousHeartRateMonitoring(enable: Boolean) {
        if (enable) {
            enqueueWriteCharacteristic(
                HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT,
                stopHeartMeasurementManual,
                "Disabling Manual Heart Rate Measurement"
            )
            enqueueWriteCharacteristic(
                HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT,
                startHeartMeasurementContinuous,
                "Enabling Continuous Heart Rate Measurement"
            )
        } else {
            enqueueWriteCharacteristic(
                HuamiService.UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT,
                stopHeartMeasurementContinuous,
                "Disabling Continuous Heart Rate Measurement"
            )
        }
    }

    fun liftWristToWake(enable: Boolean) {
        val cmd =
            if (enable) HuamiService.COMMAND_ENABLE_DISPLAY_ON_LIFT_WRIST else HuamiService.COMMAND_DISABLE_DISPLAY_ON_LIFT_WRIST
        //for scheduled enabling, write last 4 bytes as start (byte)HH, (byte)MM, end (byte)HH, (byte)MM
        enqueueWriteCharacteristic(
            HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION,
            cmd,
            "Lift Wrist To Wake"
        )
    }

    fun sendFindDeviceCommand(start: Boolean) {
        val UUID_CHARACTERISTIC_ALERT_LEVEL = UUID.fromString((String.format(HuamiService.BASE_UUID, "2A06")))
        val cmd = if (start) byteArrayOf(3) else byteArrayOf(0) // Amazfit uses 3 to start, 0 to stop
        enqueueWriteCharacteristic(UUID_CHARACTERISTIC_ALERT_LEVEL, cmd, "Send Find Device Command")
    }

    fun enable24HrFormatTime(enable: Boolean) {
        val cmd = if (enable) HuamiService.DATEFORMAT_TIME_24_HOURS else HuamiService.DATEFORMAT_TIME_12_HOURS
        enqueueWriteCharacteristic(
            HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION,
            cmd,
            "change Time Format"
        )
    }

    fun onSetPhoneVolume(volume: Float) {
        val volumeCommand = byteArrayOf(HuamiService.MUSIC_FLAG_VOLUME, volume.roundToInt().toByte())
        writeToChunkedOld(3, volumeCommand)
    }

    //Doesn't Work
    fun sendReboot() {
        enqueueWriteCharacteristic(
            HuamiService.UUID_CHARACTERISTIC_FIRMWARE_CONTROL,
            byteArrayOf(HuamiService.COMMAND_FIRMWARE_REBOOT),
            "Sending Reboot Command"
        )
    }

    private fun handleChunkedRead(value: ByteArray) {
        when (value[0]) {
            (0x03).toByte() -> {
                if (huami2021ChunkedDecoder != null) {
                    val needsAck = huami2021ChunkedDecoder!!.decode(value)
                    if (needsAck) {
                        sendChunkedAck()
                    }
                } else {
                    Log.w(TAG, "Got chunked payload, but decoder is null")
                }
                return
            }

            (0x04).toByte() -> {
                val handle = value[2]
                val count = value[4]
                Log.i(
                    TAG,
                    MessageFormat.format("Got chunked ack, handle={0}, count={1}", handle, count)
                )
                // TODO: We should probably update the handle and count on the encoder
                return
            }

            else -> Log.w(
                TAG,
                MessageFormat.format("Unhandled chunked payload of type {0}", value[0])
            )
        }
    }

    fun sendChunkedAck() {
        val handle = huami2021ChunkedDecoder!!.lastHandle
        val count = huami2021ChunkedDecoder!!.lastCount
        sendChunkedReadAcknowledgement(byteArrayOf(0x04, 0x00, handle, 0x01, count))
    }

    private fun sendChunkedReadAcknowledgement(value: ByteArray) {
        val chunkedReadCharUUID = HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ // Assuming this is correct
        if (characteristicMap[chunkedReadCharUUID] == null && characteristicChunked2021Read == null) {
            Log.e(
                TAG,
                "Chunked read characteristic (2021) is null, can't write :: Send chunked ack"
            )
            onOperationComplete() // Unblock queue
            return
        }
        // Prefer the explicitly stored characteristic if available
        val targetCharUUID: UUID = (if (characteristicChunked2021Read != null) characteristicChunked2021Read!!.uuid else chunkedReadCharUUID)!!

        try {
            // Note: Writing to a "read" characteristic is unusual, ensure this is the correct behavior for ack
            enqueueWriteCharacteristic(targetCharUUID, value, "Send chunked ack")
        } catch (e: Exception) {
            Log.e(TAG, MessageFormat.format("Failed to send chunked ack, {0}", e))
        }
    }

    //Used to send large data
    protected fun writeToChunkedOld(type: Int, data: ByteArray) {
        val MAX_CHUNKLENGTH = mMTU - 6
        var remaining = data.size
        var count: Byte = 0
        while (remaining > 0) {
            val copybytes = min(remaining, MAX_CHUNKLENGTH)
            val chunk = ByteArray(copybytes + 3)

            var flags: Byte = 0
            if (remaining <= MAX_CHUNKLENGTH) {
                flags = (0 or 0x80).toByte() // last chunk
                if (count.toInt() == 0) {
                    flags = (flags.toInt() or 0x40).toByte() // weird but true
                }
            } else if (count > 0) {
                flags = (0 or 0x40).toByte() // consecutive chunk
            }

            chunk[0] = 0
            chunk[1] = (flags.toInt() or type).toByte()
            chunk[2] = (count.toInt() and 0xff).toByte()

            System.arraycopy(data, count++ * MAX_CHUNKLENGTH, chunk, 3, copybytes)
            enqueueWriteCharacteristic(
                HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER,
                chunk,
                "Writing chunked payload"
            )
            remaining -= copybytes
        }
    }

    fun onMusicAppOpenOnWatch(opened: Boolean) {
        this.isMusicAppOpen = opened
        if (opened) {
            MediaUtil.refresh(context)
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                sendMusicStateToDevice(MediaUtil.bufferMusicBean, MediaUtil.bufferMusicStateBean)
                onSetPhoneVolume(MediaUtil.getPhoneVolume(applicationContext).toFloat())
            }, 100)
        }
    }

    protected fun sendMusicStateToDevice(musicBean: MusicBean?, musicStateBean: MusicStateBean?) {
        if (!isMusicAppOpen || characteristicChunked2021Write == null || musicBean == null || musicStateBean == null) {
            return
        }
        writeToChunkedOld(3, MediaUtil.encodeMusicState(musicBean, musicStateBean))
        Log.i(TAG, "sendMusicStateToDevice: $musicBean, $musicStateBean")
    }


    fun setCallStatus(callStatus: CallStatus?, caller: String?) {
        Log.e(TAG, "CALLER_NAME = $caller")
        if (callStatus == CallStatus.INCOMING) {
            sendIncomingCallAlert(caller)
        } else if ((callStatus == CallStatus.PICKED) || (callStatus == CallStatus.ENDED)) {
            writeToChunkedOld(0, byteArrayOf(3, 3, 0, 0, 0, 0))
        }
    }

    //Does not work needs fix
    fun setTime() {
        val timestamp: Calendar = ConversionUtil.createCalendar()
        timestamp.set(2025, 9, 20)
        val year = ConversionUtil.fromUint16(timestamp.get(Calendar.YEAR))
        val timeByte = byteArrayOf(
            year[0],
            year[1],
            ConversionUtil.fromUint8(timestamp.get(Calendar.MONTH) + 1),
            ConversionUtil.fromUint8(timestamp.get(Calendar.DATE)),
            ConversionUtil.fromUint8(timestamp.get(Calendar.HOUR_OF_DAY)),
            ConversionUtil.fromUint8(timestamp.get(Calendar.MINUTE)),
            ConversionUtil.fromUint8(timestamp.get(Calendar.SECOND)),
            ConversionUtil.dayOfWeekToRawBytes(timestamp),
            0,  // fractions256 (not set)
            // 0 (DST offset?) Mi2
            // k (tz) Mi2
        )
        enqueueWriteCharacteristic(
            HuamiService.UUID_CHARACTERISTIC_CURRENT_TIME,
            timeByte,
            "Set Current Time"
        )
    }

    fun updateConnectionState() {
        NotificationUtility.updateNotification(
            context,
            deviceInfo.connected,
            this@GTR2eBleService
        )
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    private fun registerDevicePresence() {
        val cdm = getSystemService(COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val request = ObservingDevicePresenceRequest.Builder()
                .setAssociationId(Prefs.getLastDeviceAssociationId(context))
                .build()
            cdm.startObservingDevicePresence(request)
        } else {
            val mac = Prefs.getLastDeviceMac(context)
            if (mac != null) {
                cdm.startObservingDevicePresence(mac)
                Log.d(TAG, "Started observing device presence for: $mac")
            }
        }
    }

    private fun registerVolumeListener() {
        val volumeChangeReceiver = GTR2eVolumeChangeReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(ANDROID_MEDIA_VOLUME_CHANGED_ACTION)
        ContextCompat.registerReceiver(
            this.context!!,
            volumeChangeReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    //mute call
    fun muteCall() {
        val intent: Intent = Intent(COM_VIKAS_GTR_2_E_MUTE_CALL)
        sendBroadcast(intent)
    }

    private fun getBytesFromHex(hex: String): ByteArray {
        var hex = hex
        hex = hex.replace("0x", "")
        val bytes = ByteArray(hex.length / 2)
        var i = 0
        while (i < hex.length) {
            bytes[i / 2] = hex.substring(i, i + 2).toInt(16).toByte()
            i += 2
        }
        return bytes
    }

    fun setMtu(mtu: Int) {
        if (mtu < MIN_MTU) {
            Log.e(TAG, "Device announced unreasonable low MTU of $mtu, ignoring")
            return
        }
        mMTU = mtu
        if (huami2021ChunkedEncoder != null) {
            huami2021ChunkedEncoder!!.setMTU(mMTU)
        }
    }

    fun setMusicControl(control: MusicControl?) {
        MediaUtil.setMediaState(applicationContext, control)
        if (control == MusicControl.VOLUME_UP || control == MusicControl.VOLUME_DOWN) {
            onSetPhoneVolume(MediaUtil.getPhoneVolume(applicationContext).toFloat())
        }
    }

    fun updateMediaController() {
        val newController = MediaUtil.getMediaController(applicationContext) ?: return
        // same session -> ignore
        if (currentController != null && currentController!!.sessionToken == newController.sessionToken) {
            return
        }
        Log.d("MEDIA", "Switching media session")
        // unregister old
        if (currentController != null && mediaCallback != null) {
            currentController!!.unregisterCallback(mediaCallback!!)
        }
        currentController = newController
        // create callback
        mediaCallback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                if (state == null) {
                    Log.d("MEDIA", "State null → refreshing controller")
                    updateMediaController()
                    return
                }
                Log.d("MEDIA", "Playback changed: " + state.state)
                val newState = MediaUtil.extractMusicStateBean(state)
                if (MediaUtil.bufferMusicStateBean != newState) {
                    MediaUtil.bufferMusicStateBean = MediaUtil.extractMusicStateBean(state)
                    sendMusicStateToDevice(
                        MediaUtil.bufferMusicBean,
                        MediaUtil.bufferMusicStateBean
                    )
                }
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                if (metadata == null) {
                    Log.d("MEDIA", "Metadata null → refreshing controller")
                    updateMediaController()
                    return
                }
                Log.d("MEDIA", "Metadata changed")
                val newMetaData = MediaUtil.extractMusicBean(metadata)
                if (MediaUtil.bufferMusicBean != newMetaData) {
                    MediaUtil.bufferMusicBean = MediaUtil.extractMusicBean(metadata)
                    sendMusicStateToDevice(
                        MediaUtil.bufferMusicBean,
                        MediaUtil.bufferMusicStateBean
                    )
                }
            }

            override fun onSessionDestroyed() {
                super.onSessionDestroyed()
                Log.e("MEDIA", "Session Destroyed")
                if (currentController != null && mediaCallback != null) {
                    currentController!!.unregisterCallback(mediaCallback!!)
                }
            }
        }

        currentController!!.registerCallback(mediaCallback!!, Handler(Looper.getMainLooper()))

        // push initial state immediately
        MediaUtil.bufferMusicBean = MediaUtil.extractMusicBean(currentController!!.metadata)
        MediaUtil.bufferMusicStateBean = MediaUtil.extractMusicStateBean(currentController!!.playbackState)
        sendMusicStateToDevice(MediaUtil.bufferMusicBean, MediaUtil.bufferMusicStateBean)
    }

    fun setWatchFaceWithId(id: Int) {
        enqueueWriteCharacteristic(
            HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION,
            GTR2eWatchFaceUtil.setWatchFaceById(id),
            "Set watch face"
        )
    }

    fun requestWatchFaceIdList() {
        enqueueWriteCharacteristic(
            HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION,
            GTR2eWatchFaceUtil.getWatchFaceListCommand(),
            "Get Watch Id List"
        )
    }

    fun testNotifications(packageName: String?, appName: String?, message: String?) {
//        onNotification(packageName, appName, message);
//        writeToChunkedOld(0, GTR2eNotificationUtil.getWeatherAlertData("26 C temperature in Bhubaneswar, Odisha", "Test"));

        enqueueWriteCharacteristic(
            HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION,
            GTR2eWatchFaceUtil.getCurrentWatchFace(),
            "Get Watch Id List"
        )
    }


    fun onNotification(packageName: String?, sourceName: String?, message: String?) {
        if (!deviceInfo.connected || !deviceInfo.authenticated || characteristicChunked2021Write == null) {
            return
        }
        writeToChunkedOld(
            GTR2eNotificationUtil.NOTIFICATION_WRITE_TYPE,
            GTR2eNotificationUtil.getNotificationData(packageName, sourceName, message)
        )
    }

    fun sendMissedCallAlert(contactName: String?) {
        if (!deviceInfo.connected || !deviceInfo.authenticated || characteristicChunked2021Write == null) {
            return
        }
        writeToChunkedOld(
            GTR2eNotificationUtil.NOTIFICATION_WRITE_TYPE,
            GTR2eNotificationUtil.getMissedCallAlertData(contactName)
        )
    }

    fun sendIncomingCallAlert(contactName: String?) {
        if (!deviceInfo.connected || !deviceInfo.authenticated || characteristicChunked2021Write == null) {
            return
        }
        writeToChunkedOld(
            GTR2eNotificationUtil.NOTIFICATION_WRITE_TYPE,
            GTR2eNotificationUtil.getIncomingCallAlertData(contactName)
        )
    }

    // Binder class for clients to access this service
    inner class LocalBinder : Binder() {
        val service: GTR2eBleService get() = this@GTR2eBleService
    }

    companion object {
        // UUIDs for Huami protocol
        val AUTH_SERVICE_UUID: UUID = UUID.fromString("0000fee1-0000-1000-8000-00805f9b34fb")
        const val COMMAND_SET_HR_SLEEP: Byte = 0x0
        const val COMMAND_SET__HR_CONTINUOUS: Byte = 0x1
        const val COMMAND_SET_HR_MANUAL: Byte = 0x2
        const val MIN_MTU: Int = 23
        private const val TAG = "GTR2eBleService"
        private val AUTH_CHARACTERISTIC_UUID: UUID = UUID.fromString("00000009-0000-3512-2118-0009af100700")
        private val AUTH_CHARACTERISTIC_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private val HUAMI_SERVICE_UUID: UUID = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb")
        private val CHUNKED_READ_UUID: UUID = UUID.fromString("00000017-0000-3512-2118-0009af100700")
        private val DEFAULT_AUTH_KEY = byteArrayOf(
            0x6c.toByte(), 0xd3.toByte(), 0x1f.toByte(), 0x60.toByte(),
            0x6d.toByte(), 0xf4.toByte(), 0xb7.toByte(), 0x8d.toByte(),
            0x3a.toByte(), 0xfd.toByte(), 0xcc.toByte(), 0x0f.toByte(),
            0x7b.toByte(), 0x61.toByte(), 0x74.toByte(), 0x84.toByte()
        )
        private val startHeartMeasurementManual = byteArrayOf(0x15, COMMAND_SET_HR_MANUAL, 1)
        private val stopHeartMeasurementManual = byteArrayOf(0x15, COMMAND_SET_HR_MANUAL, 0)
        private val startHeartMeasurementContinuous = byteArrayOf(0x15, COMMAND_SET__HR_CONTINUOUS, 1)
        private val stopHeartMeasurementContinuous = byteArrayOf(0x15, COMMAND_SET__HR_CONTINUOUS, 0)
        const val ANDROID_MEDIA_VOLUME_CHANGED_ACTION: String = "android.media.VOLUME_CHANGED_ACTION"
        const val COM_VIKAS_GTR_2_E_MUTE_CALL: String = "com.vikas.gtr2e.MUTE_CALL"
    }
}
