package com.liquidwidgets.app.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liquidwidgets.app.liquidApp
import com.liquidwidgets.app.data.model.LiquidGlassConfig
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.ui.components.LiveLiquidWidgetPreview
import com.liquidwidgets.app.widgets.LiquidBatteryWidgetReceiver
import com.liquidwidgets.app.widgets.LiquidCalendarWidgetReceiver
import com.liquidwidgets.app.widgets.LiquidClockWidgetReceiver
import com.liquidwidgets.app.widgets.LiquidFocusNotesWidgetReceiver
import com.liquidwidgets.app.widgets.LiquidMusicWidgetReceiver
import com.liquidwidgets.app.widgets.LiquidQuickPillWidgetReceiver
import com.liquidwidgets.app.widgets.LiquidScreenTimeWidgetReceiver
import com.liquidwidgets.app.widgets.LiquidStepsWidgetReceiver
import com.liquidwidgets.app.widgets.LiquidSystemWidgetReceiver
import com.liquidwidgets.app.widgets.LiquidWeatherWidgetReceiver

@Composable
fun WidgetGalleryScreen(
    columns: Int,
    onEditWidget: (WidgetType) -> Unit
) {
    val context = LocalContext.current
    val app = LocalContext.current.liquidApp()
    val palette by app.wallpaperSampler.palette.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                text = "Liquid Widgets",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Real Liquid Glass Widgets for Android 17 • Tap any glass card to customize",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(WidgetType.entries, key = { it.id }) { type ->
                val config by app.configStore.observeConfig(type).collectAsState(initial = LiquidGlassConfig())
                Column(modifier = Modifier.fillMaxWidth()) {
                    LiveLiquidWidgetPreview(
                        widgetType = type,
                        config = config,
                        wallpaperPalette = palette,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (type == WidgetType.QUICK_PILL) 88.dp else 156.dp),
                        onTap = { onEditWidget(type) }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(type.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${type.defaultSpan} • ${type.subtitle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { requestPinWidget(context, type) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Add to Home Screen")
                        }
                        OutlinedButton(
                            onClick = { onEditWidget(type) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = "Customize")
                        }
                    }
                }
            }
        }
    }
}

private fun requestPinWidget(context: Context, type: WidgetType) {
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) {
        Toast.makeText(context, "Long-press your Home Screen → Widgets → Liquid Widgets", Toast.LENGTH_LONG).show()
        return
    }
    val receiverClass = when (type) {
        WidgetType.CLOCK -> LiquidClockWidgetReceiver::class.java
        WidgetType.BATTERY -> LiquidBatteryWidgetReceiver::class.java
        WidgetType.SCREEN_TIME -> LiquidScreenTimeWidgetReceiver::class.java
        WidgetType.WEATHER -> LiquidWeatherWidgetReceiver::class.java
        WidgetType.CALENDAR -> LiquidCalendarWidgetReceiver::class.java
        WidgetType.MUSIC -> LiquidMusicWidgetReceiver::class.java
        WidgetType.STEPS -> LiquidStepsWidgetReceiver::class.java
        WidgetType.SYSTEM -> LiquidSystemWidgetReceiver::class.java
        WidgetType.FOCUS_NOTES -> LiquidFocusNotesWidgetReceiver::class.java
        WidgetType.QUICK_PILL -> LiquidQuickPillWidgetReceiver::class.java
    }
    val provider = ComponentName(context, receiverClass)
    manager.requestPinAppWidget(provider, null, null)
}
