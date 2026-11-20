package com.liquidwidgets.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liquidwidgets.app.LiquidWidgetsApp
import com.liquidwidgets.app.data.model.LiquidGlassConfig
import com.liquidwidgets.app.data.providers.ScreenTimeDataProvider
import com.liquidwidgets.app.data.providers.SystemStatsDataProvider
import com.liquidwidgets.app.data.providers.WeatherDataProvider
import com.liquidwidgets.app.data.providers.WeatherSnapshot
import com.liquidwidgets.app.ui.components.LiquidGlassSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WeatherDetailScreen() {
    val context = LocalContext.current
    val palette by LiquidWidgetsApp.instance.wallpaperSampler.palette.collectAsState()
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<WeatherSnapshot?>(null) }

    LaunchedEffect(Unit) { snapshot = WeatherDataProvider.read(context, forceNetwork = true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Liquid Weather", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        LiquidGlassSurface(
            config = LiquidGlassConfig(),
            wallpaperPalette = palette,
            modifier = Modifier.fillMaxWidth().height(170.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.Center) {
                Text("${snapshot?.tempC ?: "--"}°C", style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(snapshot?.conditionLabel ?: "Fetching Open-Meteo…", color = Color.White.copy(alpha = 0.9f))
                Text(
                    "Humidity ${snapshot?.humidityPercent ?: 0}%  •  Wind ${snapshot?.windKmh ?: 0} km/h",
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        Button(onClick = { scope.launch { snapshot = WeatherDataProvider.read(context, forceNetwork = true) } }) {
            Text("Refresh Live Forecast")
        }
    }
}

@Composable
fun SystemMonitorDetailScreen() {
    val context = LocalContext.current
    val palette by LiquidWidgetsApp.instance.wallpaperSampler.palette.collectAsState()
    val cpuHistory = remember { mutableStateListOf<Int>() }
    val ramHistory = remember { mutableStateListOf<Int>() }
    var latest by remember { mutableStateOf(SystemStatsDataProvider.read(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            val s = SystemStatsDataProvider.read(context)
            latest = s
            cpuHistory.add(s.cpuPercent)
            ramHistory.add(s.ramPercent)
            if (cpuHistory.size > 30) cpuHistory.removeAt(0)
            if (ramHistory.size > 30) ramHistory.removeAt(0)
            delay(1000L)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Live System Monitor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        GlassGraphCard("CPU Load: ${latest.cpuPercent}%", cpuHistory, Color(0xFF38E8FF), palette)
        GlassGraphCard("RAM Usage: ${latest.ramPercent}% (${String.format("%.1f / %.1f GB", latest.ramUsedGb, latest.ramTotalGb)})", ramHistory, Color(0xFFB388FF), palette)
        LiquidGlassSurface(config = LiquidGlassConfig(), wallpaperPalette = palette, modifier = Modifier.fillMaxWidth().height(92.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
                Text("Storage Used: ${latest.storagePercent}%", color = Color.White, fontWeight = FontWeight.Bold)
                Text("${String.format("%.1f GB", latest.storageFreeGb)} available", color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun GlassGraphCard(
    title: String,
    points: List<Int>,
    strokeColor: Color,
    palette: com.liquidwidgets.app.engine.WallpaperPalette
) {
    LiquidGlassSurface(config = LiquidGlassConfig(), wallpaperPalette = palette, modifier = Modifier.fillMaxWidth().height(148.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (points.size >= 2) {
                    val path = Path()
                    points.forEachIndexed { idx, v ->
                        val x = size.width * idx / (points.size - 1)
                        val y = size.height * (1f - (v / 100f).coerceIn(0f, 1f))
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = strokeColor, style = Stroke(width = 3.dp.toPx()))
                }
            }
        }
    }
}

@Composable
fun ScreenTimeWeeklyScreen() {
    val context = LocalContext.current
    val snap = remember { ScreenTimeDataProvider.read(context, includeWeekly = true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Weekly Screen Time (On-Device)", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (!snap.hasPermission) {
            Text("Enable Usage Access in Settings to see real weekly screen time.")
        } else {
            val maxMins = snap.weeklyMinutes.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
            snap.weeklyMinutes.forEach { (day, mins) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(day, fontWeight = FontWeight.SemiBold)
                    Text("${mins / 60}h ${mins % 60}m (${((mins * 100) / maxMins)}%)")
                }
            }
        }
    }
}

@Composable
fun AppSearchDetailScreen() {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val installedApps = remember {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }
    }
    val filtered = remember(query, installedApps) {
        if (query.isBlank()) installedApps else installedApps.filter { it.first.contains(query, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Quick Glass App Search", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search installed apps") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(filtered, key = { it.second }) { (label, pkg) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.packageManager.getLaunchIntentForPackage(pkg)?.let { launch ->
                                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(launch)
                            }
                        }
                        .padding(vertical = 10.dp, horizontal = 8.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
