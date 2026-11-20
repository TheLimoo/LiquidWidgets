package com.liquidwidgets.app.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.data.providers.WeatherCondition
import com.liquidwidgets.app.data.providers.WeatherDataProvider
import com.liquidwidgets.app.engine.MetaballSimulation
import com.liquidwidgets.app.receivers.WidgetActionReceiver
import kotlin.math.sin

class LiquidWeatherWidget : BaseLiquidGlanceWidget(WidgetType.WEATHER) {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (config, palette, ripple) = loadContextState(context)
        val weather = WeatherDataProvider.read(context)
        val timeSec = (System.currentTimeMillis() % 600_000L) / 1000f

        provideContent {
            val size = LocalSize.current
            val bmp = renderBitmap(context, size, config, palette, ripple, contentKey = "${weather?.tempC}|${weather?.condition}|${weather?.hourly?.joinToString { it.tempC.toString() }}") { canvas, w, h, txt ->
                val wthr = weather
                if (wthr == null) {
                    // No location granted and no cached forecast: never show fake weather.
                    txt.textAlign = Paint.Align.CENTER
                    txt.textSize = (h * 0.16f * config.fontSizeScale).coerceIn(20f, 40f)
                    canvas.drawText("No weather yet", w * 0.5f, h * 0.46f, txt)
                    txt.textSize = (h * 0.11f).coerceIn(14f, 24f)
                    txt.alpha = 210
                    canvas.drawText("Set location in Settings", w * 0.5f, h * 0.64f, txt)
                    return@renderBitmap
                }
                val weather = wthr
                // Condition-specific real liquid weather physics
                when (weather.condition) {
                    WeatherCondition.CLEAR -> {
                        val sunGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            shader = RadialGradient(
                                w * 0.82f, h * 0.24f, minOf(w, h) * 0.55f,
                                intArrayOf(Color.argb(180, 255, 215, 90), Color.TRANSPARENT),
                                floatArrayOf(0f, 1f),
                                Shader.TileMode.CLAMP
                            )
                        }
                        canvas.drawCircle(w * 0.82f, h * 0.24f, minOf(w, h) * 0.55f, sunGlow)
                    }
                    WeatherCondition.RAIN, WeatherCondition.STORM -> {
                        // Rain droplets running down the glass
                        val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.argb(190, 210, 245, 255)
                            strokeCap = Paint.Cap.ROUND
                            strokeWidth = 5f
                        }
                        for (i in 0..11) {
                            val x = w * ((i * 0.083f + 0.05f) % 0.92f)
                            val y = ((timeSec * (55f + (i % 3) * 20f) + i * 37f) % h)
                            canvas.drawLine(x, y, x, y + 18f, dropPaint)
                            MetaballSimulation.drawLiquidDroplet(canvas, x, y + 20f, 5.5f, Color.parseColor("#88E8FF"))
                        }
                    }
                    WeatherCondition.SNOW -> {
                        val flakePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.argb(215, 255, 255, 255)
                        }
                        for (i in 0..16) {
                            val x = w * ((i * 0.067f + 0.04f) % 0.94f) + sin(timeSec + i) * 10f
                            val y = ((timeSec * 22f + i * 41f) % h)
                            canvas.drawCircle(x, y, 4f + (i % 3) * 2f, flakePaint)
                        }
                    }
                    WeatherCondition.CLOUDY -> {
                        MetaballSimulation.drawRisingBubbles(canvas, w, h, h * 0.2f, timeSec, 0.55f, 0.6f, Color.WHITE)
                    }
                }

                // Giant liquid temperature numerals
                txt.textAlign = Paint.Align.LEFT
                txt.textSize = (h * 0.42f * config.fontSizeScale).coerceIn(42f, 130f)
                canvas.drawText("${weather.tempC}°", w * 0.08f, h * 0.48f, txt)

                txt.textSize = (h * 0.12f * config.fontSizeScale).coerceIn(16f, 30f)
                canvas.drawText(weather.conditionLabel, w * 0.08f, h * 0.66f, txt)

                txt.textSize = (h * 0.095f).coerceIn(13f, 22f)
                txt.alpha = 200
                canvas.drawText("${weather.locationLabel} • ${weather.humidityPercent}% RH", w * 0.08f, h * 0.80f, txt)

                // Hourly forecast as small glass bubbles
                weather.hourly.take(4).forEachIndexed { idx, hour ->
                    val bx = w * (0.47f + idx * 0.135f)
                    val by = h * 0.66f
                    val br = (minOf(w, h) * 0.145f).coerceIn(22f, 38f)
                    MetaballSimulation.drawLiquidDroplet(
                        canvas = canvas,
                        cx = bx,
                        cy = by,
                        radius = br,
                        tintColor = config.theme.tintArgb.toInt(),
                        label = "${hour.tempC}°"
                    )
                    txt.textAlign = Paint.Align.CENTER
                    txt.textSize = (h * 0.085f).coerceIn(12f, 19f)
                    canvas.drawText(hour.hourLabel, bx, by + br + 18f, txt)
                }
            }

            GlassBitmapSurface(
                bitmap = bmp,
                contentDescription = "Liquid Weather ${weather?.tempC?.let { "$it°" } ?: "no data"} ${weather?.conditionLabel ?: ""}",
                onTap = broadcastAction(context, WidgetActionReceiver.ACTION_OPEN_WEATHER)
            )
        }
    }
}

class LiquidWeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiquidWeatherWidget()
}
