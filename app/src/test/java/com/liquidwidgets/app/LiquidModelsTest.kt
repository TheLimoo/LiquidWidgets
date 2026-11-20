package com.liquidwidgets.app

import com.liquidwidgets.app.data.model.LiquidFont
import com.liquidwidgets.app.data.model.LiquidShape
import com.liquidwidgets.app.data.model.LiquidThemes
import com.liquidwidgets.app.data.model.WidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidModelsTest {

    @Test
    fun `themes lookup falls back to first theme for unknown id`() {
        assertEquals("aqua", LiquidThemes.byId("nope").id)
        assertEquals("mercury", LiquidThemes.byId("mercury").id)
        assertEquals(12, LiquidThemes.ALL.size)
    }

    @Test
    fun `all six fonts map to distinct bundled font files`() {
        val resIds = LiquidFont.entries.map { it.fontRes }
        assertEquals(6, resIds.distinct().size)
        assertTrue(resIds.all { it != 0 })
        assertEquals(LiquidFont.FLUID_SANS, LiquidFont.fromId("fluid_sans"))
        assertEquals(LiquidFont.FLUID_SANS, LiquidFont.fromId(null))
        assertEquals(LiquidFont.GLASS_DISPLAY, LiquidFont.fromId("glass_display"))
    }

    @Test
    fun `widget and shape lookups fall back safely`() {
        assertEquals(WidgetType.CLOCK, WidgetType.fromId("unknown"))
        assertEquals(WidgetType.WEATHER, WidgetType.fromId("weather"))
        assertEquals(10, WidgetType.entries.size)
        assertEquals(LiquidShape.SQUIRCLE, LiquidShape.fromId(null))
        assertNotEquals(LiquidShape.PILL, LiquidShape.BLOB)
    }
}
