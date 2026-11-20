package com.liquidwidgets.app

import com.liquidwidgets.app.data.providers.JBool
import com.liquidwidgets.app.data.providers.JObj
import com.liquidwidgets.app.data.providers.MiniJson
import com.liquidwidgets.app.data.providers.WeatherCondition
import com.liquidwidgets.app.data.providers.WeatherDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherDataProviderTest {

    @Test
    fun `cache TTL is 30 minutes`() {
        val now = 1_000_000_000L
        assertTrue(WeatherDataProvider.isCacheFresh(now - 29 * 60_000L, now))
        assertFalse(WeatherDataProvider.isCacheFresh(now - 31 * 60_000L, now))
        assertFalse(WeatherDataProvider.isCacheFresh(0L, now))
    }

    @Test
    fun `wmo codes map to conditions`() {
        assertEquals(WeatherCondition.CLEAR to "Clear Sky", WeatherDataProvider.mapWmoCode(0))
        assertEquals(WeatherCondition.RAIN to "Rain Showers", WeatherDataProvider.mapWmoCode(61))
        assertEquals(WeatherCondition.SNOW to "Snowfall", WeatherDataProvider.mapWmoCode(71))
        assertEquals(WeatherCondition.STORM to "Thunderstorm", WeatherDataProvider.mapWmoCode(95))
    }

    @Test
    fun `parses open-meteo json without fake defaults`() {
        val json = """{"current":{"temperature_2m":18.5,"relative_humidity_2m":60,"weather_code":0,"wind_speed_10m":12.0},"hourly":{"time":["2026-11-21T00:00","2026-11-21T01:00","2026-11-21T02:00","2026-11-21T03:00"],"temperature_2m":[18,17,17,16],"weather_code":[0,0,1,1]},"_label":"Local Weather"}"""
        val snap = WeatherDataProvider.parseOpenMeteoJson(json, isCached = false)
        assertNotNull(snap)
        assertEquals(18, snap!!.tempC)
        assertEquals(WeatherCondition.CLEAR, snap.condition)
        assertEquals("Local Weather", snap.locationLabel)
        assertEquals(4, snap.hourly.size)
    }

    @Test
    fun `malformed json returns null instead of fake weather`() {
        assertEquals(null, WeatherDataProvider.parseOpenMeteoJson("not json", isCached = false))
        assertEquals(null, WeatherDataProvider.parseOpenMeteoJson("{}", isCached = false))
    }

    @Test
    fun `mini json parses nested objects arrays and escapes`() {
        val parsed = MiniJson.parse("""{"a":{"b":[1,2.5,"x\n"],"c":true},"d":null}""")
        assertNotNull(parsed)
        val root = parsed as JObj
        assertEquals(2.5, root.obj("a")!!.arr("b")!!.numAt(1)!!, 0.001)
        assertEquals("x\n", root.obj("a")!!.arr("b")!!.strAt(2))
        assertEquals(true, (root.obj("a")!!.map["c"] as JBool).v)
        assertEquals(null, MiniJson.parse("{bad"))
        assertEquals(null, MiniJson.parse(""))
    }
}
