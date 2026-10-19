package com.copilot.stealth.engine.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors active audio routing to detect covert Bluetooth earpieces / headsets (e.g. Pixel Buds)
 * versus internal phone microphone and wired routing.
 */
class BluetoothAudioMonitor(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isBluetoothConnected = MutableStateFlow(false)
    val isBluetoothConnected: StateFlow<Boolean> = _isBluetoothConnected.asStateFlow()

    private val _activeDeviceName = MutableStateFlow("Internal Mic")
    val activeDeviceName: StateFlow<String> = _activeDeviceName.asStateFlow()

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            evaluateAudioDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            evaluateAudioDevices()
        }
    }

    init {
        evaluateAudioDevices()
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)
    }

    fun evaluateAudioDevices() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL)
        var foundBluetooth = false
        var deviceName = "Internal Mic"

        for (device in devices) {
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER -> {
                    foundBluetooth = true
                    deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        device.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth Earpiece"
                    } else {
                        "Bluetooth Earpiece"
                    }
                    break
                }
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                    if (!foundBluetooth) {
                        deviceName = "Wired Headset"
                    }
                }
            }
        }

        _isBluetoothConnected.value = foundBluetooth
        _activeDeviceName.value = deviceName
    }

    fun release() {
        try {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        } catch (_: Exception) {
            // Callback not registered or already removed
        }
    }
}
