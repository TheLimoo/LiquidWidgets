package com.liquidwidgets.app.data.providers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.liquidwidgets.app.LiquidWidgetsApp
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
 * Fetches live weather from Open-Meteo (free, no API key) and caches offline in DataStore.
 */
object WeatherDataProvider {

    suspend fun read(context: Context, forceNetwork: Boolean = false): WeatherSnapshot =
        withContext(Dispatchers.IO) {
            val store = LiquidWidgetsApp.instance.configStore
            val cachedJson = store.getCachedWeatherJson()

            if (!forceNetwork && cachedJson != null) {
                parseOpenMeteoJson(cachedJson, isCached = true)?.let { return@withContext it }
            }

            val (lat, lon, label) = resolveLocation(context)
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

            if (cachedJson != null) {
                parseOpenMeteoJson(cachedJson, isCached = true)?.let { return@withContext it }
            }

            WeatherSnapshot(
                tempC = 21,
                condition = WeatherCondition.CLEAR,
                conditionLabel = "Offline · Tap to refresh",
                humidityPercent = 52,
                windKmh = 10,
                locationLabel = label,
                hourly = listOf(
                    HourlyBubble("Now", 21, WeatherCondition.CLEAR),
                    HourlyBubble("+1h", 22, WeatherCondition.CLEAR),
                    HourlyBubble("+2h", 22, WeatherCondition.CLOUDY),
                    HourlyBubble("+3h", 20, WeatherCondition.CLOUDY)
                ),
                isCached = true
            )
        }

    private fun resolveLocation(context: Context): Triple<Double, Double, String> {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val loc = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (loc != null) {
                return Triple(loc.latitude, loc.longitude, "Local Weather")
            }
        }
        return Triple(39.9612, -82.9988, "Columbus (Grant Location)")
    }

    private fun parseOpenMeteoJson(raw: String, isCached: Boolean): WeatherSnapshot? {
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

    private fun mapWmoCode(code: Int): Pair<WeatherCondition, String> = when (code) {
        0 -> WeatherCondition.CLEAR to "Clear Sky"
        1, 2, 3 -> WeatherCondition.CLOUDY to "Partly Cloudy"
        45, 48 -> WeatherCondition.CLOUDY to "Misty Fog"
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> WeatherCondition.RAIN to "Rain Showers"
        71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW to "Snowfall"
        95, 96, 99 -> WeatherCondition.STORM to "Thunderstorm"
        else -> WeatherCondition.CLOUDY to "Overcast"
    }
}
