package com.liquidwidgets.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.liquidwidgets.app.workers.BatteryAwareScheduler
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.ui.screens.AppSearchDetailScreen
import com.liquidwidgets.app.ui.screens.LiveEditorScreen
import com.liquidwidgets.app.ui.screens.ScreenTimeWeeklyScreen
import com.liquidwidgets.app.ui.screens.SettingsScreen
import com.liquidwidgets.app.ui.screens.SystemMonitorDetailScreen
import com.liquidwidgets.app.ui.screens.ThemesScreen
import com.liquidwidgets.app.ui.screens.WeatherDetailScreen
import com.liquidwidgets.app.ui.screens.WidgetGalleryScreen
import com.liquidwidgets.app.ui.theme.LiquidWidgetsTheme

private data class NavDest(val route: String, val label: String, val icon: ImageVector)

private val TOP_DESTINATIONS = listOf(
    NavDest("widgets", "Widgets", Icons.Rounded.GridView),
    NavDest("themes", "Themes", Icons.Rounded.AutoAwesome),
    NavDest("editor/clock", "Editor", Icons.Rounded.Tune),
    NavDest("settings", "Settings", Icons.Rounded.Settings)
)

class MainActivity : ComponentActivity() {

    /**
     * Re-reads real system state (Wi-Fi/Bluetooth after the user flipped
     * them in Settings, steps, battery) every time the user comes back.
     */
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            runCatching { BatteryAwareScheduler.updateAllWidgets(this@MainActivity) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val startRoute = when {
            appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID -> "editor/clock"
            else -> intent?.getStringExtra(EXTRA_START_ROUTE) ?: "widgets"
        }

        setContent {
            val windowSize = calculateWindowSizeClass(this)
            LiquidWidgetsTheme {
                LiquidWidgetsRoot(
                    widthSizeClass = windowSize.widthSizeClass,
                    startRoute = startRoute,
                    onConfigureComplete = {
                        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            setResult(Activity.RESULT_OK, result)
                            finish()
                        }
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_START_ROUTE = "extra_start_route"
    }
}

@Composable
private fun LiquidWidgetsRoot(
    widthSizeClass: WindowWidthSizeClass,
    startRoute: String,
    onConfigureComplete: () -> Unit
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "widgets"
    val useRail = widthSizeClass != WindowWidthSizeClass.Compact
    val gridColumns = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 1
        WindowWidthSizeClass.Medium -> 2
        else -> 3
    }

    fun navigateTop(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (!useRail) {
                NavigationBar {
                    TOP_DESTINATIONS.forEach { dest ->
                        val selected = currentRoute.startsWith(dest.route.substringBefore('/'))
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTop(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (useRail) {
                NavigationRail {
                    TOP_DESTINATIONS.forEach { dest ->
                        val selected = currentRoute.startsWith(dest.route.substringBefore('/'))
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTop(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                NavHost(navController = navController, startDestination = startRoute) {
                    composable("widgets") {
                        WidgetGalleryScreen(
                            columns = gridColumns,
                            onEditWidget = { type -> navController.navigate("editor/${type.id}") }
                        )
                    }
                    composable("themes") {
                        ThemesScreen(columns = gridColumns)
                    }
                    composable("editor/{widgetType}") { entry ->
                        val type = WidgetType.fromId(entry.arguments?.getString("widgetType"))
                        LiveEditorScreen(
                            initialWidgetType = type,
                            onSaved = onConfigureComplete
                        )
                    }
                    composable("settings") { SettingsScreen() }
                    composable("detail_weather") { WeatherDetailScreen() }
                    composable("detail_system") { SystemMonitorDetailScreen() }
                    composable("detail_screentime") { ScreenTimeWeeklyScreen() }
                    composable("detail_search") { AppSearchDetailScreen() }
                }
            }
        }
    }
}
