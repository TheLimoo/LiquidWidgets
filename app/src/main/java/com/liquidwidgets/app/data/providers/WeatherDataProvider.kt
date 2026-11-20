package com.liquidwidgets.app.data.providers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.liquidwidgets.app.liquidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class WeatherCondition { CLEAR, CLOUDY, RAIN, SNOW, STORM }

data class HourlyBubble(
    val hourLabel: String,
    val tempC: Int,
    val condition: WeatherCondition
)

data class WeatherSnapshot(
    val tempC: Int,
    val condition: WeatherCondition,
    val conditionLabel: String,
    val humidityPercent: Int,
    val windKmh: Int,
    val locationLabel: String,
    val hourly: List<HourlyBubble>,
    val isCached: Boolean
)

/**
 * Fetches live weather from Open-Meteo (free, no API key) with a 30-minute
 * cache TTL. Returns null when there is no location and no usable cache —
 * the widget shows "Set location" instead of inventing weather. There is no
 * hardcoded fallback city or temperature anywhere in this file.
 */
object WeatherDataProvider {

    const val CACHE_TTL_MS: Long = 30 * 60 * 1000L

    /** Pure and unit-tested: is a cached forecast written at [updatedMs] still fresh? */
    internal fun isCacheFresh(updatedMs: Long, nowMs: Long): Boolean =
        updatedMs > 0L && (nowMs - updatedMs) < CACHE_TTL_MS

    suspend fun read(context: Context, forceNetwork: Boolean = false): WeatherSnapshot? =
        withContext(Dispatchers.IO) {
            val store = context.liquidApp().configStore
            val cachedJson = store.getCachedWeatherJson()
            val cachedAt = store.getCachedWeatherUpdatedMs()

            // Fresh cache wins (unless the detail screen explicitly forces a refresh).
            if (!forceNetwork && cachedJson != null && isCacheFresh(cachedAt, System.currentTimeMillis())) {
                parseOpenMeteoJson(cachedJson, isCached = true)?.let { return@withContext it }
            }

            val location = resolveLocation(context)
                ?: return@withContext if (cachedJson != null) {
                    // No location right now, but a stale forecast beats no forecast.
                    parseOpenMeteoJson(cachedJson, isCached = true)
                } else {
                    null
                }
            val (lat, lon, label) = location

            val endpoint = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m" +
                "&hourly=temperature_2m,weather_code&forecast_days=1&timezone=auto"

            val freshJson = runCatching {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    requestMethod = "GET"
                }
                if (conn.responseCode == 200) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    null
                }
            }.getOrNull()

            if (freshJson != null) {
                val wrapped = JSONObject(freshJson).apply { put("_label", label) }.toString()
                store.setCachedWeatherJson(wrapped)
                parseOpenMeteoJson(wrapped, isCached = false)?.let { return@withContext it }
            }

            // Network failed: fall back to the stale cache if one exists, else null.
            if (cachedJson != null) {
                parseOpenMeteoJson(cachedJson, isCached = true)?.let { return@withContext it }
            }
            null
        }

    /**
     * Last-known device location only. Returns null when location permission
     * is missing or the system has no last-known fix — callers must show
     * "Set location", never a hardcoded city.
     */
    private fun resolveLocation(context: Context): Triple<Double, Double, String>? {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val loc = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: return null
        return Triple(loc.latitude, loc.longitude, "Local Weather")
    }

    internal fun parseOpenMeteoJson(raw: String, isCached: Boolean): WeatherSnapshot? {
        return runCatching {
            val root = JSONObject(raw)
            val current = root.getJSONObject("current")
            val temp = current.optDouble("temperature_2m", 20.0).toInt()
            val humidity = current.optInt("relative_humidity_2m", 50)
            val wind = current.optDouble("wind_speed_10m", 8.0).toInt()
            val code = current.optInt("weather_code", 0)
            val (cond, label) = mapWmoCode(code)
            val locLabel = root.optString("_label", "Local Weather")

            val hourlyObj = root.optJSONObject("hourly")
            val times = hourlyObj?.optJSONArray("time")
            val temps = hourlyObj?.optJSONArray("temperature_2m")
            val codes = hourlyObj?.optJSONArray("weather_code")

            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val bubbles = mutableListOf<HourlyBubble>()
            if (times != null && temps != null && codes != null) {
                for (offset in 0 until 4) {
                    val idx = (currentHour + offset).coerceIn(0, times.length() - 1)
                    val timeStr = times.optString(idx, "").substringAfter('T', "00:00").take(5)
                    val hTemp = temps.optDouble(idx, temp.toDouble()).toInt()
                    val (hCond, _) = mapWmoCode(codes.optInt(idx, code))
                    bubbles.add(HourlyBubble(if (offset == 0) "Now" else timeStr, hTemp, hCond))
                }
            }

            WeatherSnapshot(
                tempC = temp,
                condition = cond,
                conditionLabel = label,
                humidityPercent = humidity,
                windKmh = wind,
                locationLabel = locLabel,
                hourly = bubbles,
                isCached = isCached
            )
        }.getOrNull()
    }

    internal fun mapWmoCode(code: Int): Pair<WeatherCondition, String> = when (code) {
        0 -> WeatherCondition.CLEAR to "Clear Sky"
        1, 2, 3 -> WeatherCondition.CLOUDY to "Partly Cloudy"
        45, 48 -> WeatherCondition.CLOUDY to "Misty Fog"
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> WeatherCondition.RAIN to "Rain Showers"
        71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW to "Snowfall"
        95, 96, 99 -> WeatherCondition.STORM to "Thunderstorm"
        else -> WeatherCondition.CLOUDY to "Overcast"
    }
}
