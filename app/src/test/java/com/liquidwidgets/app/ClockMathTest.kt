package com.liquidwidgets.app

import com.liquidwidgets.app.widgets.minuteFillFor
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockMathTest {

    @Test
    fun `minute fill rises across the hour, not the minute`() {
        assertEquals(0.06f, minuteFillFor(0), 0.001f)
        assertEquals(0.5f, minuteFillFor(30), 0.001f)
        assertEquals(0.96f, minuteFillFor(59), 0.001f)
        // The old bug used seconds/60, which reset the fill every minute.
        assertEquals(0.06f, minuteFillFor(-5), 0.001f)
        assertEquals(0.96f, minuteFillFor(90), 0.001f)
    }
}
