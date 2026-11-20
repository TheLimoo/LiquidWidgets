package com.liquidwidgets.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liquidwidgets.app.liquidApp
import com.liquidwidgets.app.data.model.BackgroundMode
import com.liquidwidgets.app.data.model.LiquidFont
import com.liquidwidgets.app.data.model.LiquidGlassConfig
import com.liquidwidgets.app.data.model.LiquidShape
import com.liquidwidgets.app.data.model.LiquidThemes
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.ui.components.LiveLiquidWidgetPreview
import com.liquidwidgets.app.ui.components.SampleWallpapers
import com.liquidwidgets.app.workers.BatteryAwareScheduler
import kotlinx.coroutines.launch

@Composable
fun LiveEditorScreen(
    initialWidgetType: WidgetType,
    onSaved: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = LocalContext.current.liquidApp()
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(initialWidgetType) }
    var config by remember { mutableStateOf(LiquidGlassConfig()) }
    var selectedWallpaper by remember { mutableStateOf(SampleWallpapers.ALL.first()) }

    LaunchedEffect(selectedType) {
        config = app.configStore.getConfig(selectedType)
    }

    fun updateAndPersist(next: LiquidGlassConfig) {
        config = next
        scope.launch {
            app.configStore.saveConfig(selectedType, next)
            BatteryAwareScheduler.updateWidgetType(context, selectedType)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp)
    ) {
        // Fake Android 17 Home Screen Live 60fps Preview Stage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(selectedWallpaper.palette.primaryColor),
                            Color(selectedWallpaper.palette.secondaryColor),
                            Color(selectedWallpaper.palette.accentColor).copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Live 60fps Preview • Tilt phone & tap glass",
                    color = if (selectedWallpaper.palette.isLightWallpaper) Color(0xFF0A2034) else Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(12.dp))
                LiveLiquidWidgetPreview(
                    widgetType = selectedType,
                    config = config,
                    wallpaperPalette = selectedWallpaper.palette,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (selectedType == WidgetType.QUICK_PILL) 88.dp else 184.dp)
                )
            }
        }

        // Preview Wallpaper Switcher
        SectionLabel("Preview Wallpaper")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SampleWallpapers.ALL) { wp ->
                FilterChip(
                    selected = selectedWallpaper.id == wp.id,
                    onClick = { selectedWallpaper = wp },
                    label = { Text(wp.name) }
                )
            }
        }

        // Widget Switcher
        SectionLabel("Widget")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(WidgetType.entries) { wt ->
                FilterChip(
                    selected = selectedType == wt,
                    onClick = { selectedType = wt },
                    label = { Text(wt.title) }
                )
            }
        }

        // 12 Liquid Themes
        SectionLabel("Liquid Theme")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LiquidThemes.ALL) { preset ->
                FilterChip(
                    selected = config.themeId == preset.id,
                    onClick = { updateAndPersist(config.applyTheme(preset)) },
                    label = { Text(preset.name) }
                )
            }
        }

        // 5 Glass Shapes
        SectionLabel("Glass Shape")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LiquidShape.entries) { shape ->
                FilterChip(
                    selected = config.shape == shape,
                    onClick = { updateAndPersist(config.copy(shape = shape)) },
                    label = { Text(shape.label) }
                )
            }
        }

        // 6 Fluid Fonts
        SectionLabel("Fluid Font")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LiquidFont.entries) { font ->
                FilterChip(
                    selected = config.font == font,
                    onClick = { updateAndPersist(config.copy(font = font, fontWeight = font.defaultWeight)) },
                    label = { Text(font.label) }
                )
            }
        }

        // Background Mode
        SectionLabel("Background Mode")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(BackgroundMode.entries) { mode ->
                FilterChip(
                    selected = config.backgroundMode == mode,
                    onClick = { updateAndPersist(config.copy(backgroundMode = mode)) },
                    label = { Text(mode.label) }
                )
            }
        }

        // All 8 Real Optical & Fluid Sliders
        SectionLabel("Optical & Fluid Physics")
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            GlassSlider("Glass Thickness", config.thickness, 0.1f..1f) { updateAndPersist(config.copy(thickness = it)) }
            GlassSlider("Refraction Strength", config.refractionStrength, 0.1f..1.2f) { updateAndPersist(config.copy(refractionStrength = it)) }
            GlassSlider("Optical Blur", config.blur, 0f..1f) { updateAndPersist(config.copy(blur = it)) }
            GlassSlider("Tint Opacity", config.tintOpacity, 0.05f..0.75f) { updateAndPersist(config.copy(tintOpacity = it)) }
            GlassSlider("Bubble Density", config.bubbleDensity, 0f..1.2f) { updateAndPersist(config.copy(bubbleDensity = it)) }
            GlassSlider("Viscosity / Speed", config.viscosity, 0.15f..1.4f) { updateAndPersist(config.copy(viscosity = it)) }
            GlassSlider("Edge Glow", config.edgeGlow, 0f..1.2f) { updateAndPersist(config.copy(edgeGlow = it)) }
            GlassSlider("Chromatic Aberration", config.chromaticAberration, 0f..1.2f) { updateAndPersist(config.copy(chromaticAberration = it)) }
            GlassSlider("Font Size Scale", config.fontSizeScale, 0.75f..1.35f) { updateAndPersist(config.copy(fontSizeScale = it)) }
        }

        // Widget-specific options
        if (selectedType == WidgetType.CLOCK) {
            SectionLabel("Clock Options")
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                ToggleRow("24-Hour Format", config.clockUse24Hour) { updateAndPersist(config.copy(clockUse24Hour = it)) }
                ToggleRow("Analog Liquid Hands Mode", config.clockAnalogMode) { updateAndPersist(config.copy(clockAnalogMode = it)) }
                ToggleRow("Floating Bubble Seconds", config.clockShowSeconds) { updateAndPersist(config.copy(clockShowSeconds = it)) }
            }
        } else if (selectedType == WidgetType.FOCUS_NOTES) {
            SectionLabel("Focus & Glass Note")
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                ToggleRow("Quick Note Mode (Ink in Glass)", config.focusNoteMode) { updateAndPersist(config.copy(focusNoteMode = it)) }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = config.quickNoteText,
                    onValueChange = { updateAndPersist(config.copy(quickNoteText = it)) },
                    label = { Text("Floating Glass Note Text") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    app.configStore.saveConfig(selectedType, config)
                    BatteryAwareScheduler.updateWidgetType(context, selectedType)
                    Toast.makeText(context, "${selectedType.title} applied live!", Toast.LENGTH_SHORT).show()
                    onSaved?.invoke()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Text("Apply Instantly to Home & Lock Screen")
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun GlassSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
