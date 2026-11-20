package com.liquidwidgets.app

import android.app.Application
import com.liquidwidgets.app.data.prefs.WidgetConfigStore
import com.liquidwidgets.app.engine.WallpaperSampler
import com.liquidwidgets.app.workers.BatteryAwareScheduler

class LiquidWidgetsApp : Application() {

    lateinit var configStore: WidgetConfigStore
        private set

    lateinit var wallpaperSampler: WallpaperSampler
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        configStore = WidgetConfigStore(this)
        wallpaperSampler = WallpaperSampler(this)
        wallpaperSampler.refreshFromWallpaper()
        BatteryAwareScheduler.schedulePeriodicUpdates(this)
    }

    companion object {
        lateinit var instance: LiquidWidgetsApp
            private set
    }
}
