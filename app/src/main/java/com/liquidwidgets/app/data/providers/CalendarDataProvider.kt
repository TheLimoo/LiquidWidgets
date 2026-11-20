package com.liquidwidgets.app.data.providers

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AgendaEvent(
    val eventId: Long,
    val title: String,
    val startMs: Long,
    val timeLabel: String
)

data class CalendarSnapshot(
    val hasPermission: Boolean,
    val events: List<AgendaEvent>,
    val minutesUntilNext: Long?
)

object CalendarDataProvider {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun readTodayNextEvents(context: Context): CalendarSnapshot {
        if (!hasPermission(context)) {
            return CalendarSnapshot(hasPermission = false, events = emptyList(), minutesUntilNext = null)
        }

        val now = System.currentTimeMillis()
        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, endOfDay)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN
        )

        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        val items = mutableListOf<AgendaEvent>()

        runCatching {
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                while (cursor.moveToNext() && items.size < 3) {
                    val id = cursor.getLong(0)
                    val title = cursor.getString(1)?.takeIf { it.isNotBlank() } ?: "Untitled Event"
                    val begin = cursor.getLong(2)
                    items.add(AgendaEvent(id, title, begin, fmt.format(Date(begin))))
                }
            }
        }

        val minsUntil = items.firstOrNull()?.let { ((it.startMs - now) / 60_000L).coerceAtLeast(0L) }
        return CalendarSnapshot(
            hasPermission = true,
            events = items,
            minutesUntilNext = minsUntil
        )
    }
}
