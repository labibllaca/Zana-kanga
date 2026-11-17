package com.labix.navirom.player

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import com.labix.navirom.data.model.SleepTimerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Helper to manage device connectivity state (Bluetooth, Wi-Fi, Mobile Data)
 * when sleep timer actions are triggered. Supports multi-tier shutdown:
 * 1. Root / Shell command execution (svc, cmd, settings put)
 * 2. System Service Reflection (IWifiManager, IBluetooth)
 * 3. Bluetooth Audio Profile disconnection (A2DP, Headset)
 * 4. AudioManager device clearance
 * 5. Direct legacy/system APIs
 * 6. Interactive System Panel / Settings fallback for unrooted Android 10+ / 13+
 */
class DeviceConnectionManager(private val context: Context) {

    companion object {
        private const val TAG = "DeviceConnectionMgr"
    }

    private suspend fun runCommand(cmd: String): Boolean = withContext(Dispatchers.IO) {
        // Shizuku (ADB privileges without root) is the only path that can really
        // switch Wi-Fi/Bluetooth/mobile data off on a locked-down modern Android.
        try {
            if (ShizukuShell.run(cmd)) {
                Log.d(TAG, "Command succeeded via Shizuku: $cmd")
                return@withContext true
            }
        } catch (_: Throwable) {}
        try {
            withTimeoutOrNull(2500L) {
                // 1. Try su (root / emulator userdebug)
                var succeeded = false
                try {
                    val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                    p.outputStream.close()
                    val exit = p.waitFor()
                    if (exit == 0) succeeded = true
                } catch (_: Throwable) {}

                // 2. Try sh
                if (!succeeded) {
                    try {
                        val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                        p.outputStream.close()
                        val exit = p.waitFor()
                        if (exit == 0) succeeded = true
                    } catch (_: Throwable) {}
                }

                // 3. Try direct exec
                if (!succeeded) {
                    try {
                        val p = Runtime.getRuntime().exec(cmd)
                        p.outputStream.close()
                        val exit = p.waitFor()
                        if (exit == 0) succeeded = true
                    } catch (_: Throwable) {}
                }

                succeeded
            } ?: false
        } catch (e: Throwable) {
            false
        }
    }

    private fun bluetoothAdapter(): BluetoothAdapter? = try {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        @Suppress("DEPRECATION")
        manager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    } catch (_: Throwable) {
        null
    }

    /** Radio state, not connection state: disconnected headphones do not count as "off". */
    private fun isBluetoothRadioOff(): Boolean = try {
        val adapter = bluetoothAdapter()
        adapter == null || !adapter.isEnabled
    } catch (_: Throwable) {
        false
    }

    private fun isWifiRadioOff(): Boolean = try {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        @Suppress("DEPRECATION")
        val apiEnabled = wifiManager?.isWifiEnabled == true
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val onWifiTransport =
            cm?.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        !apiEnabled && !onWifiTransport
    } catch (_: Throwable) {
        false
    }

