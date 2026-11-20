package com.liquidwidgets.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.liquidwidgets.app.liquidApp
import com.liquidwidgets.app.MainActivity
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.data.providers.MusicDataProvider
import com.liquidwidgets.app.data.providers.QuickControlsProvider
import com.liquidwidgets.app.workers.BatteryAwareScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handles every widget tap: triggers a real liquid touch ripple frame settle and
 * executes the widget's native action (Clock, Battery Settings, App Launch, Media, Toggles, etc.).
 */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val type = WidgetType.fromId(intent.getStringExtra(EXTRA_WIDGET_TYPE))
        val pending = goAsync()
        val app = context.liquidApp()

        app.applicationScope.launch(Dispatchers.Main) {
            try {
                val store = app.configStore
                store.triggerRipple(type)
                BatteryAwareScheduler.updateWidgetType(context, type)

                when (action) {
                    ACTION_OPEN_CLOCK -> {
                        val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(clockIntent) }
                    }
                    ACTION_OPEN_BATTERY -> {
                        val batIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(batIntent) }
                    }
                    ACTION_LAUNCH_PACKAGE -> {
                        val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                        val launch = pkg?.let { context.packageManager.getLaunchIntentForPackage(it) }
                        if (launch != null) {
                            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launch)
                        } else {
                            openInAppRoute(context, "detail_screentime")
                        }
                    }
                    ACTION_OPEN_WEATHER -> openInAppRoute(context, "detail_weather")
                    ACTION_OPEN_CALENDAR -> {
                        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
                        val uri = if (eventId > 0L) {
                            Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, eventId.toString())
                        } else {
                            CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
                        }
                        val calIntent = Intent(Intent.ACTION_VIEW, uri)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(calIntent) }
                    }
                    ACTION_MUSIC_PLAY_PAUSE -> {
                        MusicDataProvider.togglePlayPause(context)
                        BatteryAwareScheduler.updateWidgetType(context, WidgetType.MUSIC)
                    }
                    ACTION_MUSIC_NEXT -> {
                        MusicDataProvider.skipNext(context)
                        BatteryAwareScheduler.updateWidgetType(context, WidgetType.MUSIC)
                    }
                    ACTION_MUSIC_OPEN -> MusicDataProvider.openPlayerApp(context)
                    ACTION_OPEN_STEPS -> openInAppRoute(context, "editor/steps")
                    ACTION_OPEN_SYSTEM -> openInAppRoute(context, "detail_system")
                    ACTION_FOCUS_TOGGLE -> {
                        val endMs = store.getFocusEndEpochMs()
                        val now = System.currentTimeMillis()
                        if (endMs > now) {
                            store.setFocusEndEpochMs(0L)
                        } else {
                            val cfg = store.getConfig(WidgetType.FOCUS_NOTES)
                            store.setFocusEndEpochMs(now + cfg.focusDurationMinutes * 60_000L)
                        }
                        BatteryAwareScheduler.updateWidgetType(context, WidgetType.FOCUS_NOTES)
                    }
                    ACTION_OPEN_NOTES -> openInAppRoute(context, "editor/focus")
                    // Wi-Fi / Bluetooth: Android 10+ does not let a normal app toggle
                    // these directly, so the tap honestly opens the system panel
                    // instead of pretending to toggle. State is re-read from the
                    // system (and the widget refreshed) when the user comes back.
                    ACTION_TOGGLE_WIFI -> QuickControlsProvider.openWifiPanel(context)
                    ACTION_TOGGLE_BT -> QuickControlsProvider.openBluetoothSettings(context)
                    ACTION_TOGGLE_TORCH -> {
                        QuickControlsProvider.toggleFlashlight(context)
                        BatteryAwareScheduler.updateWidgetType(context, WidgetType.QUICK_PILL)
                    }
                    ACTION_TOGGLE_DND -> {
                        QuickControlsProvider.toggleDnd(context)
                        BatteryAwareScheduler.updateWidgetType(context, WidgetType.QUICK_PILL)
                    }
                    ACTION_OPEN_SEARCH -> openInAppRoute(context, "detail_search")
                }

                // Settle ripple after 550ms
                delay(550L)
                BatteryAwareScheduler.updateWidgetType(context, type)
            } finally {
                pending.finish()
            }
        }
    }

    private fun openInAppRoute(context: Context, route: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_START_ROUTE, route)
        }
        context.startActivity(intent)
    }

    companion object {
        const val EXTRA_WIDGET_TYPE = "extra_widget_type"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_EVENT_ID = "extra_event_id"

        const val ACTION_OPEN_CLOCK = "com.liquidwidgets.app.ACTION_OPEN_CLOCK"
        const val ACTION_OPEN_BATTERY = "com.liquidwidgets.app.ACTION_OPEN_BATTERY"
        const val ACTION_LAUNCH_PACKAGE = "com.liquidwidgets.app.ACTION_LAUNCH_PACKAGE"
        const val ACTION_OPEN_WEATHER = "com.liquidwidgets.app.ACTION_OPEN_WEATHER"
        const val ACTION_OPEN_CALENDAR = "com.liquidwidgets.app.ACTION_OPEN_CALENDAR"
        const val ACTION_MUSIC_PLAY_PAUSE = "com.liquidwidgets.app.ACTION_MUSIC_PLAY_PAUSE"
        const val ACTION_MUSIC_NEXT = "com.liquidwidgets.app.ACTION_MUSIC_NEXT"
        const val ACTION_MUSIC_OPEN = "com.liquidwidgets.app.ACTION_MUSIC_OPEN"
        const val ACTION_OPEN_STEPS = "com.liquidwidgets.app.ACTION_OPEN_STEPS"
        const val ACTION_OPEN_SYSTEM = "com.liquidwidgets.app.ACTION_OPEN_SYSTEM"
        const val ACTION_FOCUS_TOGGLE = "com.liquidwidgets.app.ACTION_FOCUS_TOGGLE"
        const val ACTION_OPEN_NOTES = "com.liquidwidgets.app.ACTION_OPEN_NOTES"
        const val ACTION_TOGGLE_WIFI = "com.liquidwidgets.app.ACTION_TOGGLE_WIFI"
        const val ACTION_TOGGLE_BT = "com.liquidwidgets.app.ACTION_TOGGLE_BT"
        const val ACTION_TOGGLE_TORCH = "com.liquidwidgets.app.ACTION_TOGGLE_TORCH"
        const val ACTION_TOGGLE_DND = "com.liquidwidgets.app.ACTION_TOGGLE_DND"
        const val ACTION_OPEN_SEARCH = "com.liquidwidgets.app.ACTION_OPEN_SEARCH"

        fun intentFor(
            context: Context,
            action: String,
            widgetType: WidgetType,
            packageName: String? = null,
            eventId: Long? = null
        ): Intent = Intent(context, WidgetActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_WIDGET_TYPE, widgetType.id)
            if (packageName != null) putExtra(EXTRA_PACKAGE_NAME, packageName)
            if (eventId != null) putExtra(EXTRA_EVENT_ID, eventId)
        }
    }
}
