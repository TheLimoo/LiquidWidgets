package com.liquidwidgets.app.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.data.providers.SystemStatsDataProvider
import com.liquidwidgets.app.receivers.WidgetActionReceiver
import kotlin.math.sin

class LiquidSystemWidget : BaseLiquidGlanceWidget(WidgetType.SYSTEM) {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (config, palette, ripple) = loadContextState(context)
        val snap = SystemStatsDataProvider.read(context)
        val timeSec = (System.currentTimeMillis() % 600_000L) / 1000f

        provideContent {
            val size = LocalSize.current
            val bmp = renderBitmap(context, size, config, palette, ripple, contentKey = "${snap.cpuPercent}|${snap.ramPercent}|${snap.storagePercent}") { canvas, w, h, txt ->
                // CPU is null when /proc/stat is blocked: show Unavailable, never a fake %.
                val gauges = listOf(
                    Triple("CPU", snap.cpuPercent ?: -1, Color.parseColor("#38E8FF")),
                    Triple("RAM", snap.ramPercent, Color.parseColor("#B388FF")),
                    Triple("DISK", snap.storagePercent, Color.parseColor("#52FFA8"))
                )

                val tubeW = w * 0.21f
                val tubeH = h * 0.56f
                val topY = h * 0.16f
                val spacing = (w - tubeW * 3) / 4f

                val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(55, 255, 255, 255)
                }
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                    color = Color.argb(165, 255, 255, 255)
                }

                gauges.forEachIndexed { idx, (label, pct, color) ->
                    val left = spacing + idx * (tubeW + spacing)
                    val rect = RectF(left, topY, left + tubeW, topY + tubeH)
                    canvas.drawRoundRect(rect, tubeW * 0.45f, tubeW * 0.45f, trackPaint)

                    val slosh = sin(timeSec * (2.2f + pct * 0.03f) + idx * 1.4f) * (pct * 0.06f)
                    val fillH = (tubeH * (pct / 100f) + slosh).coerceIn(tubeW * 0.4f, tubeH)
                    val fillRect = RectF(left + 3f, topY + tubeH - fillH, left + tubeW - 3f, topY + tubeH - 3f)
                    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.color = Color.argb(205, Color.red(color), Color.green(color), Color.blue(color))
                    }
                    canvas.drawRoundRect(fillRect, tubeW * 0.4f, tubeW * 0.4f, fillPaint)
                    canvas.drawRoundRect(rect, tubeW * 0.45f, tubeW * 0.45f, strokePaint)

                    txt.textAlign = Paint.Align.CENTER
                    txt.textSize = (h * 0.105f).coerceIn(14f, 24f)
                    canvas.drawText(if (pct < 0) "—" else "$pct%", left + tubeW * 0.5f, topY + tubeH + h * 0.12f, txt)
                    txt.alpha = 200
                    txt.textSize = (h * 0.085f).coerceIn(12f, 19f)
                    canvas.drawText(label, left + tubeW * 0.5f, topY + tubeH + h * 0.22f, txt)
                    txt.alpha = 255
                }
            }

            GlassBitmapSurface(
                bitmap = bmp,
                contentDescription = "System CPU ${snap.cpuPercent?.let { "$it%" } ?: "unavailable"} RAM ${snap.ramPercent}% Storage ${snap.storagePercent}%",
                onTap = broadcastAction(context, WidgetActionReceiver.ACTION_OPEN_SYSTEM)
            )
        }
    }
}

class LiquidSystemWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiquidSystemWidget()
}
