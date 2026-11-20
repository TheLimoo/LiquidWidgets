package com.liquidwidgets.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.RenderNode
import android.graphics.Typeface
import android.media.ImageReader
import android.os.Build
import com.liquidwidgets.app.data.model.LiquidFont
import com.liquidwidgets.app.data.model.LiquidGlassConfig
import com.liquidwidgets.app.data.model.LiquidShape

/**
 * Renders a complete home-screen / lock-screen widget frame using the real AGSL
 * RuntimeShader via HardwareRenderer + RenderNode (API 33+), then composites
 * widget-specific fluid layers and typography inside the glass shape.
 */
object LiquidGlassBitmapRenderer {

    fun renderGlassSurface(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        config: LiquidGlassConfig,
        palette: WallpaperPalette,
        timeSec: Float,
        tiltX: Float = 0.15f,
        tiltY: Float = -0.10f,
        rippleProgress: Float = 0f,
        customBackdrop: Bitmap? = null,
        overlayDrawer: (Canvas, Float, Float, Paint) -> Unit
    ): Bitmap {
        val w = widthPx.coerceIn(180, 1280)
        val h = heightPx.coerceIn(96, 1280)

        val backdrop = customBackdrop ?: WallpaperSampler(context).createBackdropBitmap(w, h, palette, timeSec * 0.25f)
        val baseGlass = renderAgslHardwareBitmap(
            context = context,
            backdrop = backdrop,
            width = w,
            height = h,
            config = config,
            palette = palette,
            timeSec = timeSec,
            tiltX = tiltX,
            tiltY = tiltY,
            rippleProgress = rippleProgress
        )

        val output = baseGlass.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val textPaint = createTextPaint(config, palette)

        // Draw widget-specific fluid interior & data
        overlayDrawer(canvas, w.toFloat(), h.toFloat(), textPaint)

        // Mask interior cleanly to the glass silhouette
        val mask = createShapeMask(w, h, config.shape)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(mask, 0f, 0f, maskPaint)
        mask.recycle()
        if (customBackdrop == null) backdrop.recycle()
        baseGlass.recycle()

        return output
    }

    private fun renderAgslHardwareBitmap(
        context: Context,
        backdrop: Bitmap,
        width: Int,
        height: Int,
        config: LiquidGlassConfig,
        palette: WallpaperPalette,
        timeSec: Float,
        tiltX: Float,
        tiltY: Float,
        rippleProgress: Float
    ): Bitmap {
        return runCatching {
            val imageReader = ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                1,
                android.hardware.HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or
                    android.hardware.HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
            )
            val renderer = HardwareRenderer()
            val renderNode = RenderNode("LiquidGlassWidgetNode")
            renderNode.setPosition(0, 0, width, height)

            val shader = LiquidGlassShaders.createShader(context)
            LiquidGlassShaders.bindUniforms(
                shader = shader,
                backdropBitmap = backdrop,
                width = width.toFloat(),
                height = height.toFloat(),
                timeSeconds = timeSec,
                tiltX = tiltX,
                tiltY = tiltY,
                config = config,
                wallpaperPalette = palette,
                rippleProgress = rippleProgress
            )

            val recCanvas = renderNode.beginRecording(width, height)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
            recCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            renderNode.endRecording()

            renderer.setSurface(imageReader.surface)
            renderer.setContentRoot(renderNode)
            renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()

            val image = imageReader.acquireNextImage()
            val hwBuffer = image?.hardwareBuffer
            val result = if (hwBuffer != null) {
                val hwBitmap = Bitmap.wrapHardwareBuffer(hwBuffer, null)
                val soft = hwBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                hwBitmap?.recycle()
                hwBuffer.close()
                image.close()
                soft
            } else {
                image?.close()
                null
            }
            renderer.destroy()
            imageReader.close()
            result ?: fallbackGlassBitmap(backdrop, width, height, config, palette)
        }.getOrElse {
            fallbackGlassBitmap(backdrop, width, height, config, palette)
        }
    }

    private fun fallbackGlassBitmap(
        backdrop: Bitmap,
        width: Int,
        height: Int,
        config: LiquidGlassConfig,
        palette: WallpaperPalette
    ): Bitmap {
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawBitmap(backdrop, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), null)
        val tint = config.theme.tintArgb.toInt()
        val overlay = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((config.tintOpacity * 180).toInt().coerceIn(30, 180), Color.red(tint), Color.green(tint), Color.blue(tint))
        }
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlay)
        val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.argb(180, 255, 255, 255)
        }
        val radius = cornerRadiusFor(width.toFloat(), height.toFloat(), config.shape)
        c.drawRoundRect(RectF(2f, 2f, width - 2f, height - 2f), radius, radius, rim)
        return out
    }

    private fun createShapeMask(width: Int, height: Int, shape: LiquidShape): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(mask)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val r = cornerRadiusFor(width.toFloat(), height.toFloat(), shape)
        val inset = 2f
        c.drawRoundRect(RectF(inset, inset, width - inset, height - inset), r, r, p)
        return mask
    }

    private fun cornerRadiusFor(w: Float, h: Float, shape: LiquidShape): Float {
        val minDim = minOf(w, h)
        return when (shape) {
            LiquidShape.SQUIRCLE -> minDim * 0.28f
            LiquidShape.BLOB -> minDim * 0.42f
            LiquidShape.PILL -> minDim * 0.50f
            LiquidShape.SHARP_GLASS -> minDim * 0.10f
            LiquidShape.FREEFORM -> minDim * 0.34f
        }
    }

    fun createTextPaint(config: LiquidGlassConfig, palette: WallpaperPalette): Paint {
        val baseFamily = when (config.font) {
            LiquidFont.LIQUID_SERIF, LiquidFont.GLASS_DISPLAY -> Typeface.SERIF
            LiquidFont.DROPLET_MONO -> Typeface.MONOSPACE
            else -> Typeface.SANS_SERIF
        }
        val style = if (config.fontWeight >= 600) Typeface.BOLD else Typeface.NORMAL
        val textColor = if (config.themeId == "frosted_milk") {
            Color.parseColor("#0D1E2E")
        } else if (palette.isLightWallpaper && config.themeId == "clear_water") {
            Color.parseColor("#0A2034")
        } else {
            config.theme.textArgb.toInt()
        }
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            typeface = Typeface.create(baseFamily, style)
            setShadowLayer(8f, 0f, 3f, Color.argb(120, 0, 12, 26))
        }
    }
}
