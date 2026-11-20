package com.liquidwidgets.app.data.providers

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

data class SystemStatsSnapshot(
    /** null when /proc/stat is blocked: show Unavailable, never a fake %. */
    val cpuPercent: Int?,
    val ramPercent: Int,
    val ramUsedGb: Float,
    val ramTotalGb: Float,
    val storagePercent: Int,
    val storageFreeGb: Float
)

object SystemStatsDataProvider {

    @Volatile
    private var prevIdle: Long = 0L

    @Volatile
    private var prevTotal: Long = 0L

    fun read(context: Context): SystemStatsSnapshot {
        val cpu = readCpuPercent()

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val totalMem = memInfo.totalMem.coerceAtLeast(1L)
        val usedMem = (totalMem - memInfo.availMem).coerceAtLeast(0L)
        val ramPct = ((usedMem * 100L) / totalMem).toInt().coerceIn(0, 100)

        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalBytes = statFs.totalBytes.coerceAtLeast(1L)
        val freeBytes = statFs.availableBytes.coerceAtLeast(0L)
        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
        val storagePct = ((usedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)

        return SystemStatsSnapshot(
            cpuPercent = cpu,
            ramPercent = ramPct,
            ramUsedGb = usedMem / 1_073_741_824f,
            ramTotalGb = totalMem / 1_073_741_824f,
            storagePercent = storagePct,
            storageFreeGb = freeBytes / 1_073_741_824f
        )
    }

    /**
     * Pure, unit-tested: parse one "/proc/stat" cpu line into (idle, total) jiffies.
     * Returns null for a malformed line.
     */
    internal fun parseProcStatLine(line: String): Pair<Long, Long>? {
        val parts = line.trim().split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
        if (parts.size < 4) return null
        val idle = parts[3] + (parts.getOrNull(4) ?: 0L)
        return idle to parts.sum()
    }

    /**
     * Reads real CPU load from /proc/stat. Returns null when the file is
     * unreadable (SELinux) or malformed — callers show "Unavailable".
     * There is intentionally NO heap-based fallback: heap pressure is not
     * CPU load, and inventing a percentage would be fake data.
     */
    private fun readCpuPercent(): Int? {
        val (idle, total) = runCatching {
            File("/proc/stat").bufferedReader().use { it.readLine() }
                ?.let { parseProcStatLine(it) }
        }.getOrNull() ?: return null
        val diffTotal = (total - prevTotal).coerceAtLeast(1L)
        val diffIdle = (idle - prevIdle).coerceAtLeast(0L)
        prevTotal = total
        prevIdle = idle
        return if (diffTotal > 0L) {
            (((diffTotal - diffIdle) * 100L) / diffTotal).toInt().coerceIn(0, 100)
        } else {
            null
        }
    }
}
