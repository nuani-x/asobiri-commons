package com.nuani.asobiri.commons

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LaunchIntentTest {

    private val target = PluginDescriptor(
        packageName = "com.nuani.asobiri.renpy",
        activityClassName = "com.nuani.asobiri.renpy.LauncherActivity",
        engines = listOf(Engine.RENPY),
        apiVersion = PluginContract.API_VERSION,
        label = "Ren'Py",
    )

    private val request = LaunchRequest(
        gameId = "game-42",
        gamePath = "/storage/emulated/0/Asobiri/games/game-42",
        engine = Engine.RENPY,
        config = LaunchConfig(locale = "en", keepScreenOn = false),
    )

    @Test
    fun `build then parse recovers the request`() {
        val recovered = LaunchIntent.parse(LaunchIntent.build(target, request))
        assertEquals(request, recovered)
    }

    @Test
    fun `build addresses the resolved plugin component`() {
        val intent = LaunchIntent.build(target, request)
        assertEquals(PluginContract.ACTION_LAUNCH, intent.action)
        assertEquals(target.packageName, intent.component?.packageName)
        assertEquals(target.activityClassName, intent.component?.className)
        assertEquals(
            PluginContract.API_VERSION,
            intent.getIntExtra(PluginContract.EXTRA_API_VERSION, -1),
        )
    }

    @Test
    fun `parse returns null when a required extra is missing`() {
        // A game id but nothing else — the kind of half-formed intent an
        // untrusted caller could send to the exported activity.
        val intent = Intent(PluginContract.ACTION_LAUNCH)
            .putExtra(PluginContract.EXTRA_GAME_ID, "game-42")
        assertNull(LaunchIntent.parse(intent))
    }

    @Test
    fun `parse returns null for an unknown engine id`() {
        val intent = LaunchIntent.build(target, request)
            .putExtra(PluginContract.EXTRA_ENGINE, "unreal5")
        assertNull(LaunchIntent.parse(intent))
    }

    @Test
    fun `parse falls back to default config when the blob is malformed`() {
        // Identity and path are valid; only the options JSON is garbage. The
        // launch should still parse, with defaults, rather than fail whole.
        val intent = LaunchIntent.build(target, request)
            .putExtra(PluginContract.EXTRA_CONFIG, "{not json")
        val recovered = LaunchIntent.parse(intent)
        assertEquals(LaunchConfig(), recovered?.config)
        assertEquals(request.gameId, recovered?.gameId)
    }
}
