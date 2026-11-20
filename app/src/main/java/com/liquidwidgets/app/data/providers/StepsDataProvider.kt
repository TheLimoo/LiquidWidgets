package com.liquidwidgets.app.data.providers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import com.liquidwidgets.app.liquidApp
import com.liquidwidgets.app.widgets.LiquidStepsWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StepsSnapshot(
    val hasPermission: Boolean,
    val stepsToday: Int,
    val goal: Int,
    val distanceKm: Float,
    val caloriesKcal: Int
)

object StepsDataProvider {

    private const val PREF_NAME = "liquid_steps_state"

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    fun read(context: Context, goal: Int = 10000): StepsSnapshot {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val storedDay = prefs.getString("day", todayKey)
        val steps = if (storedDay == todayKey) prefs.getInt("steps", 0) else 0

        if (hasPermission(context)) {
            sampleStepSensorOnce(context, todayKey)
        }

        val distanceKm = steps * 0.00076f
        val calories = (steps * 0.042f).toInt()
        return StepsSnapshot(
            hasPermission = hasPermission(context),
            stepsToday = steps,
            goal = goal.coerceAtLeast(1000),
            distanceKm = distanceKm,
            caloriesKcal = calories
        )
    }

    /**
     * One-shot step-counter sample. The listener is ALWAYS unregistered:
     * on the first step event, or after a 5-second timeout even if the
     * sensor never fires (previously it leaked forever when no step event
     * arrived). When a step event lands, the Steps widget is refreshed so
     * the new count shows immediately instead of at the next 15-min sync.
     */
    private fun sampleStepSensorOnce(context: Context, todayKey: String) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        val handler = Handler(Looper.getMainLooper())
        var done = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (done) return
                done = true
                handler.removeCallbacksAndMessages(null)
                sm.unregisterListener(this)
                val totalSinceBoot = event.values.firstOrNull()?.toInt() ?: 0
                val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                val savedDay = prefs.getString("day", null)
                val baseline = if (savedDay == todayKey && prefs.contains("baseline")) {
                    prefs.getInt("baseline", totalSinceBoot)
                } else {
                    prefs.edit().putString("day", todayKey).putInt("baseline", totalSinceBoot).apply()
                    totalSinceBoot
                }
                val todaySteps = (totalSinceBoot - baseline).coerceAtLeast(0)
                prefs.edit().putInt("steps", todaySteps).apply()
                context.liquidApp().applicationScope.launch(Dispatchers.Main) {
                    runCatching { LiquidStepsWidget().updateAll(context) }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        handler.postDelayed({
            if (!done) {
                done = true
                sm.unregisterListener(listener)
            }
        }, 5_000L)
    }
}
