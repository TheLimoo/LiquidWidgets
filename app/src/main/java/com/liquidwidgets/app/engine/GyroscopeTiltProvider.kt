package com.liquidwidgets.app.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class TiltVector(val x: Float = 0f, val y: Float = 0f)

/**
 * Streams smoothed device tilt [-1..1] from the rotation vector / accelerometer
 * so specular highlights shift smoothly when the phone tilts. Automatically yields
 * zero-power static tilt when Battery Saver is enabled.
 */
class GyroscopeTiltProvider(private val context: Context) {

    fun observeTilt(enabled: Boolean = true): Flow<TiltVector> = callbackFlow {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (!enabled || powerManager?.isPowerSaveMode == true) {
            trySend(TiltVector(0f, 0f))
            awaitClose { }
            return@callbackFlow
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || sensor == null) {
            trySend(TiltVector(0f, 0f))
            awaitClose { }
            return@callbackFlow
        }

        var smoothX = 0f
        var smoothY = 0f
        val alpha = 0.14f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rawX: Float
                val rawY: Float
                if (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                    val rot = FloatArray(9)
                    val orient = FloatArray(3)
                    SensorManager.getRotationMatrixFromVector(rot, event.values)
                    SensorManager.getOrientation(rot, orient)
                    rawX = (orient[2] / 0.85f).coerceIn(-1f, 1f)
                    rawY = (orient[1] / 0.85f).coerceIn(-1f, 1f)
                } else {
                    rawX = (-event.values[0] / 6.5f).coerceIn(-1f, 1f)
                    rawY = (event.values[1] / 6.5f).coerceIn(-1f, 1f)
                }
                smoothX += alpha * (rawX - smoothX)
                smoothY += alpha * (rawY - smoothY)
                trySend(TiltVector(smoothX, smoothY))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
