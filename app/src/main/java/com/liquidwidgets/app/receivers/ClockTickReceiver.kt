package com.liquidwidgets.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.liquidApp
import com.liquidwidgets.app.workers.BatteryAwareScheduler
import kotlinx.coroutines.launch

/**
 * Fired once a minute by an AlarmManager RTC alarm (see
 * BatteryAwareScheduler.scheduleMinuteTicks). TIME_TICK cannot be received
 * from the manifest, so the alarm is the reliable way to keep the Clock
 * (and the running Pomodoro timer) fresh: previously both only updated on
 * the 15-minute WorkManager sync and could sit 15-30 minutes stale.
 * The alarm is non-waking (RTC) and re-armed on every tick; ticks are
 * skipped entirely while the screen is off.
 */
class ClockTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        context.liquidApp().applicationScope.launch {
            try {
                if (BatteryAwareScheduler.isScreenInteractive(context)) {
                    BatteryAwareScheduler.updateWidgetType(context, WidgetType.CLOCK)
                    BatteryAwareScheduler.updateWidgetType(context, WidgetType.FOCUS_NOTES)
                }
            } finally {
                BatteryAwareScheduler.scheduleMinuteTicks(context)
                pending.finish()
            }
        }
    }
}
