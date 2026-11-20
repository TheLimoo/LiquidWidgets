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
import com.liquidwidgets.app.data.providers.CalendarDataProvider
import com.liquidwidgets.app.engine.MetaballSimulation
import com.liquidwidgets.app.receivers.WidgetActionReceiver

class LiquidCalendarWidget : BaseLiquidGlanceWidget(WidgetType.CALENDAR) {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (config, palette, ripple) = loadContextState(context)
        val snap = CalendarDataProvider.readTodayNextEvents(context)
        val timeSec = (System.currentTimeMillis() % 600_000L) / 1000f

        provideContent {
            val size = LocalSize.current
            val bmp = renderBitmap(context, size, config, palette, ripple, contentKey = "${snap.events.joinToString { it.id.toString() + it.title }}") { canvas, w, h, txt ->
                if (!snap.hasPermission) {
                    MetaballSimulation.drawLiquidFillTank(
                        canvas, w, h, 0.28f, timeSec, config.theme.tintArgb.toInt(), config.theme.accentArgb.toInt(), 0.35f, 0.35f
                    )
                    txt.textAlign = Paint.Align.CENTER
                    txt.textSize = (h * 0.14f).coerceIn(18f, 30f)
                    canvas.drawText("Grant Calendar access in Liquid Widgets", w * 0.5f, h * 0.54f, txt)
                } else if (snap.events.isEmpty()) {
                    // Empty day = calm flat water with "No events, enjoy the calm"
                    MetaballSimulation.drawLiquidFillTank(
                        canvas, w, h, 0.32f, timeSec * 0.35f, config.theme.tintArgb.toInt(), config.theme.accentArgb.toInt(), 0.25f, 0.25f
                    )
                    txt.textAlign = Paint.Align.CENTER
                    txt.textSize = (h * 0.155f * config.fontSizeScale).coerceIn(20f, 34f)
                    canvas.drawText("No events, enjoy the calm", w * 0.5f, h * 0.52f, txt)
                } else {
                    // Shrinking liquid droplet showing time until next event
                    val mins = snap.minutesUntilNext ?: 0L
                    val shrinkFactor = (mins.coerceIn(5L, 180L) / 180f)
                    val dropR = (h * (0.14f + 0.12f * shrinkFactor)).coerceIn(22f, 44f)
                    val label = if (mins >= 60) "${mins / 60}h" else "${mins}m"
                    MetaballSimulation.drawLiquidDroplet(
                        canvas = canvas,
                        cx = w * 0.14f,
                        cy = h * 0.50f,
                        radius = dropR,
                        tintColor = config.theme.accentArgb.toInt(),
                        label = label
                    )

                    // Next 3 events as floating glass pills
                    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(65, 255, 255, 255)
                    }
                    val pillStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                        color = Color.argb(145, 255, 255, 255)
                    }
                    val pillLeft = w * 0.28f
                    val pillRight = w * 0.94f
                    val rowH = (h * 0.24f)
                    val gap = h * 0.055f
                    val startY = h * 0.11f

                    snap.events.take(3).forEachIndexed { idx, ev ->
                        val top = startY + idx * (rowH + gap)
                        val rect = RectF(pillLeft, top, pillRight, top + rowH)
                        canvas.drawRoundRect(rect, rowH * 0.5f, rowH * 0.5f, pillPaint)
                        canvas.drawRoundRect(rect, rowH * 0.5f, rowH * 0.5f, pillStroke)

                        txt.textAlign = Paint.Align.LEFT
                        txt.textSize = (rowH * 0.48f).coerceIn(15f, 26f)
                        val truncated = if (ev.title.length > 22) ev.title.take(21) + "…" else ev.title
                        canvas.drawText(truncated, pillLeft + 22f, top + rowH * 0.66f, txt)

                        txt.textAlign = Paint.Align.RIGHT
                        txt.alpha = 210
                        canvas.drawText(ev.timeLabel, pillRight - 20f, top + rowH * 0.66f, txt)
                        txt.alpha = 255
                    }
                }
            }

            GlassBitmapSurface(
                bitmap = bmp,
                contentDescription = "Calendar Agenda Widget",
                onTap = broadcastAction(
                    context,
                    WidgetActionReceiver.ACTION_OPEN_CALENDAR,
                    eventId = snap.events.firstOrNull()?.eventId
                )
            )
        }
    }
}

class LiquidCalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiquidCalendarWidget()
}