    private fun isMobileDataOff(): Boolean = try {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && telephonyManager != null) {
            !telephonyManager.isDataEnabled
        } else {
            Settings.Global.getInt(context.contentResolver, "mobile_data", 1) == 0
        }
    } catch (_: Throwable) {
        false
    }

    private fun isLocationOff(): Boolean = try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        locationManager?.isLocationEnabled == false
    } catch (_: Throwable) {
        false
    }

    private fun disconnectActiveBluetoothAudio() {
        try {
            val adapter = bluetoothAdapter() ?: return
            fun disconnectProfile(profile: Int) {
                try {
                    adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                        override fun onServiceConnected(profileType: Int, proxy: BluetoothProfile) {
                            try {
                                proxy.connectedDevices.forEach { device ->
                                    @Suppress("DEPRECATION")
                                    proxy.javaClass.methods.find { it.name == "disconnect" }?.invoke(proxy, device)
                                    Log.d(TAG, "Disconnected Bluetooth audio device: ${device.name ?: device.address}")
                                }
                            } catch (e: Throwable) {
                                Log.w(TAG, "Error disconnecting audio devices on profile $profileType", e)
                            } finally {
                                try {
                                    adapter.closeProfileProxy(profileType, proxy)
                                } catch (_: Throwable) {}
                            }
                        }

                        override fun onServiceDisconnected(profileType: Int) {}
                    }, profile)
                } catch (e: Throwable) {
                    Log.w(TAG, "Error accessing Bluetooth profile proxy $profile", e)
                }
            }
            disconnectProfile(BluetoothProfile.A2DP)
            disconnectProfile(BluetoothProfile.HEADSET)
        } catch (e: Throwable) {
            Log.w(TAG, "Error accessing Bluetooth profile proxies", e)
        }
    }

    /**
     * Turns the Bluetooth radio off and verifies the radio state afterwards.
     * Returns true only when the adapter reports disabled, not merely disconnected.
     */
    suspend fun disableBluetooth(): Boolean = withContext(Dispatchers.IO) {
        repeat(3) { attempt ->
            if (isBluetoothRadioOff()) return@withContext true

            try {
                @Suppress("DEPRECATION")
                bluetoothAdapter()?.takeIf { it.isEnabled }?.disable()
            } catch (t: Throwable) {
                Log.w(TAG, "Bluetooth adapter.disable failed: ${t.message}")
            }

            for (cmd in listOf("svc bluetooth disable", "cmd bluetooth_manager disable")) {
                runCommand(cmd)
                if (isBluetoothRadioOff()) return@withContext true
            }

            try {
                val adapter = bluetoothAdapter()
                if (adapter != null) {
                    val serviceField = adapter.javaClass.getDeclaredField("mService")
                    serviceField.isAccessible = true
                    val service = serviceField.get(adapter)
                    val disableMethod = service?.javaClass?.declaredMethods?.find { it.name == "disable" }
                    if (disableMethod != null && service != null) {
                        disableMethod.isAccessible = true
                        when (disableMethod.parameterTypes.size) {
                            2 -> disableMethod.invoke(service, context.packageName, false)
                            1 -> disableMethod.invoke(service, false)
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.d(TAG, "IBluetooth reflection failed: ${t.message}")
            }

            try {
                Settings.Global.putInt(context.contentResolver, "bluetooth_on", 0)
            } catch (_: Throwable) {}

            disconnectActiveBluetoothAudio()

            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audioManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        audioManager.clearCommunicationDevice()
                    }
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = false
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothA2dpOn = false
                    @Suppress("DEPRECATION")
                    audioManager.stopBluetoothSco()
                }
            } catch (t: Throwable) {
                Log.w(TAG, "AudioManager Bluetooth reset failed: ${t.message}")
            }

            delay(if (attempt == 0) 500 else 1200)
        }

        if (isBluetoothRadioOff()) {
            true
        } else {
            Handler(Looper.getMainLooper()).post {
                try {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                    Toast.makeText(context, "Sleep timer: turn Bluetooth off (system blocked the app)", Toast.LENGTH_LONG).show()
                } catch (e: Throwable) {
                    Log.w(TAG, "Could not start Bluetooth settings activity", e)
                }
            }
            false
        }
    }

    /** Turns the Wi-Fi radio off and verifies the radio state afterwards. */
    suspend fun disableWifi(): Boolean = withContext(Dispatchers.IO) {
        repeat(3) { attempt ->
            if (isWifiRadioOff()) return@withContext true

            for (cmd in listOf(
                "cmd wifi set-wifi-enabled disabled",
                "cmd -w wifi set-wifi-enabled disabled",
                "svc wifi disable",
                "settings put global wifi_on 0"
            )) {
                runCommand(cmd)
                if (isWifiRadioOff()) return@withContext true
            }

            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                if (wifiManager != null && wifiManager.isWifiEnabled) {
                    wifiManager.setWifiEnabled(false)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "WifiManager disable failed", t)
            }

            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val serviceField = wifiManager?.javaClass?.getDeclaredField("mService")
                serviceField?.isAccessible = true
                val service = serviceField?.get(wifiManager)
                val setWifiEnabledMethod = service?.javaClass?.declaredMethods?.find { it.name == "setWifiEnabled" }
                if (setWifiEnabledMethod != null && service != null) {
                    setWifiEnabledMethod.isAccessible = true
                    when (setWifiEnabledMethod.parameterTypes.size) {
                        2 -> setWifiEnabledMethod.invoke(service, context.packageName, false)
                        1 -> setWifiEnabledMethod.invoke(service, false)
                    }
                }
            } catch (t: Throwable) {
                Log.d(TAG, "IWifiManager reflection failed: ${t.message}")
            }

            try {
                Settings.Global.putInt(context.contentResolver, Settings.Global.WIFI_ON, 0)
            } catch (_: Throwable) {}

            delay(if (attempt == 0) 500 else 1200)
        }

        if (isWifiRadioOff()) {
            true
        } else {
            Handler(Looper.getMainLooper()).post {
                try {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                    } else {
                        Intent(Settings.ACTION_WIFI_SETTINGS)
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    context.startActivity(intent)
                    Toast.makeText(context, "Sleep timer: turn Wi-Fi off (system blocked the app)", Toast.LENGTH_LONG).show()
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed opening Wi-Fi settings", e)
                }
            }
            false
        }
    }

    /** Turns mobile data off and verifies the radio state afterwards. */
    suspend fun disableMobileData(): Boolean = withContext(Dispatchers.IO) {
        repeat(3) { attempt ->
            if (isMobileDataOff()) return@withContext true

            for (cmd in listOf(
                "svc data disable",
                "cmd telephony data disable",
                "settings put global mobile_data 0"
            )) {
                runCommand(cmd)
                if (isMobileDataOff()) return@withContext true
            }

            try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                val setDataEnabled = telephonyManager?.javaClass?.getDeclaredMethod(
                    "setDataEnabled",
                    Boolean::class.javaPrimitiveType
                )
                setDataEnabled?.isAccessible = true
                setDataEnabled?.invoke(telephonyManager, false)
            } catch (t: Throwable) {
                Log.d(TAG, "TelephonyManager data reflection not available: ${t.message}")
            }

            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val setMobileDataEnabled =
                    connectivityManager?.javaClass?.getDeclaredMethod("setMobileDataEnabled", Boolean::class.javaPrimitiveType)
                setMobileDataEnabled?.isAccessible = true
                setMobileDataEnabled?.invoke(connectivityManager, false)
            } catch (t: Throwable) {
                Log.d(TAG, "ConnectivityManager data reflection not available: ${t.message}")
            }

            try {
                Settings.Global.putInt(context.contentResolver, "mobile_data", 0)
            } catch (_: Throwable) {}

            delay(if (attempt == 0) 500 else 1200)
        }

        if (isMobileDataOff()) {
            true
        } else {
            Handler(Looper.getMainLooper()).post {
                try {
                    context.startActivity(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                    Toast.makeText(context, "Sleep timer: turn mobile data off (system blocked the app)", Toast.LENGTH_LONG).show()
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed opening Mobile Data settings", e)
                }
            }
            false
        }
    }

    /** Turns location/GPS off and verifies that location is actually disabled. */
    suspend fun disableGps(): Boolean = withContext(Dispatchers.IO) {
        repeat(3) { attempt ->
            if (isLocationOff()) return@withContext true

            try {
                @Suppress("DEPRECATION")
                Settings.Secure.putInt(
                    context.contentResolver,
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF
                )
            } catch (t: Throwable) {
                Log.d(TAG, "Secure location_mode write failed: ${t.message}")
            }

            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                val method = locationManager?.javaClass?.getMethod(
                    "setLocationEnabledForUser",
                    Boolean::class.javaPrimitiveType,
                    android.os.UserHandle::class.java
                )
                method?.isAccessible = true
                method?.invoke(locationManager, false, android.os.Process.myUserHandle())
            } catch (t: Throwable) {
                Log.d(TAG, "LocationManager disable reflection failed: ${t.message}")
            }

            runCommand("cmd location set-location-enabled false")

            delay(if (attempt == 0) 500 else 1200)
        }

        if (isLocationOff()) {
            true
        } else {
            Handler(Looper.getMainLooper()).post {
                try {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                    Toast.makeText(context, "Sleep timer: turn Location off (system blocked the app)", Toast.LENGTH_LONG).show()
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed opening Location settings", e)
                }
            }
            false
        }
    }

    suspend fun executeSleepTimerActions(options: SleepTimerOptions) = withContext(Dispatchers.IO) {
        val stillOn = mutableListOf<String>()

        if (options.disableBluetooth && !disableBluetooth()) stillOn += "Bluetooth"
        if (options.disableWifi && !disableWifi()) stillOn += "Wi-Fi"
        if (options.disableMobileData && !disableMobileData()) stillOn += "Mobile data"
        if (options.disableGps && !disableGps()) stillOn += "Location"

        if (stillOn.isNotEmpty()) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "Sleep timer: ${stillOn.joinToString(", ")} could not be switched off by the system",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
