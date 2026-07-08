package com.nuani.asobiri.commons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PluginResolverTest {

    private fun plugin(
        pkg: String,
        engines: List<Engine>,
        apiVersion: Int = PluginContract.API_VERSION,
    ) = PluginDescriptor(
        packageName = pkg,
        activityClassName = "$pkg.LauncherActivity",
        engines = engines,
        apiVersion = apiVersion,
        label = pkg.substringAfterLast('.'),
    )

    @Test
    fun `resolves the plugin that supports the engine`() {
        val renpy = plugin("com.nuani.asobiri.renpy", listOf(Engine.RENPY))
        val rpgm = plugin("com.nuani.asobiri.rpgm", listOf(Engine.RPG_MAKER_MV, Engine.RPG_MAKER_MZ))

        assertEquals(renpy, PluginResolver.resolve(listOf(renpy, rpgm), Engine.RENPY))
        assertEquals(rpgm, PluginResolver.resolve(listOf(renpy, rpgm), Engine.RPG_MAKER_MZ))
    }

    @Test
    fun `skips plugin built for a different api version`() {
        val stale = plugin("com.nuani.asobiri.renpy", listOf(Engine.RENPY), apiVersion = 99)
        assertNull(PluginResolver.resolve(listOf(stale), Engine.RENPY))
    }

    @Test
    fun `returns null when nothing supports the engine`() {
        val renpy = plugin("com.nuani.asobiri.renpy", listOf(Engine.RENPY))
        assertNull(PluginResolver.resolve(listOf(renpy), Engine.GODOT))
    }

    @Test
    fun `first match wins when two plugins support the engine`() {
        val first = plugin("com.nuani.asobiri.renpy", listOf(Engine.RENPY))
        val second = plugin("com.example.thirdparty.renpy", listOf(Engine.RENPY))
        assertEquals(first, PluginResolver.resolve(listOf(first, second), Engine.RENPY))
    }

    @Test
    fun `diagnose distinguishes missing plugin from incompatible version`() {
        val stale = plugin("com.nuani.asobiri.renpy", listOf(Engine.RENPY), apiVersion = 99)
        val ok = plugin("com.nuani.asobiri.rpgm", listOf(Engine.RPG_MAKER_MV))

        assertEquals(
            PluginResolver.Diagnosis.NO_PLUGIN,
            PluginResolver.diagnose(listOf(ok), Engine.RENPY),
        )
        assertEquals(
            PluginResolver.Diagnosis.INCOMPATIBLE_VERSION,
            PluginResolver.diagnose(listOf(stale), Engine.RENPY),
        )
        assertEquals(
            PluginResolver.Diagnosis.OK,
            PluginResolver.diagnose(listOf(ok), Engine.RPG_MAKER_MV),
        )
    }
}
