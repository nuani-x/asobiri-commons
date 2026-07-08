package com.nuani.asobiri.commons

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchConfigTest {

    @Test
    fun `round-trips through json`() {
        val config = LaunchConfig(locale = "en", keepScreenOn = false, displayScaling = "stretch")
        assertEquals(config, LaunchConfig.fromJson(config.toJson()))
    }

    @Test
    fun `displayScaling defaults to the never-distorting fit mode`() {
        assertEquals("fit", LaunchConfig().displayScaling)
    }

    @Test
    fun `unknown keys from a newer sender are ignored`() {
        // Forward compatibility: a newer core app adds fields; an older
        // plugin decoding with this contract version must not crash.
        val json = """{"locale":"pt","keepScreenOn":true,"futureField":42}"""
        assertEquals(LaunchConfig(), LaunchConfig.fromJson(json))
    }

    @Test
    fun `missing keys from an older sender take defaults`() {
        val json = """{"locale":"en"}"""
        val decoded = LaunchConfig.fromJson(json)
        assertEquals("en", decoded.locale)
        assertEquals(true, decoded.keepScreenOn)
    }

    @Test
    fun `defaults are encoded explicitly for older receivers`() {
        val json = LaunchConfig().toJson()
        // encodeDefaults=true means every field is always on the wire.
        assert(json.contains("locale"))
        assert(json.contains("keepScreenOn"))
        assert(json.contains("displayScaling"))
    }
}
