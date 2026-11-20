package com.liquidwidgets.app.ui.screens

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liquidwidgets.app.liquidApp
import com.liquidwidgets.app.wallpaper.LiquidGlassWallpaperService
import com.liquidwidgets.app.data.providers.CalendarDataProvider
import com.liquidwidgets.app.data.providers.MusicDataProvider
import com.liquidwidgets.app.data.providers.ScreenTimeDataProvider
import com.liquidwidgets.app.data.providers.StepsDataProvider
import com.liquidwidgets.app.workers.BatteryAwareScheduler
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = LocalContext.current.liquidApp()
    val scope = rememberCoroutineScope()

    val pauseOnBatterySaver by app.configStore.observePauseOnBatterySaver().collectAsState(initial = true)
    val gyroEnabled by app.configStore.observeGyroscopeEnabled().collectAsState(initial = true)

    val runtimePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        scope.launch { BatteryAwareScheduler.updateAllWidgets(context) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings & Privacy", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "100% on-device. No account, no ads, no analytics. Only Weather uses the free Open-Meteo API.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Performance & Battery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pause animations on Battery Saver")
                        Text("Keeps widget drain under 1%/hr", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = pauseOnBatterySaver,
                        onCheckedChange = { scope.launch { app.configStore.setPauseOnBatterySaver(it) } }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gyroscope Specular Highlight")
                        Text("Shifts top-left glass reflection as phone tilts", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = gyroEnabled,
                        onCheckedChange = { scope.launch { app.configStore.setGyroscopeEnabled(it) } }
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Widget Permissions (On-Device)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                PermissionActionRow(
                    title = "Calendar, Steps & Weather Location",
                    granted = CalendarDataProvider.hasPermission(context) && StepsDataProvider.hasPermission(context),
                    actionLabel = "Grant"
                ) {
                    runtimePermLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_CALENDAR,
                            Manifest.permission.ACTIVITY_RECOGNITION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    )
                }

                PermissionActionRow(
                    title = "Usage Access (Screen Time Widget)",
                    granted = ScreenTimeDataProvider.hasUsageAccess(context),
                    actionLabel = "Open"
                ) {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }

                PermissionActionRow(
                    title = "Notification Access (Music Player)",
                    granted = MusicDataProvider.hasNotificationAccess(context),
                    actionLabel = "Open"
                ) {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Live Liquid Wallpaper (optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Widgets cannot run a live shader, but a live wallpaper can. " +
                        "Set the animated liquid-glass wallpaper, then use the Clear Glass theme — " +
                        "the widgets sit transparently on living liquid.",
                    style = MaterialTheme.typography.bodyMedium
                )
                FilledTonalButton(
                    onClick = {
                        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                            putExtra(
                                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                ComponentName(context, LiquidGlassWallpaperService::class.java)
                            )
                        }
                        runCatching { context.startActivity(intent) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set Liquid Live Wallpaper")
                }
                Text(
                    "Quick Pill honesty: Wi-Fi and Bluetooth open the system settings — " +
                        "Android does not let apps toggle them directly. Torch and DND really toggle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                scope.launch {
                    app.wallpaperSampler.refreshFromWallpaper()
                    BatteryAwareScheduler.updateAllWidgets(context)
                    Toast.makeText(context, "Resampled wallpaper & updated all 10 widgets", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Resample Wallpaper & Refresh All Widgets")
        }
    }
}

@Composable
private fun PermissionActionRow(
    title: String,
    granted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (granted) "Granted" else "Not granted yet",
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FilledTonalButton(onClick = onClick) {
            Text(if (granted) "OK" else actionLabel)
        }
    }
}
