package com.liquidwidgets.app.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import com.liquidwidgets.app.data.model.LiquidGlassConfig
import com.liquidwidgets.app.engine.GyroscopeTiltProvider
import com.liquidwidgets.app.engine.LiquidGlassShaders
import com.liquidwidgets.app.engine.TiltVector
import com.liquidwidgets.app.engine.WallpaperPalette
import com.liquidwidgets.app.engine.WallpaperSampler
import kotlinx.coroutines.launch

data class SampleWallpaper(
    val id: String,
    val name: String,
    val palette: WallpaperPalette
)

object SampleWallpapers {
    val ALL = listOf(
        SampleWallpaper(
            "deep_ocean",
            "Deep Ocean",
            WallpaperPalette(
                primaryColor = AndroidColor.parseColor("#04162A"),
                secondaryColor = AndroidColor.parseColor("#0F5A8A"),
                accentColor = AndroidColor.parseColor("#4BF2FF"),
                isLightWallpaper = false
            )
        ),
        SampleWallpaper(
            "light_sky",
            "Morning Sky",
            WallpaperPalette(
                primaryColor = AndroidColor.parseColor("#CBEFFF"),
                secondaryColor = AndroidColor.parseColor("#78D0F7"),
                accentColor = AndroidColor.parseColor("#FFFFFF"),
                isLightWallpaper = true
            )
        ),
        SampleWallpaper(
            "sunset",
            "Golden Sunset",
            WallpaperPalette(
                primaryColor = AndroidColor.parseColor("#2B1136"),
                secondaryColor = AndroidColor.parseColor("#C94A5A"),
                accentColor = AndroidColor.parseColor("#FFC46B"),
                isLightWallpaper = false
            )
        ),
        SampleWallpaper(
            "neon",
            "Neon Cyber",
            WallpaperPalette(
                primaryColor = AndroidColor.parseColor("#090418"),
                secondaryColor = AndroidColor.parseColor("#6A1B9A"),
                accentColor = AndroidColor.parseColor("#00F5FF"),
                isLightWallpaper = false
            )
        ),
        SampleWallpaper(
            "forest",
            "Emerald Mist",
            WallpaperPalette(
                primaryColor = AndroidColor.parseColor("#06211B"),
                secondaryColor = AndroidColor.parseColor("#136F5A"),
                accentColor = AndroidColor.parseColor("#7FFFD4"),
                isLightWallpaper = false
            )
        ),
        SampleWallpaper(
            "minimal_light",
            "Pearl Light",
            WallpaperPalette(
                primaryColor = AndroidColor.parseColor("#EEF4F8"),
                secondaryColor = AndroidColor.parseColor("#C6DCEB"),
                accentColor = AndroidColor.parseColor("#6BD7FF"),
                isLightWallpaper = true
            )
        )
    )
}

/**
 * Full 60fps real-time AGSL RuntimeShader liquid glass container for the companion app
 * and Live Editor. Supports true edge refraction, chromatic aberration, gyroscope speculars,
 * metaball drift, and interactive touch ripple waves.
 */
@Composable
fun LiquidGlassSurface(
    config: LiquidGlassConfig,
    wallpaperPalette: WallpaperPalette,
    modifier: Modifier = Modifier,
    enableGyroscope: Boolean = true,
    onTap: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shader = remember(context) { LiquidGlassShaders.createShader(context) }
    val tiltFlow = remember(context, enableGyroscope) {
        GyroscopeTiltProvider(context).observeTilt(enableGyroscope)
    }
    val tilt by tiltFlow.collectAsState(initial = TiltVector())

    var boxSize by remember { mutableStateOf(IntSize(400, 220)) }
    var timeSec by remember { mutableFloatStateOf(0f) }
    var rippleCenter by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    val rippleProgress = remember { Animatable(0f) }

    // 60fps animation clock driven by Compose frame loop
    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameTime ->
                timeSec = ((frameTime - startNanos) / 1_000_000_000f)
            }
        }
    }

    val backdropBitmap = remember(boxSize, wallpaperPalette) {
        WallpaperSampler(context).createBackdropBitmap(
            width = boxSize.width.coerceIn(128, 720),
            height = boxSize.height.coerceIn(128, 720),
            palette = wallpaperPalette
        )
    }

    Box(
        modifier = modifier
            .onSizeChanged { if (it.width > 0 && it.height > 0) boxSize = it }
            .pointerInput(onTap) {
                detectTapGestures { offset ->
                    val nx = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    val ny = (offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                    rippleCenter = Offset(nx, ny)
                    scope.launch {
                        rippleProgress.snapTo(0.01f)
                        rippleProgress.animateTo(1f, animationSpec = tween(durationMillis = 650))
                        rippleProgress.snapTo(0f)
                    }
                    onTap?.invoke()
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            LiquidGlassShaders.bindUniforms(
                shader = shader,
                backdropBitmap = backdropBitmap,
                width = size.width,
                height = size.height,
                timeSeconds = timeSec,
                tiltX = tilt.x,
                tiltY = tilt.y,
                config = config,
                wallpaperPalette = wallpaperPalette,
                rippleX = rippleCenter.x,
                rippleY = rippleCenter.y,
                rippleProgress = rippleProgress.value
            )
            drawRect(brush = ShaderBrush(shader))
        }
        content()
    }
}
