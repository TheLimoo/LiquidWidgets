package com.liquidwidgets.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import com.liquidwidgets.app.LiquidWidgetsApp
import com.liquidwidgets.app.data.model.LiquidGlassConfig
import com.liquidwidgets.app.data.model.LiquidThemes
import com.liquidwidgets.app.ui.components.LiquidGlassSurface
import com.liquidwidgets.app.workers.BatteryAwareScheduler
import kotlinx.coroutines.launch

@Composable
fun ThemesScreen(columns: Int) {
    val context = LocalContext.current
    val app = LiquidWidgetsApp.instance
    val palette by app.wallpaperSampler.palette.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text("12 Liquid Glass Themes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Tap Apply to instantly update every widget on your home & lock screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns.coerceAtLeast(2)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(LiquidThemes.ALL, key = { it.id }) { preset ->
                val sampleConfig = LiquidGlassConfig().applyTheme(preset)
                Column(modifier = Modifier.fillMaxWidth()) {
                    LiquidGlassSurface(
                        config = sampleConfig,
                        wallpaperPalette = palette,
                        modifier = Modifier.fillMaxWidth().height(112.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = preset.name,
                                color = preset.textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = preset.tagline,
                                color = preset.textColor.copy(alpha = 0.82f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                app.configStore.applyThemeToAllWidgets(preset)
                                BatteryAwareScheduler.updateAllWidgets(context)
                                Toast.makeText(context, "Applied ${preset.name} to all widgets", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply to All")
                    }
                }
            }
        }
    }
}
