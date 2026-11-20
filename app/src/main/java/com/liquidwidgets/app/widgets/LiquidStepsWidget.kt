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
import com.liquidwidgets.app.data.providers.StepsDataProvider
import com.liquidwidgets.app.engine.MetaballSimulation
import com.liquidwidgets.app.receivers.WidgetActionReceiver
import java.util.Locale
import kotlin.math.sin

class LiquidStepsWidget : BaseLiquidGlanceWidget(WidgetType.STEPS) {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (config, palette, ripple) = loadContextState()
        val snap = StepsDataProvider.read(context, config.dailyStepGoal)
        val fraction = (snap.stepsToday.toFloat() / snap.goal).coerceIn(0.05f, 1f)
        val timeSec = (System.currentTimeMillis() % 600_000L) / 1000f

        provideContent {
            val size = LocalSize.current
            val bmp = renderBitmap(context, size, config, palette, ripple) { canvas, w, h, txt ->
                MetaballSimulation.drawLiquidFillTank(
                    canvas = canvas,
                    width = w,
                    height = h,
                    fillFraction = fraction,
                    timeSec = timeSec,
                    liquidColor = Color.parseColor("#20E3B2"),
                    highlightColor = Color.parseColor("#B2FFF0"),
                    bubbleDensity = config.bubbleDensity,
                    viscosity = config.viscosity
                )

                val cx = w * 0.5f
                val cy = h * 0.45f
                val r = minOf(w, h) * 0.30f

                val ringTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 10f
                    color = Color.argb(70, 255, 255, 255)
                }
                canvas.drawCircle(cx, cy, r, ringTrack)

                val ringFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeWidth = 10f
                    color = Color.parseColor("#A8FFE6")
                }
                canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, fraction * 360f, false, ringFill)

                // Bobbing walking droplet
                val bobY = h * 0.16f + sin(timeSec * 3.2f) * 5f
                MetaballSimulation.drawLiquidDroplet(canvas, w * 0.80f, bobY, 15f, Color.parseColor("#6FFFE9"))

                txt.textAlign = Paint.Align.CENTER
                txt.textSize = (minOf(w, h) * 0.18f * config.fontSizeScale).coerceIn(22f, 52f)
                canvas.drawText(String.format(Locale.US, "%,d", snap.stepsToday), cx, cy + txt.textSize * 0.32f, txt)

                txt.textSize = (minOf(w, h) * 0.088f).coerceIn(13f, 22f)
                txt.alpha = 225
                canvas.drawText(
                    String.format(Locale.US, "%.1f km • %d kcal", snap.distanceKm, snap.caloriesKcal),
                    cx,
                    h * 0.86f,
                    txt
                )
            }

            GlassBitmapSurface(
                bitmap = bmp,
                contentDescription = "Steps ${snap.stepsToday}",
                onTap = broadcastAction(context, WidgetActionReceiver.ACTION_OPEN_STEPS)
            )
        }
    }
}

class LiquidStepsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiquidStepsWidget()
}
