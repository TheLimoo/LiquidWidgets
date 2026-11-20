package com.liquidwidgets.app.data.providers

import android.Manifest
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.provider.Settings

data class QuickControlsState(
    val wifiOn: Boolean,
    val bluetoothOn: Boolean,
    val torchOn: Boolean,
    val dndOn: Boolean
)

object QuickControlsProvider {

    private const val PREF_NAME = "liquid_quick_controls"

    fun read(context: Context): QuickControlsState {
        val wifi = runCatching {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wm?.isWifiEnabled == true
        }.getOrDefault(false)

        val bt = runCatching {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bm?.adapter?.isEnabled == true
        }.getOrDefault(false)

        val dnd = runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val filter = nm?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
            filter != NotificationManager.INTERRUPTION_FILTER_ALL
        }.getOrDefault(false)

        val torch = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean("torch_on", false)

        return QuickControlsState(wifi, bt, torch, dnd)
    }

    fun toggleFlashlight(context: Context): Boolean {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val next = !prefs.getBoolean("torch_on", false)
        return runCatching {
            val camId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            cm.setTorchMode(camId, next)
            prefs.edit().putBoolean("torch_on", next).apply()
            next
        }.getOrDefault(false)
    }

    fun toggleDnd(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (!nm.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }
        val next = if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        } else {
            NotificationManager.INTERRUPTION_FILTER_ALL
        }
        nm.setInterruptionFilter(next)
    }

    /**
     * Wi-Fi opens the system Internet panel. Same honesty rule as
     * Bluetooth: no fake toggle, the user flips the real switch there and
     * the widget re-reads the real state when they come back.
     */
    fun openWifiPanel(context: Context) {
        val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Bluetooth opens the system settings page. Android does not let a
     * normal app toggle Bluetooth (or Wi-Fi) directly, so this never
     * pretends to toggle — the widget labels say "opens settings".
     */
    fun openBluetoothSettings(context: Context) {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
