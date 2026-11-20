package com.liquidwidgets.app.workers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.PowerManager
import android.view.Display
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.liquidwidgets.app.liquidApp
import com.liquidwidgets.app.receivers.ClockTickReceiver
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.widgets.LiquidBatteryWidget
import com.liquidwidgets.app.widgets.LiquidCalendarWidget
import com.liquidwidgets.app.widgets.LiquidClockWidget
import com.liquidwidgets.app.widgets.LiquidFocusNotesWidget
import com.liquidwidgets.app.widgets.LiquidMusicWidget
import com.liquidwidgets.app.widgets.LiquidQuickPillWidget
import com.liquidwidgets.app.widgets.LiquidScreenTimeWidget
import com.liquidwidgets.app.widgets.LiquidStepsWidget
import com.liquidwidgets.app.widgets.LiquidSystemWidget
import com.liquidwidgets.app.widgets.LiquidWeatherWidget
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object BatteryAwareScheduler {

    private const val PERIODIC_WORK_NAME = "liquid_widgets_periodic_sync"

    /**
     * Arms a one-shot, non-waking alarm for the next minute boundary. The
     * receiver re-arms itself on every tick, so the clock keeps ticking
     * once a minute without a persistent service or a waking alarm.
     */
    fun scheduleMinuteTicks(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ClockTickReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val now = System.currentTimeMillis()
        val nextMinute = ((now / 60_000L) + 1) * 60_000L
        runCatching { am.set(AlarmManager.RTC, nextMinute, pi) }
    }

    fun schedulePeriodicUpdates(context: Context) {
        val request = PeriodicWorkRequestBuilder<LiquidWidgetSyncWorker>(
            15, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun isScreenInteractive(context: Context): Boolean {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return true
        return dm.displays.any { it.state == Display.STATE_ON }
    }

    suspend fun shouldThrottleAnimations(context: Context): Boolean {
        if (!isScreenInteractive(context)) return true
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val pauseSetting = context.liquidApp().configStore.observePauseOnBatterySaver().first()
        return pauseSetting && (pm?.isPowerSaveMode == true)
    }

    suspend fun updateAllWidgets(context: Context) {
        if (!isScreenInteractive(context)) return
        LiquidClockWidget().updateAll(context)
        LiquidBatteryWidget().updateAll(context)
        LiquidScreenTimeWidget().updateAll(context)
        LiquidWeatherWidget().updateAll(context)
        LiquidCalendarWidget().updateAll(context)
        LiquidMusicWidget().updateAll(context)
        LiquidStepsWidget().updateAll(context)
        LiquidSystemWidget().updateAll(context)
        LiquidFocusNotesWidget().updateAll(context)
        LiquidQuickPillWidget().updateAll(context)
    }

    suspend fun updateWidgetType(context: Context, type: WidgetType) {
        when (type) {
            WidgetType.CLOCK -> LiquidClockWidget().updateAll(context)
            WidgetType.BATTERY -> LiquidBatteryWidget().updateAll(context)
            WidgetType.SCREEN_TIME -> LiquidScreenTimeWidget().updateAll(context)
            WidgetType.WEATHER -> LiquidWeatherWidget().updateAll(context)
            WidgetType.CALENDAR -> LiquidCalendarWidget().updateAll(context)
            WidgetType.MUSIC -> LiquidMusicWidget().updateAll(context)
            WidgetType.STEPS -> LiquidStepsWidget().updateAll(context)
            WidgetType.SYSTEM -> LiquidSystemWidget().updateAll(context)
            WidgetType.FOCUS_NOTES -> LiquidFocusNotesWidget().updateAll(context)
            WidgetType.QUICK_PILL -> LiquidQuickPillWidget().updateAll(context)
        }
    }
}

class LiquidWidgetSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!BatteryAwareScheduler.isScreenInteractive(applicationContext)) {
            return Result.success()
        }
        applicationContext.liquidApp().wallpaperSampler.refreshFromWallpaper()
        BatteryAwareScheduler.updateAllWidgets(applicationContext)
        return Result.success()
    }
}
