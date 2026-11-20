package com.liquidwidgets.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.liquidwidgets.app.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidwidgets.app.data.model.LiquidGlassConfig
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.data.providers.BatteryDataProvider
import com.liquidwidgets.app.data.providers.CalendarDataProvider
import com.liquidwidgets.app.data.providers.MusicDataProvider
import com.liquidwidgets.app.data.providers.QuickControlsProvider
import com.liquidwidgets.app.data.providers.ScreenTimeDataProvider
import com.liquidwidgets.app.data.providers.StepsDataProvider
import com.liquidwidgets.app.data.providers.SystemStatsDataProvider
import com.liquidwidgets.app.data.providers.WeatherDataProvider
import com.liquidwidgets.app.data.providers.WeatherSnapshot
import com.liquidwidgets.app.engine.WallpaperPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun LiveLiquidWidgetPreview(
    widgetType: WidgetType,
    config: LiquidGlassConfig,
    wallpaperPalette: WallpaperPalette,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val textColor = remember(config.themeId, wallpaperPalette.isLightWallpaper) {
        if (config.themeId == "frosted_milk") Color(0xFF0E2233)
        else if (wallpaperPalette.isLightWallpaper && config.themeId == "clear_water") Color(0xFF0A2034)
        else config.theme.textColor
    }
    val accent = config.theme.accentColor
    val fontFamily = config.font.family
    val fontWeight = config.font.toFontWeight(config.fontWeight)

    var timeSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { timeSec = (it - start) / 1_000_000_000f }
        }
    }

    LiquidGlassSurface(
        config = config,
        wallpaperPalette = wallpaperPalette,
        modifier = modifier,
        onTap = onTap
    ) {
        when (widgetType) {
            WidgetType.CLOCK -> {
                val cal = Calendar.getInstance()
                val pattern = if (config.clockUse24Hour) "HH:mm" else "h:mm"
                val timeStr = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
                val dateStr = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
                val sec = cal.get(Calendar.SECOND)
                // Rising fill follows the MINUTE across the hour, not the second.
                FluidWaveBackdrop(fillFraction = (cal.get(Calendar.MINUTE) / 60f).coerceIn(0.1f, 0.92f), color = config.theme.tintColor, timeSec = timeSec)
                Column(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = timeStr,
                        color = textColor,
                        fontFamily = fontFamily,
                        fontWeight = fontWeight,
                        fontSize = (42f * config.fontSizeScale).sp
                    )
                    Text(
                        text = if (config.clockShowSeconds) "$dateStr  •  ${sec}s" else dateStr,
                        color = textColor.copy(alpha = 0.85f),
                        fontFamily = fontFamily,
                        fontSize = 13.sp
                    )
                }
            }

            WidgetType.BATTERY -> {
                val snap = remember { BatteryDataProvider.read(context) }
                val levelColor = when {
                    snap.percent > 60 -> Color(0xFF24E0F5)
                    snap.percent >= 20 -> Color(0xFFFFB020)
                    else -> Color(0xFFFF2E4C)
                }
                FluidWaveBackdrop(fillFraction = snap.percent / 100f, color = levelColor, timeSec = timeSec * if (snap.isCharging) 2f else 1f)
                Column(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${snap.percent}%",
                        color = textColor,
                        fontFamily = fontFamily,
                        fontWeight = fontWeight,
                        fontSize = (36f * config.fontSizeScale).sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (snap.isCharging) {
                            Icon(
                                painter = painterResource(R.drawable.ic_bolt),
                                contentDescription = "Charging",
                                tint = textColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                        }
                        Text(
                            text = if (snap.isCharging) String.format(Locale.US, "Charging %.1fW", snap.wattage)
                            else "${String.format(Locale.US, "%.1f°C", snap.temperatureC)} • ${snap.healthLabel}",
                            color = textColor.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            WidgetType.SCREEN_TIME -> {
                val snap = remember { ScreenTimeDataProvider.read(context) }
                Row(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Color.White.copy(alpha = 0.25f), style = Stroke(8.dp.toPx()))
                            drawArc(
                                color = accent,
                                startAngle = -90f,
                                sweepAngle = ((snap.totalMinutesToday / 360f).coerceIn(0.12f, 1f)) * 360f,
                                useCenter = false,
                                style = Stroke(8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = if (snap.hasPermission) "${snap.totalMinutesToday / 60}h ${snap.totalMinutesToday % 60}m" else "Grant",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            text = if (snap.hasPermission) "Unlocks ${snap.unlocksToday} • Pickups ${snap.pickupsToday}"
                            else "On-device Usage Access",
                            color = textColor.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        val apps = snap.topApps.ifEmpty { emptyList() }
                        Text(
                            text = if (apps.isNotEmpty()) apps.joinToString("  •  ") { "${it.appLabel.take(6)} ${it.minutes}m" }
                            else "Enable in Settings for top 3 app droplets",
                            color = textColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            WidgetType.WEATHER -> {
                var weather by remember { mutableStateOf<WeatherSnapshot?>(null) }
                LaunchedEffect(Unit) { weather = WeatherDataProvider.read(context) }
                val w = weather
                Row(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${w?.tempC ?: "--"}°",
                            color = textColor,
                            fontFamily = fontFamily,
                            fontWeight = fontWeight,
                            fontSize = (38f * config.fontSizeScale).sp
                        )
                        Text(
                            text = w?.conditionLabel ?: "Loading Open-Meteo…",
                            color = textColor.copy(alpha = 0.88f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = w?.locationLabel ?: "",
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (w?.hourly ?: emptyList()).take(3).forEach { h ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${h.tempC}°", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(h.hourLabel, color = textColor.copy(alpha = 0.75f), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            WidgetType.CALENDAR -> {
                val snap = remember { CalendarDataProvider.readTodayNextEvents(context) }
                Column(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!snap.hasPermission) {
                        Text("Grant Calendar access in Settings", color = textColor, fontSize = 13.sp)
                    } else if (snap.events.isEmpty()) {
                        Text("No events, enjoy the calm", color = textColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    } else {
                        snap.events.take(3).forEach { ev ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(ev.title, color = textColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text(ev.timeLabel, color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            WidgetType.MUSIC -> {
                val snap = remember { MusicDataProvider.read(context) }
                FluidWaveBackdrop(fillFraction = 0.32f, color = accent, timeSec = timeSec * if (snap.isPlaying) 2.5f else 0.7f)
                Row(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(snap.title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(snap.artist, color = textColor.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            painter = painterResource(if (snap.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                            contentDescription = if (snap.isPlaying) "Pause" else "Play",
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_next),
                            contentDescription = "Next track",
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            WidgetType.STEPS -> {
                val snap = remember { StepsDataProvider.read(context, config.dailyStepGoal) }
                val frac = (snap.stepsToday.toFloat() / snap.goal).coerceIn(0.08f, 1f)
                FluidWaveBackdrop(fillFraction = frac, color = Color(0xFF20E3B2), timeSec = timeSec)
                Column(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(String.format(Locale.US, "%,d", snap.stepsToday), color = textColor, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    Text(
                        String.format(Locale.US, "%.1f km • %d kcal", snap.distanceKm, snap.caloriesKcal),
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
            }

            WidgetType.SYSTEM -> {
                val snap = remember { SystemStatsDataProvider.read(context) }
                Row(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("CPU" to snap.cpuPercent, "RAM" to snap.ramPercent, "DISK" to snap.storagePercent).forEach { (label, pct) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (pct == null) "—" else "$pct%", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(label, color = accent, fontSize = 11.sp)
                        }
                    }
                }
            }

            WidgetType.FOCUS_NOTES -> {
                FluidWaveBackdrop(fillFraction = 0.65f, color = config.theme.tintColor, timeSec = timeSec)
                Column(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (config.focusNoteMode) {
                        Text("GLASS NOTE", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(config.quickNoteText, color = textColor, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text("${config.focusDurationMinutes}:00", color = textColor, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                        Text("Pomodoro Droplet", color = textColor.copy(alpha = 0.82f), fontSize = 11.sp)
                    }
                }
            }

            WidgetType.QUICK_PILL -> {
                val state = remember { QuickControlsProvider.read(context) }
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val items = listOf(
                        "Wi-Fi" to state.wifiOn,
                        "BT" to state.bluetoothOn,
                        "Torch" to state.torchOn,
                        "DND" to state.dndOn
                    )
                    items.forEach { (label, on) ->
                        Text(
                            text = label,
                            color = if (on) accent else textColor.copy(alpha = 0.65f),
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = "Search",
                            tint = textColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text("Search", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FluidWaveBackdrop(fillFraction: Float, color: Color, timeSec: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val surfaceY = size.height * (1f - fillFraction.coerceIn(0.05f, 0.95f))
        val amp = 6.dp.toPx()
        val path = Path().apply {
            moveTo(0f, size.height)
            lineTo(0f, surfaceY)
            val steps = 24
            for (i in 0..steps) {
                val x = size.width * i / steps
                val y = surfaceY + sin((i.toFloat() / steps) * 2f * PI.toFloat() * 1.5f + timeSec * 2.2f) * amp
                lineTo(x, y)
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(path, color = color.copy(alpha = 0.36f))

        // Rising bubbles
        for (i in 0..5) {
            val prog = ((timeSec * (0.18f + i * 0.04f) + i * 0.23f) % 1f)
            val y = size.height - prog * (size.height - surfaceY).coerceAtLeast(16f)
            val x = size.width * (0.14f + (i * 0.16f) % 0.76f)
            drawCircle(color = Color.White.copy(alpha = 0.45f), radius = (3 + i % 3).dp.toPx(), center = Offset(x, y))
        }
    }
}
