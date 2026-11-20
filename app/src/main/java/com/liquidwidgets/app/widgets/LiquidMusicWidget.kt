package com.liquidwidgets.app.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.data.providers.MusicDataProvider
import com.liquidwidgets.app.engine.MetaballSimulation
import com.liquidwidgets.app.receivers.WidgetActionReceiver
import kotlin.math.sin

class LiquidMusicWidget : BaseLiquidGlanceWidget(WidgetType.MUSIC) {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (config, palette, ripple) = loadContextState()
        val snap = MusicDataProvider.read(context)
        val timeSec = (System.currentTimeMillis() % 600_000L) / 1000f

        provideContent {
            val size = LocalSize.current
            val bmp = renderBitmap(
                context = context,
                size = size,
                config = config,
                palette = palette,
                rippleProgress = ripple,
                customBackdrop = snap.albumArt
            ) { canvas, w, h, txt ->
                // Live liquid audio visualizer wave inside the glass
                val wavePath = Path()
                val baseY = h * 0.72f
                val amp = if (snap.isPlaying) h * 0.12f else h * 0.025f
                wavePath.moveTo(0f, h)
                for (i in 0..36) {
                    val x = w * i / 36f
                    val y = baseY + sin(i * 0.65f + timeSec * (if (snap.isPlaying) 5.5f else 1.2f)) * amp *
                        (0.45f + 0.55f * sin(i * 0.22f + timeSec))
                    wavePath.lineTo(x, y)
                }
                wavePath.lineTo(w, h)
                wavePath.close()

                val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(120, Color.red(config.theme.accentArgb.toInt()), Color.green(config.theme.accentArgb.toInt()), Color.blue(config.theme.accentArgb.toInt()))
                }
                canvas.drawPath(wavePath, wavePaint)

                // Song title & artist
                txt.textAlign = Paint.Align.LEFT
                txt.textSize = (h * 0.18f * config.fontSizeScale).coerceIn(20f, 38f)
                val title = if (snap.title.length > 22) snap.title.take(21) + "…" else snap.title
                canvas.drawText(title, w * 0.08f, h * 0.36f, txt)

                txt.textSize = (h * 0.12f).coerceIn(15f, 26f)
                txt.alpha = 215
                canvas.drawText(snap.artist.take(28), w * 0.08f, h * 0.54f, txt)

                // Play/Pause & Next glass buttons
                val btnR = (h * 0.18f).coerceIn(24f, 40f)
                MetaballSimulation.drawLiquidDroplet(
                    canvas = canvas,
                    cx = w * 0.72f,
                    cy = h * 0.50f,
                    radius = btnR,
                    tintColor = config.theme.accentArgb.toInt(),
                    label = if (snap.isPlaying) "❚❚" else "▶"
                )
                MetaballSimulation.drawLiquidDroplet(
                    canvas = canvas,
                    cx = w * 0.88f,
                    cy = h * 0.50f,
                    radius = btnR * 0.85f,
                    tintColor = config.theme.tintArgb.toInt(),
                    label = "⏭"
                )
            }

            GlassBitmapSurface(
                bitmap = bmp,
                contentDescription = "Liquid Music Player ${snap.title}",
                onTap = broadcastAction(context, WidgetActionReceiver.ACTION_MUSIC_OPEN)
            ) {
                Row(modifier = GlanceModifier.fillMaxSize()) {
                    // Left 60% taps open music app
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .clickable(broadcastAction(context, WidgetActionReceiver.ACTION_MUSIC_OPEN))
                    ) {}
                    // Middle-right = Play/Pause
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .clickable(broadcastAction(context, WidgetActionReceiver.ACTION_MUSIC_PLAY_PAUSE))
                    ) {}
                    // Far-right = Next track
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .clickable(broadcastAction(context, WidgetActionReceiver.ACTION_MUSIC_NEXT))
                    ) {}
                }
            }
        }
    }
}

class LiquidMusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiquidMusicWidget()
}
