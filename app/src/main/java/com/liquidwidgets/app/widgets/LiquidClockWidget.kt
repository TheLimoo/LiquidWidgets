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
import com.liquidwidgets.app.engine.MetaballSimulation
import com.liquidwidgets.app.receivers.WidgetActionReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class LiquidClockWidget : BaseLiquidGlanceWidget(WidgetType.CLOCK) {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (config, palette, ripple) = loadContextState()
        val cal = Calendar.getInstance()
        val seconds = cal.get(Calendar.SECOND)
        val minuteFill = (seconds / 60f).coerceIn(0.06f, 0.96f)
        val timePattern = if (config.clockUse24Hour) "HH:mm" else "h:mm"
        val timeStr = SimpleDateFormat(timePattern, Locale.getDefault()).format(Date())
        val dateStr = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
        val timeSec = (System.currentTimeMillis() % 600_000L) / 1000f

        provideContent {
            val size = LocalSize.current
            val bmp = renderBitmap(context, size, config, palette, ripple) { canvas, w, h, txt ->
                // Rising liquid fill inside glass as the minute passes
                MetaballSimulation.drawLiquidFillTank(
                    canvas = canvas,
                    width = w,
                    height = h,
                    fillFraction = minuteFill,
                    timeSec = timeSec,
                    liquidColor = config.theme.tintArgb.toInt(),
                    highlightColor = config.theme.accentArgb.toInt(),
                    bubbleDensity = config.bubbleDensity,
                    viscosity = config.viscosity
                )

                if (config.clockAnalogMode) {
                    val cx = w * 0.5f
                    val cy = h * 0.5f
                    val r = minOf(w, h) * 0.36f
                    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 4f
                        color = Color.argb(170, 255, 255, 255)
                    }
                    canvas.drawCircle(cx, cy, r, ringPaint)

                    val hourAngle = ((cal.get(Calendar.HOUR) % 12 + cal.get(Calendar.MINUTE) / 60f) / 12f) * 2 * PI - PI / 2
                    val minAngle = (cal.get(Calendar.MINUTE) / 60f) * 2 * PI - PI / 2
                    val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = txt.color
                        strokeCap = Paint.Cap.ROUND
                        strokeWidth = 10f
                    }
                    canvas.drawLine(cx, cy, cx + (cos(hourAngle) * r * 0.52).toFloat(), cy + (sin(hourAngle) * r * 0.52).toFloat(), handPaint)
                    handPaint.strokeWidth = 6f
                    canvas.drawLine(cx, cy, cx + (cos(minAngle) * r * 0.78).toFloat(), cy + (sin(minAngle) * r * 0.78).toFloat(), handPaint)
                } else {
                    txt.textAlign = Paint.Align.CENTER
                    txt.textSize = (minOf(w * 0.27f, h * 0.52f) * config.fontSizeScale).coerceIn(36f, 150f)
                    canvas.drawText(timeStr, w * 0.5f, h * 0.58f, txt)

                    txt.textSize = (h * 0.135f * config.fontSizeScale).coerceIn(18f, 38f)
                    txt.alpha = 220
                    canvas.drawText(dateStr, w * 0.5f, h * 0.80f, txt)
                }

                // Floating bubble second indicator
                if (config.clockShowSeconds) {
                    val bx = w * (0.14f + 0.72f * (seconds / 59f))
                    val by = h * 0.18f + sin(timeSec * 2.2f) * 6f
                    MetaballSimulation.drawLiquidDroplet(
                        canvas = canvas,
                        cx = bx,
                        cy = by,
                        radius = (minOf(w, h) * 0.08f).coerceIn(12f, 24f),
                        tintColor = config.theme.accentArgb.toInt(),
                        label = String.format(Locale.US, "%02d", seconds)
                    )
                }
            }

            GlassBitmapSurface(
                bitmap = bmp,
                contentDescription = "Liquid Clock $timeStr",
                onTap = broadcastAction(context, WidgetActionReceiver.ACTION_OPEN_CLOCK)
            )
        }
    }
}

class LiquidClockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiquidClockWidget()
}
