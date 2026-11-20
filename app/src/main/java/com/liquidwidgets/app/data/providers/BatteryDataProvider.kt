package com.liquidwidgets.app.data.providers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlin.math.abs

data class BatterySnapshot(
    val percent: Int,
    val isCharging: Boolean,
    val wattage: Float,
    val temperatureC: Float,
    val healthLabel: String,
    val timeToFullMinutes: Long?
)

object BatteryDataProvider {

    fun read(context: Context): BatterySnapshot {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 50) ?: 50
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.coerceAtLeast(1) ?: 100
        val pct = ((level * 100f) / scale).toInt().coerceIn(0, 100)

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        val tempC = tempTenths / 10f

        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 3900) ?: 3900
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val currentUa = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        val watts = (abs(currentUa) / 1_000_000f) * (voltageMv / 1000f)

        val healthInt = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
            ?: BatteryManager.BATTERY_HEALTH_GOOD
        val healthLabel = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Warm"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Overvolt"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Degraded"
            else -> "Normal"
        }

        val remainingMs = runCatching { bm?.computeChargeTimeRemaining() ?: -1L }.getOrDefault(-1L)
        val minutesToFull = if (isCharging && remainingMs > 0L) (remainingMs / 60_000L) else null

        return BatterySnapshot(
            percent = pct,
            isCharging = isCharging,
            wattage = watts,
            temperatureC = tempC,
            healthLabel = healthLabel,
            timeToFullMinutes = minutesToFull
        )
    }
}
