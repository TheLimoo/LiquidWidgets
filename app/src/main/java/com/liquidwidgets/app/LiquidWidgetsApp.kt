package com.liquidwidgets.app

import android.app.Application
import android.content.Context
import com.liquidwidgets.app.data.prefs.WidgetConfigStore
import com.liquidwidgets.app.engine.WallpaperSampler
import com.liquidwidgets.app.workers.BatteryAwareScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Resolve the app from any Context instead of a global singleton. */
fun Context.liquidApp(): LiquidWidgetsApp =
    applicationContext as LiquidWidgetsApp

class LiquidWidgetsApp : Application() {

    lateinit var configStore: WidgetConfigStore
        private set

    lateinit var wallpaperSampler: WallpaperSampler
        private set

    /**
     * One application-scoped coroutine scope (SupervisorJob so one failing
     * child never cancels the rest). Receivers and workers launch here
     * instead of creating a fresh, never-cancelled CoroutineScope per call.
     */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        configStore = WidgetConfigStore(this)
        wallpaperSampler = WallpaperSampler(this)
        wallpaperSampler.refreshFromWallpaper()
        BatteryAwareScheduler.schedulePeriodicUpdates(this)
        BatteryAwareScheduler.scheduleMinuteTicks(this)
    }
}
