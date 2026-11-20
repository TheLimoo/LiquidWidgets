package com.liquidwidgets.app.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.data.providers.BatteryDataProvider
import com.liquidwidgets.app.engine.MetaballSimulation
import com.liquidwidgets.app.receivers.WidgetActionReceiver
import java.util.Locale

class LiquidBatteryWidget : BaseLiquidGlanceWidget(WidgetType.BATTERY) {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (config, palette, ripple) = loadContextState()
        val snap = BatteryDataProvider.read(context)
        val timeSec = (System.currentTimeMillis() % 600_000L) / 1000f

        // Color shifts: aqua >60%, amber 20-60%, red liquid <20%
        val levelColor = when {
            snap.percent > 60 -> Color.parseColor("#24E0F5")
            snap.percent >= 20 -> Color.parseColor("#FFB020")
            else -> Color.parseColor("#FF2E4C")
        }

        provideContent {
            val size = LocalSize.current
            val bmp = renderBitmap(context, size, config, palette, ripple) { canvas, w, h, txt ->
                MetaballSimulation.drawLiquidFillTank(
                    canvas = canvas,
                    width = w,
                    height = h,
                    fillFraction = snap.percent / 100f,
                    timeSec = timeSec,
                    liquidColor = levelColor,
                    highlightColor = Color.WHITE,
                    bubbleDensity = if (snap.isCharging) (config.bubbleDensity * 1.4f) else config.bubbleDensity,
                    viscosity = config.viscosity,
                    chargingBoost = snap.isCharging
                )

                val isWide = w > h * 1.35f
                txt.textAlign = if (isWide) Paint.Align.LEFT else Paint.Align.CENTER
                val anchorX = if (isWide) w * 0.10f else w * 0.5f

                txt.textSize = (h * 0.34f * config.fontSizeScale).coerceIn(36f, 110f)
                canvas.drawText("${snap.percent}%", anchorX, h * 0.48f, txt)

                val statusText = buildString {
                    if (snap.isCharging) {
                        append("⚡ ")
                        if (snap.wattage > 0.4f) append(String.format(Locale.US, "%.1fW", snap.wattage))
                        else append("Charging")
                        snap.timeToFullMinutes?.let { append(" · ${it}m left") }
                    } else {
                        append(String.format(Locale.US, "%.1f°C · %s", snap.temperatureC, snap.healthLabel))
                    }
                }
                txt.textSize = (h * 0.115f * config.fontSizeScale).coerceIn(16f, 30f)
                txt.alpha = 230
                canvas.drawText(statusText, anchorX, h * 0.72f, txt)

                if (isWide) {
                    val detail = String.format(Locale.US, "%.1f°C  •  Health: %s", snap.temperatureC, snap.healthLabel)
                    txt.textSize = (h * 0.095f).coerceIn(14f, 24f)
                    txt.alpha = 195
                    canvas.drawText(detail, anchorX, h * 0.87f, txt)
                }

                if (snap.isCharging) {
                    MetaballSimulation.drawLiquidDroplet(
                        canvas = canvas,
                        cx = w * 0.84f,
                        cy = h * 0.24f,
                        radius = (minOf(w, h) * 0.12f).coerceIn(18f, 34f),
                        tintColor = Color.parseColor("#FFE055"),
                        label = "⚡",
                        textColor = Color.parseColor("#112233")
                    )
                }
            }

            GlassBitmapSurface(
                bitmap = bmp,
                contentDescription = "Liquid Battery ${snap.percent}%",
                onTap = broadcastAction(context, WidgetActionReceiver.ACTION_OPEN_BATTERY)
            )
        }
    }
}

class LiquidBatteryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiquidBatteryWidget()
}
