package com.liquidwidgets.app.engine

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin

data class WallpaperPalette(
    val primaryColor: Int = Color.parseColor("#0B2A4A"),
    val secondaryColor: Int = Color.parseColor("#1878A8"),
    val accentColor: Int = Color.parseColor("#6CF2FF"),
    val isLightWallpaper: Boolean = false
) {
    /**
     * Light wallpaper => bright aqua glass; Dark wallpaper => deep blue/black glass.
     */
    val adaptiveGlassTint: Int
        get() = if (isLightWallpaper) {
            Color.parseColor("#5CEBFF")
        } else {
            Color.parseColor("#0A2542")
        }

    val adaptiveTextColor: Int
        get() = if (isLightWallpaper) Color.parseColor("#081B2C") else Color.WHITE
}

/**
 * Samples live Android wallpaper colors (and updates whenever the user changes wallpaper).
 * Builds a high-contrast refractive backdrop bitmap matching the live wallpaper palette
 * so AGSL refraction, edge bending, and chromatic aberration are always vivid even when
 * scoped storage prevents reading the raw wallpaper image directly.
 */
class WallpaperSampler(private val context: Context) {

    private val _palette = MutableStateFlow(WallpaperPalette())
    val palette: StateFlow<WallpaperPalette> = _palette.asStateFlow()

    fun refreshFromWallpaper(): WallpaperPalette {
        val wm = WallpaperManager.getInstance(context)
        val colors: WallpaperColors? = runCatching {
            wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        }.getOrNull()

        val newPalette = if (colors != null) {
            val primary = colors.primaryColor.toArgb()
            val secondary = colors.secondaryColor?.toArgb() ?: shiftLuminance(primary, 1.25f)
            val tertiary = colors.tertiaryColor?.toArgb() ?: shiftLuminance(secondary, 1.35f)
            val luminance = Color.luminance(primary)
            val isLight = (colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0 ||
                luminance > 0.52f
            WallpaperPalette(
                primaryColor = primary,
                secondaryColor = secondary,
                accentColor = tertiary,
                isLightWallpaper = isLight
            )
        } else {
            WallpaperPalette()
        }
        _palette.value = newPalette
        return newPalette
    }

    /**
     * Generates a rich optical backdrop bitmap derived from the live wallpaper palette
     * (or a preview wallpaper in the in-app Editor) for AGSL refraction sampling.
     */
    fun createBackdropBitmap(
        width: Int,
        height: Int,
        palette: WallpaperPalette = _palette.value,
        seedPhase: Float = 0f
    ): Bitmap {
        val w = width.coerceAtLeast(64)
        val h = height.coerceAtLeast(64)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(palette.primaryColor, palette.secondaryColor, palette.primaryColor),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Luminous wallpaper orbs so curved-edge refraction bends visible shapes
        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val orb1X = w * (0.25f + 0.08f * sin(seedPhase))
        val orb1Y = h * (0.30f + 0.08f * cos(seedPhase))
        orbPaint.shader = RadialGradient(
            orb1X, orb1Y, w * 0.45f,
            intArrayOf(palette.accentColor, Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(orb1X, orb1Y, w * 0.45f, orbPaint)

        val orb2X = w * (0.78f - 0.06f * cos(seedPhase * 0.8f))
        val orb2Y = h * (0.72f + 0.06f * sin(seedPhase * 0.8f))
        orbPaint.shader = RadialGradient(
            orb2X, orb2Y, w * 0.40f,
            intArrayOf(shiftLuminance(palette.secondaryColor, 1.4f), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(orb2X, orb2Y, w * 0.40f, orbPaint)

        // Subtle high-frequency diagonal light bands to make edge refraction unmistakable
        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(if (palette.isLightWallpaper) 42 else 34, 255, 255, 255)
            strokeWidth = (w * 0.025f).coerceAtLeast(3f)
        }
        for (i in -2..6) {
            val offset = i * (w * 0.22f)
            canvas.drawLine(offset, 0f, offset + h * 0.6f, h.toFloat(), stripePaint)
        }
        return bmp
    }

    private fun shiftLuminance(argb: Int, factor: Float): Int {
        val r = (Color.red(argb) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(argb) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(argb) * factor).toInt().coerceIn(0, 255)
        return Color.argb(255, r, g, b)
    }
}
