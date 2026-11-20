package com.liquidwidgets.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.liquidwidgets.app.LiquidWidgetsApp
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.workers.BatteryAwareScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Event-driven receiver: updates glass refraction on wallpaper changes, dark/light switch,
 * charger connect/disconnect, boot, and Battery Saver toggle — zero persistent background service.
 */
class LiquidSystemReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED -> {
                        BatteryAwareScheduler.schedulePeriodicUpdates(context)
                        BatteryAwareScheduler.updateAllWidgets(context)
                    }
                    Intent.ACTION_WALLPAPER_CHANGED,
                    Intent.ACTION_CONFIGURATION_CHANGED -> {
                        LiquidWidgetsApp.instance.wallpaperSampler.refreshFromWallpaper()
                        BatteryAwareScheduler.updateAllWidgets(context)
                    }
                    Intent.ACTION_POWER_CONNECTED,
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        BatteryAwareScheduler.updateWidgetType(context, WidgetType.BATTERY)
                    }
                    android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                        BatteryAwareScheduler.updateAllWidgets(context)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
