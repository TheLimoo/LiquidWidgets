package com.liquidwidgets.app

import com.liquidwidgets.app.data.providers.SystemStatsDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemStatsTest {

    @Test
    fun `parses proc stat cpu line into idle and total`() {
        // cpu user nice system idle iowait irq softirq steal guest guest_nice
        val parsed = SystemStatsDataProvider.parseProcStatLine("cpu  100 20 50 800 30 5 10 0 0 0")
        assertEquals(830L to 1015L, parsed)
    }

    @Test
    fun `malformed proc stat line returns null`() {
        assertNull(SystemStatsDataProvider.parseProcStatLine("cpu  1 2"))
        assertNull(SystemStatsDataProvider.parseProcStatLine("garbage"))
        assertNull(SystemStatsDataProvider.parseProcStatLine(""))
    }
}
