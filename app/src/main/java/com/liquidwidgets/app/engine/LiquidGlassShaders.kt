package com.liquidwidgets.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.RuntimeShader
import android.graphics.Shader
import com.liquidwidgets.app.R
import com.liquidwidgets.app.data.model.BackgroundMode
import com.liquidwidgets.app.data.model.LiquidGlassConfig

/**
 * Compiles and configures the AGSL RuntimeShader for real liquid glass refraction,
 * curved-edge distortion, chromatic aberration, gyroscope-driven specular highlights,
 * metaball lens distortion, and touch ripples.
 */
object LiquidGlassShaders {

    @Volatile
    private var cachedSource: String? = null

    fun loadSource(context: Context): String {
        cachedSource?.let { return it }
        val text = context.resources.openRawResource(R.raw.liquid_glass_refraction)
            .bufferedReader()
            .use { it.readText() }
        cachedSource = text
        return text
    }

    fun createShader(context: Context): RuntimeShader {
        return RuntimeShader(loadSource(context))
    }

    fun bindUniforms(
        shader: RuntimeShader,
        backdropBitmap: Bitmap,
        width: Float,
        height: Float,
        timeSeconds: Float,
        tiltX: Float,
        tiltY: Float,
        config: LiquidGlassConfig,
        wallpaperPalette: WallpaperPalette,
        rippleX: Float = 0.5f,
        rippleY: Float = 0.5f,
        rippleProgress: Float = 0f
    ) {
        val bmpShader = BitmapShader(backdropBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setInputShader("backdrop", bmpShader)
        shader.setFloatUniform("resolution", width.coerceAtLeast(1f), height.coerceAtLeast(1f))
        shader.setFloatUniform("time", timeSeconds)
        shader.setFloatUniform("tilt", tiltX.coerceIn(-1f, 1f), tiltY.coerceIn(-1f, 1f))
        shader.setFloatUniform("thickness", config.thickness.coerceIn(0.05f, 1f))
        shader.setFloatUniform("refractionStrength", config.refractionStrength.coerceIn(0.05f, 1.2f))
        shader.setFloatUniform("blurAmount", config.blur.coerceIn(0f, 1f))
        shader.setFloatUniform(
            "tintOpacity",
            when (config.backgroundMode) {
                BackgroundMode.TRANSPARENT -> (config.tintOpacity * 0.35f).coerceIn(0.04f, 0.25f)
                BackgroundMode.SOLID_TINT -> (config.tintOpacity + 0.35f).coerceIn(0.35f, 0.78f)
                BackgroundMode.SAMPLE_WALLPAPER -> config.tintOpacity.coerceIn(0.08f, 0.65f)
            }
        )
        shader.setFloatUniform("edgeGlow", config.edgeGlow.coerceIn(0f, 1.2f))
        shader.setFloatUniform("chromaticAberration", config.chromaticAberration.coerceIn(0f, 1.2f))
        shader.setFloatUniform("viscosity", config.viscosity.coerceIn(0.1f, 1.5f))
        shader.setFloatUniform("bubbleDensity", config.bubbleDensity.coerceIn(0f, 1.2f))
        shader.setFloatUniform("shapeType", config.shape.shaderIndex)

        val resolvedTint = resolveTintColor(config, wallpaperPalette)
        val resolvedAccent = resolveAccentColor(config, wallpaperPalette)

        shader.setFloatUniform(
            "tintColor",
            Color.red(resolvedTint) / 255f,
            Color.green(resolvedTint) / 255f,
            Color.blue(resolvedTint) / 255f,
            1f
        )
        shader.setFloatUniform(
            "accentColor",
            Color.red(resolvedAccent) / 255f,
            Color.green(resolvedAccent) / 255f,
            Color.blue(resolvedAccent) / 255f,
            1f
        )
        shader.setFloatUniform("rippleCenter", rippleX.coerceIn(0f, 1f), rippleY.coerceIn(0f, 1f))
        shader.setFloatUniform("rippleProgress", rippleProgress.coerceIn(0f, 1f))
    }

    private fun resolveTintColor(config: LiquidGlassConfig, palette: WallpaperPalette): Int {
        val themeColor = config.theme.tintArgb.toInt()
        if (config.backgroundMode != BackgroundMode.SAMPLE_WALLPAPER) return themeColor
        // Blend theme tint with live wallpaper adaptive glass color
        return blendArgb(themeColor, palette.adaptiveGlassTint, 0.45f)
    }

    private fun resolveAccentColor(config: LiquidGlassConfig, palette: WallpaperPalette): Int {
        val themeAccent = config.theme.accentArgb.toInt()
        if (config.backgroundMode != BackgroundMode.SAMPLE_WALLPAPER) return themeAccent
        return blendArgb(themeAccent, palette.accentColor, 0.40f)
    }

    fun blendArgb(c1: Int, c2: Int, ratio: Float): Int {
        val inv = 1f - ratio
        val r = (Color.red(c1) * inv + Color.red(c2) * ratio).toInt().coerceIn(0, 255)
        val g = (Color.green(c1) * inv + Color.green(c2) * ratio).toInt().coerceIn(0, 255)
        val b = (Color.blue(c1) * inv + Color.blue(c2) * ratio).toInt().coerceIn(0, 255)
        return Color.argb(255, r, g, b)
    }
}
