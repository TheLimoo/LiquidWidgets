package com.liquidwidgets.app.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.data.providers.ScreenTimeDataProvider
import com.liquidwidgets.app.engine.MetaballSimulation
import com.liquidwidgets.app.receivers.WidgetActionReceiver

class LiquidScreenTimeWidget : BaseLiquidGlanceWidget(WidgetType.SCREEN_TIME) {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (config, palette, ripple) = loadContextState(context)
        val snap = ScreenTimeDataProvider.read(context)
        val timeSec = (System.currentTimeMillis() % 600_000L) / 1000f

        provideContent {
            val size = LocalSize.current
            val bmp = renderBitmap(context, size, config, palette, ripple, contentKey = "${snap.totalMinutesToday}|${snap.topApps.joinToString { it.packageName + it.minutes }}") { canvas, w, h, txt ->
                MetaballSimulation.drawRisingBubbles(
                    canvas, w, h, h * 0.15f, timeSec, config.bubbleDensity * 0.6f, 0.8f, config.theme.accentArgb.toInt()
                )

                val cx = w * 0.24f
                val cy = h * 0.50f
                val r = minOf(w * 0.18f, h * 0.36f)

                val bgRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = r * 0.22f
                    color = Color.argb(75, 255, 255, 255)
                }
                canvas.drawCircle(cx, cy, r, bgRing)

                val sweep = ((snap.totalMinutesToday / 360f).coerceIn(0.05f, 1f)) * 360f
                val fgRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeWidth = r * 0.22f
                    color = config.theme.accentArgb.toInt()
                }
                canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, sweep, false, fgRing)

                val hours = snap.totalMinutesToday / 60
                val mins = snap.totalMinutesToday % 60
                val totalStr = if (snap.hasPermission) "${hours}h ${mins}m" else "Grant"
                txt.textAlign = Paint.Align.CENTER
                txt.textSize = (r * 0.44f).coerceIn(18f, 40f)
                canvas.drawText(totalStr, cx, cy + txt.textSize * 0.35f, txt)

                // Unlocks & pickups subtitle
                txt.textAlign = Paint.Align.LEFT
                txt.textSize = (h * 0.105f).coerceIn(14f, 24f)
                val info = if (snap.hasPermission) {
                    "Unlocks: ${snap.unlocksToday}  •  Pickups: ${snap.pickupsToday}"
                } else {
                    "Tap to enable on-device Usage Access"
                }
                canvas.drawText(info, w * 0.45f, h * 0.24f, txt)

                // Top 3 apps as 3 colored droplets sized by usage
                val dropColors = intArrayOf(
                    Color.parseColor("#38E8FF"),
                    Color.parseColor("#FF79C6"),
                    Color.parseColor("#FFD166")
                )
                val maxMin = snap.topApps.maxOfOrNull { it.minutes }?.coerceAtLeast(1L) ?: 1L
                snap.topApps.forEachIndexed { idx, app ->
                    val ratio = (app.minutes.toFloat() / maxMin).coerceIn(0.55f, 1f)
                    val dropR = (h * 0.22f * ratio).coerceIn(22f, 46f)
                    val dx = w * (0.52f + idx * 0.175f)
                    val dy = h * 0.60f
                    MetaballSimulation.drawLiquidDroplet(
                        canvas = canvas,
                        cx = dx,
                        cy = dy,
                        radius = dropR,
                        tintColor = dropColors[idx % dropColors.size],
                        label = app.appLabel.take(4)
                    )
                    txt.textAlign = Paint.Align.CENTER
                    txt.textSize = (h * 0.09f).coerceIn(12f, 20f)
                    canvas.drawText("${app.minutes}m", dx, dy + dropR + 20f, txt)
                }
            }

            GlassBitmapSurface(
                bitmap = bmp,
                contentDescription = "Screen Time Widget",
                onTap = broadcastAction(context, WidgetActionReceiver.ACTION_LAUNCH_PACKAGE, packageName = null)
            ) {
                // Invisible tap targets over the 3 top-app droplets so tapping a droplet opens that app
                if (snap.topApps.isNotEmpty()) {
                    Row(modifier = GlanceModifier.fillMaxSize()) {
                        Spacer(modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                        snap.topApps.forEach { app ->
                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxHeight()
                                    .clickable(
                                        broadcastAction(
                                            context,
                                            WidgetActionReceiver.ACTION_LAUNCH_PACKAGE,
                                            packageName = app.packageName
                                        )
                                    )
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

class LiquidScreenTimeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiquidScreenTimeWidget()
}
