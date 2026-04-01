package com.vikas.gtr2e

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.DownloadManager
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.vikas.gtr2e.beans.DeviceInfo
import com.vikas.gtr2e.beans.HuamiBatteryInfo
import com.vikas.gtr2e.databinding.FragmentHomeBinding
import com.vikas.gtr2e.interfaces.ConnectionListener
import com.vikas.gtr2e.utils.AppAutoUpdater
import com.vikas.gtr2e.utils.GTR2eManager
import com.vikas.gtr2e.utils.Prefs
import java.io.File
import java.util.Locale

private const val TAG = "HOME_FRAG"

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var mediaPlayer: MediaPlayer? = null

    // Flags
    private var programmaticallyChangingHeartRateSwitch = false
    private var isProgrammaticallyChangingLiftToWakeSwitch = false
    private var isProgrammaticallyChangingKeepRunningInBackground = false
    private var deviceAddedJustNow = false

    // Updates
    private var pendingInstallUri: Uri? = null
    private var pendingVersionName: String? = null

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var gtr2eManager: GTR2eManager? = null

    // 🔥 Activity Result APIs
    private val bluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) connectToDevice()
            else Toast.makeText(context, "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) {
                initBluetooth()
                initGTR2eManager()
            }
        }

    private val installPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                installApk(pendingInstallUri, pendingVersionName);
                this.pendingVersionName = null;
            } else Toast.makeText(context, "App update install denied", Toast.LENGTH_SHORT).show()
        }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        deviceAddedJustNow = activity?.intent?.getBooleanExtra("DEVICE_ADDED_JUST_NOW", false) == true
        initViews()
        if (checkPermissions()) {
            initBluetooth()
            initGTR2eManager()
        }
        if (Prefs.getAutoAppUpdatesEnabled(requireContext())) {
            AppAutoUpdater.checkForUpdates(requireContext(), this::showUpdateDialog);
        }
    }

    private fun initViews() {
        setFaceWatchIfNeeded(null)
        binding.connectDeviceButton.setOnClickListener { connectToDevice() }
        binding.watchBatteryProgress.progress = 0f
        binding.findWatchButton.setOnClickListener {
            gtr2eManager?.performAction("FIND_WATCH_START")
        }
        binding.continuousHeartRateSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (programmaticallyChangingHeartRateSwitch) {
                programmaticallyChangingHeartRateSwitch = false
                return@setOnCheckedChangeListener
            }
            gtr2eManager?.performAction(if (isChecked) "HEART_RATE_MONITORING_ON" else "HEART_RATE_MONITORING_OFF")
        }
        binding.liftToWakeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isProgrammaticallyChangingLiftToWakeSwitch) {
                isProgrammaticallyChangingLiftToWakeSwitch = false;
                return@setOnCheckedChangeListener
            }
            gtr2eManager?.performAction(if (isChecked) "LIFT_WRIST_TO_WAKE_ON" else "LIFT_WRIST_TO_WAKE_OFF")
        }
        isProgrammaticallyChangingKeepRunningInBackground = true;
        binding.keepRunningInBgSwitch.setChecked(Prefs.getKeepServiceRunningInBG(requireContext()));
        isProgrammaticallyChangingKeepRunningInBackground = false;
        binding.keepRunningInBgSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isProgrammaticallyChangingKeepRunningInBackground) {
                isProgrammaticallyChangingKeepRunningInBackground = false;
                return@setOnCheckedChangeListener
            }
            Prefs.setKeepServiceRunningInBG(requireContext(), isChecked);
        }
    }

    private fun setFaceWatchIfNeeded(deviceInfo: DeviceInfo?) {
        if (deviceInfo == null) {
            if (Prefs.getLastSelectedWatchFaceImageUrl(requireContext()) != null) {
                Glide.with(requireContext())
                    .load(Prefs.getLastSelectedWatchFaceImageUrl(requireContext()))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.watchFaceImage)
                binding.batteryPercentLabel.visibility = View.INVISIBLE
                binding.watchBatteryProgress.visibility = View.INVISIBLE
            } else {
                binding.watchFaceImage.visibility = View.GONE
                binding.batteryPercentLabel.visibility = View.VISIBLE
                binding.watchBatteryProgress.visibility = View.VISIBLE
            }
        } else {
            if(deviceInfo.charging) {
                binding.watchFaceImage.visibility = View.GONE
                binding.batteryPercentLabel.visibility = View.VISIBLE
                binding.watchBatteryProgress.visibility = View.VISIBLE
            } else {
                binding.watchFaceImage.visibility = View.VISIBLE
                binding.batteryPercentLabel.visibility = View.INVISIBLE
                binding.watchBatteryProgress.visibility = View.INVISIBLE
            }
        }
    }

    private fun initBluetooth() {
        val manager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter
    }

    private fun initGTR2eManager() {
        gtr2eManager = GTR2eManager.getInstance(requireActivity())
        gtr2eManager?.setConnectionListener(object : ConnectionListener {

            override fun onBackgroundServiceBound(bound: Boolean) {
                if (bound) {
                    activity?.runOnUiThread {
                        _binding ?: return@runOnUiThread
                        if (gtr2eManager!!.isConnected) {
                            watchConnectionStatusChanged(true)
                            if (gtr2eManager!!.isAuthenticated) {
                                watchAuthenticatedStatusChanged()
                            }
                        } else {
                            watchConnectionStatusChanged(false)
                        }
                    }

                    gtr2eManager!!.bleService.deviceInfoLiveData.observe(viewLifecycleOwner) { _ ->
                        Log.e(TAG, "Device info change observed")
                        activity?.runOnUiThread {
                            _binding ?: return@runOnUiThread
                            updateDeviceInfo()
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        _binding ?: return@runOnUiThread
                        watchConnectionStatusChanged(false)
                    }
                    Log.e(TAG, "Background service not bound")
                }
            }

            override fun onConnectedChanged(connected: Boolean) {
                activity?.runOnUiThread {
                    _binding ?: return@runOnUiThread
                    watchConnectionStatusChanged(connected)
                }
            }

            override fun onAuthenticated() {
                activity?.runOnUiThread {
                    _binding ?: return@runOnUiThread
                    watchAuthenticatedStatusChanged()
                }
            }

            override fun onBatteryInfoUpdated(batteryInfo: HuamiBatteryInfo?) {
                if(activity != null) {
//                    getActivity().runOnUiThread(() -> updateDeviceInfo());
                }
            }

            override fun onHeartRateChanged(heartRate: Int) {
                activity?.runOnUiThread {
                    _binding ?: return@runOnUiThread
                    binding.watchHeartRateText.text = "$heartRate"
                }
            }

            override fun onHeartRateMonitoringChanged(enabled: Boolean) {
                activity?.runOnUiThread {
                    _binding ?: return@runOnUiThread
                    if (enabled) {
                        binding.watchHeartRateIcon.visibility = View.VISIBLE
                        binding.watchHeartRateText.visibility = View.VISIBLE
                        programmaticallyChangingHeartRateSwitch = true
                        binding.continuousHeartRateSwitch.isChecked = true
                    } else {
                        binding.watchHeartRateIcon.setVisibility(View.INVISIBLE)
                        binding.watchHeartRateText.visibility = View.INVISIBLE
                        programmaticallyChangingHeartRateSwitch = true
                        binding.continuousHeartRateSwitch.isChecked = false
                        binding.batteryPercentLabel.visibility = View.VISIBLE
                    }
                }
            }

            override fun findPhoneStateChanged(started: Boolean) {
                activity?.runOnUiThread {
                    _binding ?: return@runOnUiThread
                    if (started) {
                        initFindPhoneTone()
                        mediaPlayer?.start()
                        binding.blackBg.setImageResource(R.drawable.find_phone)
                        binding.chargingIndicatorImgView.visibility = View.INVISIBLE
                        binding.tvStatus.setText(R.string.finding_phone)
                        binding.watchHeartRateIcon.visibility = View.INVISIBLE
                        binding.watchHeartRateText.visibility = View.INVISIBLE
                        binding.batteryPercentLabel.visibility = View.INVISIBLE
                        binding.watchBatteryProgress.visibility = View.INVISIBLE
                    } else {
                        mediaPlayer?.stop()
                        binding.blackBg.setImageResource(R.drawable.gtr_bg)
                        //updateDeviceInfo();
                    }
                }
            }

            override fun pendingBleProcessChanged(count: Int) {
                activity?.runOnUiThread {
                    _binding ?: return@runOnUiThread
                    binding.pendingProcessLabel.text = count.toString()
                }
            }

            override fun onWatchFaceSet(success: Boolean) {
                activity?.runOnUiThread {
                    _binding ?: return@runOnUiThread
                    Toast.makeText(
                        context,
                        if (success) "Watch face set successfully" else "Failed to set watch face",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onError(error: String?) {
                activity?.runOnUiThread {
                    _binding ?: return@runOnUiThread
                    binding.tvStatus.text = "Error: $error"
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            }
        })
        gtr2eManager!!.onMainActivityResumed()
        updateDeviceInfo()
        if (deviceAddedJustNow){
            connectToDevice()
            deviceAddedJustNow = false
        }
    }

    private fun connectToDevice() {
        if (bluetoothAdapter?.isEnabled != true) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Bluetooth permission not granted", Toast.LENGTH_SHORT).show()
                return
            }
            bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }

        gtr2eManager?.let { it ->
            if (it.isConnected) {
                it.disconnect()
                it.getDeviceInfo()?.let { it.forceDisconnected = true }
            } else {
                binding.blutoothStatusIndicatorImgView.setImageResource(R.drawable.rounded_bluetooth_searching_24);
                it.getDeviceInfo()?.let { it.forceDisconnected = false }
                it.startScan()
            }
        }
    }

    private fun watchAuthenticatedStatusChanged() {
        if (gtr2eManager == null || gtr2eManager!!.getDeviceInfo() == null) {
            Log.e(TAG, "DeviceInfo is null in onAuthenticated")
            return
        }
        if(activity != null) {
//            getActivity().runOnUiThread(this::updateDeviceInfo);
        }
    }

    private fun watchConnectionStatusChanged(connected: Boolean) {
        if (gtr2eManager == null || gtr2eManager!!.deviceInfo == null) {
            Log.e(TAG, "DeviceInfo is null in onConnectedChanged")
            binding.tvStatus.text =
                if (connected) "Connected (No DeviceInfo)" else "Disconnected (No DeviceInfo)"
            return
        }

        activity?.let {
            if (connected) {
                binding.tvStatus.setText(R.string.connected_to_gtr_2e)
                binding.connectDeviceButton.setIconResource(R.drawable.rounded_bluetooth_disabled_24)
                binding.connectDeviceButton.setText(R.string.disconnect)
            } else {
                Log.d(TAG, "onDisconnected() called in HomeFragment")
                binding.connectDeviceButton.setIconResource(R.drawable.rounded_bluetooth_connected_24)
                binding.connectDeviceButton.setText(R.string.connect)
            }
        }
    }

    private fun updateDeviceInfo() {
        val manager = gtr2eManager

        if (manager == null) {
            Log.w(TAG, "updateDeviceInfo called but gtr2eManager is null.")

            binding.tvDeviceInfo.setText(R.string.no_device_connected_manager_is_null)
            animateProgressBar(0f)
            binding.batteryPercentLabel.visibility = View.INVISIBLE
            binding.batteryPercentLabel.text = "0%"
            binding.tvStatus.setText(R.string.disconnected)
            binding.chargingIndicatorImgView.visibility = View.INVISIBLE
            binding.blutoothStatusIndicatorImgView.setImageResource(R.drawable.rounded_bluetooth_24)
            binding.continuousHeartRateSwitch.isChecked = false
            binding.continuousHeartRateSwitch.isEnabled = false
            binding.findWatchButton.isEnabled = false
            binding.liftToWakeSwitch.isEnabled = false
            setFaceWatchIfNeeded(null)
            return
        }

        val deviceInfo = manager.deviceInfo

        if (deviceInfo != null && deviceInfo.connected) {
            val info = buildDeviceInfoString(deviceInfo)
            animateProgressBar(deviceInfo.batteryPercentage.toFloat())
            binding.batteryPercentLabel.text = "${deviceInfo.batteryPercentage}%"
            binding.batteryPercentLabel.visibility = View.VISIBLE
            binding.tvDeviceInfo.text = info
            binding.blutoothStatusIndicatorImgView.setImageResource(R.drawable.rounded_bluetooth_connected_24)
            binding.chargingIndicatorImgView.visibility = if (deviceInfo.charging) View.VISIBLE else View.INVISIBLE
            binding.continuousHeartRateSwitch.isEnabled = true
            binding.findWatchButton.isEnabled = true
            binding.liftToWakeSwitch.isEnabled = true
            binding.stepsCountLabel.text = String.format(Locale.ENGLISH, "%d", deviceInfo.steps)
        } else {
            binding.tvDeviceInfo.setText(R.string.no_device_connected)
            animateProgressBar(0f)
            binding.batteryPercentLabel.visibility = View.INVISIBLE
            binding.batteryPercentLabel.text = "0%"
            binding.tvStatus.setText(R.string.disconnected)
            binding.chargingIndicatorImgView.visibility = View.INVISIBLE
            binding.blutoothStatusIndicatorImgView.setImageResource(R.drawable.rounded_bluetooth_24)
            binding.continuousHeartRateSwitch.isChecked = false
            binding.continuousHeartRateSwitch.isEnabled = false
            binding.findWatchButton.isEnabled = false
            binding.liftToWakeSwitch.isEnabled = false
            if (deviceInfo == null) {
                Log.w(TAG, "updateDeviceInfo: currentDeviceInfo is null, UI set to disconnected.")
                binding.stepsCountLabel.text = "N/a"
            }
        }
        setFaceWatchIfNeeded(deviceInfo)
    }

    private fun animateProgressBar(toProgress: Float) {
        val animation = ObjectAnimator.ofFloat(binding.watchBatteryProgress, getString(R.string.progress), toProgress);
        animation.setDuration(1000); // 1 second
        animation.interpolator = AccelerateDecelerateInterpolator()
        animation.start();
    }

    private fun buildDeviceInfoString(deviceInfo: DeviceInfo): String {
        return buildString {
            appendLine("Device: ${deviceInfo.deviceName}")
            appendLine("Sl No: ${deviceInfo.serialNumber}")
            appendLine("HW revision: ${deviceInfo.hardwareRevision}")
            appendLine("SW revision: ${deviceInfo.softwareRevision}")
            appendLine("Battery: ${deviceInfo.batteryPercentage}%")
            appendLine("Charging: ${deviceInfo.chargingStatus}")
            append("Authenticated: ${deviceInfo.authenticated}")
        }
    }

    private fun initFindPhoneTone() {
        if (mediaPlayer != null) {
            mediaPlayer!!.stop();
            mediaPlayer!!.release();
        }
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.findphone);
        mediaPlayer?.isLooping = true
    }

    private fun checkPermissions(): Boolean {
        val permissions = arrayListOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ACCESS_NOTIFICATION_POLICY
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }
        return if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
            false
        } else {
            requestNotificationPolicyAccess();
            true
        }
    }

    private fun requestNotificationPolicyAccess() {
        if (!isNotificationPolicyAccessGranted()) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }

    private fun isNotificationPolicyAccessGranted() : Boolean {
        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        return notificationManager?.isNotificationPolicyAccessGranted() == true
    }

    private fun showUpdateDialog(currentVersion: String, latestVersion: String, downloadUrl: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("New Version Available")
            .setMessage(
                String.format(
                    "Version %s is available.Current version is %s, Would you like to update now?",
                    latestVersion,
                    currentVersion
                )
            )
            .setPositiveButton(
                "Update",
                { _, _ -> downloadAndInstallApk(downloadUrl, latestVersion) })
            .setNegativeButton("Later", null)
            .setCancelable(false)
            .show()
    }

    private fun downloadAndInstallApk(apkUrl: String, versionName: String) {
        val request = DownloadManager.Request(apkUrl.toUri());
        request.setTitle("App Update");
        request.setDescription("Downloading new version...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
            "gtr2e-$versionName.apk"
        );
        val manager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager;
        val downloadId = manager.enqueue(request);

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(manager.getUriForDownloadedFile(downloadId), versionName)
                    context.unregisterReceiver(this)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(onComplete,IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),Context.RECEIVER_EXPORTED)
        } else {
            ContextCompat.registerReceiver(requireContext(),onComplete,IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),ContextCompat.RECEIVER_EXPORTED)
        }
    }

    private fun installApk(apkUri: Uri?, versionName: String?) {
        if (!requireContext().packageManager.canRequestPackageInstalls()) {
            pendingInstallUri = apkUri
            pendingVersionName = versionName
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${requireContext().packageName}".toUri()
            }
            installPermissionLauncher.launch(intent)
            return
        }

        val downloadedApkFilename = "gtr2e-$versionName.apk"
        val apkFileOnDisk = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),downloadedApkFilename)

        if (!apkFileOnDisk.exists()) return

        val fileUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileProvider",
            apkFileOnDisk
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(installIntent)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}