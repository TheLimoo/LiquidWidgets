package com.liquidwidgets.app.data.providers

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

data class AppUsageEntry(
    val packageName: String,
    val appLabel: String,
    val minutes: Long
)

data class ScreenTimeSnapshot(
    val hasPermission: Boolean,
    val totalMinutesToday: Long,
    val topApps: List<AppUsageEntry>,
    val unlocksToday: Int,
    val pickupsToday: Int,
    val weeklyMinutes: List<Pair<String, Long>> = emptyList()
)

object ScreenTimeDataProvider {

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun read(context: Context, includeWeekly: Boolean = false): ScreenTimeSnapshot {
        if (!hasUsageAccess(context)) {
            return ScreenTimeSnapshot(
                hasPermission = false,
                totalMinutesToday = 0L,
                topApps = emptyList(),
                unlocksToday = 0,
                pickupsToday = 0
            )
        }

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return ScreenTimeSnapshot(false, 0L, emptyList(), 0, 0)

        val now = System.currentTimeMillis()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now).orEmpty()
        val pm = context.packageManager

        val grouped = mutableMapOf<String, Long>()
        stats.forEach { entry ->
            if (entry.totalTimeInForeground > 0L && entry.packageName != context.packageName) {
                grouped[entry.packageName] = (grouped[entry.packageName] ?: 0L) + entry.totalTimeInForeground
            }
        }

        val totalMs = grouped.values.sum()
        val top3 = grouped.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { (pkg, ms) ->
                val label = runCatching {
                    val info = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(info).toString()
                }.getOrDefault(pkg.substringAfterLast('.'))
                AppUsageEntry(
                    packageName = pkg,
                    appLabel = label,
                    minutes = (ms / 60_000L).coerceAtLeast(1L)
                )
            }

        var unlocks = 0
        var pickups = 0
        runCatching {
            val events = usm.queryEvents(startOfDay, now)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.KEYGUARD_HIDDEN -> unlocks++
                    UsageEvents.Event.SCREEN_INTERACTIVE -> pickups++
                }
            }
        }

        val weekly = if (includeWeekly) readWeeklyBreakdown(usm) else emptyList()

        return ScreenTimeSnapshot(
            hasPermission = true,
            totalMinutesToday = totalMs / 60_000L,
            topApps = top3,
            unlocksToday = unlocks,
            pickupsToday = pickups,
            weeklyMinutes = weekly
        )
    }

    private fun readWeeklyBreakdown(usm: UsageStatsManager): List<Pair<String, Long>> {
        val result = mutableListOf<Pair<String, Long>>()
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val cal = Calendar.getInstance()
        for (offset in 6 downTo 0) {
            val dayStart = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -offset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayEnd = (dayStart.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                dayStart.timeInMillis,
                minOf(dayEnd.timeInMillis, System.currentTimeMillis())
            ).orEmpty()
            val mins = stats.sumOf { it.totalTimeInForeground } / 60_000L
            val label = dayLabels[dayStart.get(Calendar.DAY_OF_WEEK) - 1]
            result.add(label to mins)
        }
        return result
    }
}
