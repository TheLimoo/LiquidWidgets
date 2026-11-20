package com.liquidwidgets.app.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import com.liquidwidgets.app.liquidApp
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.engine.MetaballSimulation
import com.liquidwidgets.app.receivers.WidgetActionReceiver
import java.util.Locale

class LiquidFocusNotesWidget : BaseLiquidGlanceWidget(WidgetType.FOCUS_NOTES) {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (config, palette, ripple) = loadContextState(context)
        val endMs = context.liquidApp().configStore.getFocusEndEpochMs()
        val now = System.currentTimeMillis()
        val isRunning = endMs > now
        val totalMs = (config.focusDurationMinutes * 60_000L).coerceAtLeast(60_000L)
        val remainingMs = if (isRunning) (endMs - now) else totalMs
        val fillFrac = (remainingMs.toFloat() / totalMs).coerceIn(0.05f, 1f)
        val timeSec = (now % 600_000L) / 1000f

        provideContent {
            val size = LocalSize.current
            val bmp = renderBitmap(context, size, config, palette, ripple, contentKey = "${if (isRunning) remainingMs / 60_000L else -1}|${config.focusNoteMode}|${config.quickNoteText.hashCode()}") { canvas, w, h, txt ->
                if (config.focusNoteMode) {
                    // Floating ink note inside clear liquid glass
                    MetaballSimulation.drawRisingBubbles(canvas, w, h, h * 0.1f, timeSec, 0.35f, 0.5f, config.theme.accentArgb.toInt())
                    txt.textAlign = Paint.Align.LEFT
                    txt.textSize = (h * 0.10f).coerceIn(14f, 22f)
                    txt.alpha = 200
                    canvas.drawText("GLASS NOTE", w * 0.10f, h * 0.22f, txt)
                    txt.alpha = 255
                    txt.textSize = (h * 0.125f * config.fontSizeScale).coerceIn(16f, 28f)
                    val words = config.quickNoteText.split(" ")
                    var line = ""
                    var y = h * 0.42f
                    val maxWidth = w * 0.80f
                    for (word in words) {
                        val candidate = if (line.isEmpty()) word else "$line $word"
                        if (txt.measureText(candidate) > maxWidth) {
                            canvas.drawText(line, w * 0.10f, y, txt)
                            line = word
                            y += txt.textSize * 1.32f
                            if (y > h * 0.88f) break
                        } else {
                            line = candidate
                        }
                    }
                    if (line.isNotEmpty() && y <= h * 0.90f) {
                        canvas.drawText(line, w * 0.10f, y, txt)
                    }
                } else {
                    // Draining Pomodoro droplet timer
                    MetaballSimulation.drawLiquidFillTank(
                        canvas = canvas,
                        width = w,
                        height = h,
                        fillFraction = fillFrac,
                        timeSec = timeSec,
                        liquidColor = if (isRunning) Color.parseColor("#FF6B6B") else config.theme.tintArgb.toInt(),
                        highlightColor = Color.WHITE,
                        bubbleDensity = config.bubbleDensity,
                        viscosity = config.viscosity,
                        chargingBoost = isRunning
                    )

                    val mins = (remainingMs / 60_000L).toInt()
                    val secs = ((remainingMs % 60_000L) / 1000L).toInt()
                    txt.textAlign = Paint.Align.CENTER
                    txt.textSize = (minOf(w, h) * 0.24f * config.fontSizeScale).coerceIn(28f, 68f)
                    canvas.drawText(String.format(Locale.US, "%02d:%02d", mins, secs), w * 0.5f, h * 0.54f, txt)

                    txt.textSize = (minOf(w, h) * 0.095f).coerceIn(14f, 22f)
                    txt.alpha = 220
                    canvas.drawText(
                        if (isRunning) "Focusing • Tap to Pause" else "Pomodoro • Tap to Start",
                        w * 0.5f,
                        h * 0.78f,
                        txt
                    )
                }
            }

            GlassBitmapSurface(
                bitmap = bmp,
                contentDescription = if (config.focusNoteMode) "Quick Glass Note" else "Focus Pomodoro Timer",
                onTap = broadcastAction(
                    context,
                    if (config.focusNoteMode) WidgetActionReceiver.ACTION_OPEN_NOTES
                    else WidgetActionReceiver.ACTION_FOCUS_TOGGLE
                )
            )
        }
    }
}

class LiquidFocusNotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiquidFocusNotesWidget()
}
