package com.liquidwidgets.app.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import com.liquidwidgets.app.LiquidWidgetsApp
import com.liquidwidgets.app.data.model.LiquidGlassConfig
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.engine.LiquidGlassBitmapRenderer
import com.liquidwidgets.app.engine.WallpaperPalette
import com.liquidwidgets.app.receivers.WidgetActionReceiver

abstract class BaseLiquidGlanceWidget(
    protected val widgetType: WidgetType
) : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    protected fun rippleProgressFor(lastRippleMs: Long): Float {
        if (lastRippleMs <= 0L) return 0f
        val elapsed = System.currentTimeMillis() - lastRippleMs
        return if (elapsed in 1..600L) (elapsed / 600f) else 0f
    }

    protected fun dpSizeToPx(context: Context, size: DpSize): Pair<Int, Int> {
        val density = context.resources.displayMetrics.density
        val w = (size.width.value * density).toInt().coerceIn(220, 1080)
        val h = (size.height.value * density).toInt().coerceIn(110, 1080)
        return w to h
    }

    protected fun broadcastAction(
        context: Context,
        action: String,
        packageName: String? = null,
        eventId: Long? = null
    ): Action = actionSendBroadcast(
        WidgetActionReceiver.intentFor(context, action, widgetType, packageName, eventId)
    )

    protected suspend fun loadContextState(): Triple<LiquidGlassConfig, WallpaperPalette, Float> {
        val app = LiquidWidgetsApp.instance
        val cfg = app.configStore.getConfig(widgetType)
        val palette = app.wallpaperSampler.palette.value
        val ripple = rippleProgressFor(app.configStore.getLastRippleMs(widgetType))
        return Triple(cfg, palette, ripple)
    }

    protected fun renderBitmap(
        context: Context,
        size: DpSize,
        config: LiquidGlassConfig,
        palette: WallpaperPalette,
        rippleProgress: Float,
        customBackdrop: Bitmap? = null,
        drawer: (Canvas, Float, Float, Paint) -> Unit
    ): Bitmap {
        val (w, h) = dpSizeToPx(context, size)
        val timeSec = (System.currentTimeMillis() % 600_000L) / 1000f
        return LiquidGlassBitmapRenderer.renderGlassSurface(
            context = context,
            widthPx = w,
            heightPx = h,
            config = config,
            palette = palette,
            timeSec = timeSec,
            rippleProgress = rippleProgress,
            customBackdrop = customBackdrop,
            overlayDrawer = drawer
        )
    }

    @Composable
    protected fun GlassBitmapSurface(
        bitmap: Bitmap,
        contentDescription: String,
        onTap: Action,
        overlayContent: (@Composable () -> Unit)? = null
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(onTap)
        ) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = contentDescription,
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.fillMaxSize()
            )
            overlayContent?.invoke()
        }
    }
}
