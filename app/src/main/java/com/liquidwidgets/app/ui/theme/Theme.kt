package com.liquidwidgets.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LiquidDarkScheme = darkColorScheme(
    primary = Color(0xFF4BE8FF),
    onPrimary = Color(0xFF002430),
    primaryContainer = Color(0xFF093A52),
    onPrimaryContainer = Color(0xFFBDF6FF),
    secondary = Color(0xFF7AD7F0),
    onSecondary = Color(0xFF002533),
    background = Color(0xFF050D18),
    onBackground = Color(0xFFEAF6FF),
    surface = Color(0xFF091626),
    onSurface = Color(0xFFEAF6FF),
    surfaceVariant = Color(0xFF122740),
    onSurfaceVariant = Color(0xFFAAC8DF)
)

private val LiquidLightScheme = lightColorScheme(
    primary = Color(0xFF006780),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8EAFF),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF2E6478),
    background = Color(0xFFF0F8FC),
    onBackground = Color(0xFF081B29),
    surface = Color(0xFFF5FAFD),
    onSurface = Color(0xFF081B29),
    surfaceVariant = Color(0xFFDCEBF4),
    onSurfaceVariant = Color(0xFF395060)
)

@Composable
fun LiquidWidgetsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LiquidDarkScheme
        else -> LiquidLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
