package com.nuani.asobiri.commons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EngineDetectorTest {

    @Test
    fun `detects renpy from rpa archive under game dir`() {
        val paths = listOf("game/archive.rpa", "game/cache/shaders.txt", "lib/py3-linux-x86_64/renpy")
        assertEquals(Engine.RENPY, EngineDetector.detect(paths))
    }

    @Test
    fun `detects renpy from compiled scripts`() {
        val paths = listOf("game/script.rpyc", "game/options.rpy")
        assertEquals(Engine.RENPY, EngineDetector.detect(paths))
    }

    @Test
    fun `detects renpy from sdk runtime dir`() {
        val paths = listOf("renpy/common/00start.rpy", "launcher.py")
        assertEquals(Engine.RENPY, EngineDetector.detect(paths))
    }

    @Test
    fun `detects rpg maker mv browser deploy over generic html`() {
        // MV always ships an index.html — the HTML fallback must not win.
        val paths = listOf("index.html", "js/rpg_core.js", "js/plugins.js", "audio/bgm/theme.ogg")
        assertEquals(Engine.RPG_MAKER_MV, EngineDetector.detect(paths))
    }

    @Test
    fun `detects rpg maker mv desktop deploy under www`() {
        val paths = listOf("game.exe", "www/index.html", "www/js/rpg_core.js")
        assertEquals(Engine.RPG_MAKER_MV, EngineDetector.detect(paths))
    }

    @Test
    fun `detects rpg maker mz from rmmz core`() {
        val paths = listOf("index.html", "js/rmmz_core.js", "js/main.js")
        assertEquals(Engine.RPG_MAKER_MZ, EngineDetector.detect(paths))
    }

    @Test
    fun `detects rpg maker mz from project file`() {
        val paths = listOf("game.rmmzproject", "data/system.json")
        assertEquals(Engine.RPG_MAKER_MZ, EngineDetector.detect(paths))
    }

    @Test
    fun `detects vx ace from rvdata2 not plain vx`() {
        val paths = listOf("game.exe", "data/actors.rvdata2", "data/map001.rvdata2")
        assertEquals(Engine.RPG_MAKER_VX_ACE, EngineDetector.detect(paths))
    }

    @Test
    fun `detects vx from rvdata`() {
        val paths = listOf("game.exe", "data/actors.rvdata")
        assertEquals(Engine.RPG_MAKER_VX, EngineDetector.detect(paths))
    }

    @Test
    fun `detects xp from rxdata`() {
        val paths = listOf("game.exe", "data/actors.rxdata", "game.rxproj")
        assertEquals(Engine.RPG_MAKER_XP, EngineDetector.detect(paths))
    }

    @Test
    fun `detects packed releases from their rgss archive`() {
        // Released games ship one encrypted archive instead of loose Data/*
        // files — the common case for anything downloaded rather than built.
        assertEquals(
            Engine.RPG_MAKER_VX_ACE,
            EngineDetector.detect(listOf("game.exe", "game.ini", "game.rgss3a")),
        )
        assertEquals(
            Engine.RPG_MAKER_VX,
            EngineDetector.detect(listOf("game.exe", "game.rgss2a")),
        )
        assertEquals(
            Engine.RPG_MAKER_XP,
            EngineDetector.detect(listOf("game.exe", "game.rgssad")),
        )
    }

    @Test
    fun `detects kirikiri from the canonical data xp3 distribution`() {
        val paths = listOf("game.exe", "data.xp3", "patch.xp3", "readme.txt")
        assertEquals(Engine.KIRIKIRI, EngineDetector.detect(paths))
    }

    @Test
    fun `detects kirikiri from any root xp3 asset pack`() {
        // KAG games mount extra archives (bgm.xp3, scenario.xp3, ...) from
        // script — a root .xp3 is a KiriKiri signal even without data.xp3.
        val paths = listOf("game.exe", "scenario.xp3", "bgm.xp3")
        assertEquals(Engine.KIRIKIRI, EngineDetector.detect(paths))
    }

    @Test
    fun `nested xp3 does not mean kirikiri`() {
        val paths = listOf("backup/data.xp3", "readme.txt")
        assertNull(EngineDetector.detect(paths))
    }

    @Test
    fun `detects unpacked kirikiri from root startup script`() {
        val paths = listOf("startup.tjs", "system/Initialize.tjs", "scenario/first.ks")
        assertEquals(Engine.KIRIKIRI, EngineDetector.detect(paths))
    }

    @Test
    fun `unpacked kirikiri under data wins over the tyrano ks rule`() {
        // An exe+data KiriKiri distribution carries data/scenario/*.ks —
        // exactly Tyrano's signature, since Tyrano cloned KAG's layout. The
        // boot script decides: only KiriKiri requires startup.tjs.
        val paths = listOf(
            "game.exe",
            "data/startup.tjs",
            "data/system/Initialize.tjs",
            "data/scenario/first.ks",
        )
        assertEquals(Engine.KIRIKIRI, EngineDetector.detect(paths))
    }

    @Test
    fun `an encrypted wolf release with its exe goes to windows`() {
        // The native WOLF engine reads decrypted data only; the game's own
        // Game.exe opens its archives under Wine.
        val paths = listOf("game.exe", "config.exe", "data.wolf", "gurugurusmf4.dll")
        assertEquals(Engine.WINDOWS, EngineDetector.detect(paths))
    }

    @Test
    fun `a split encrypted wolf release with its exe goes to windows`() {
        val paths = listOf("game.exe", "data/data0.wolf", "data/data1.wolf")
        assertEquals(Engine.WINDOWS, EngineDetector.detect(paths))
    }

    @Test
    fun `an encrypted wolf release without an exe stays wolf rpg`() {
        // Nothing for Wine to run; the native plugin reports what is missing.
        assertEquals(Engine.WOLFRPG, EngineDetector.detect(listOf("data.wolf", "gurugurusmf4.dll")))
    }

    @Test
    fun `detects a decrypted wolf rpg game from basicdata`() {
        val paths = listOf("game.exe", "data/basicdata/game.dat", "data/basicdata/map000.mps")
        assertEquals(Engine.WOLFRPG, EngineDetector.detect(paths))
    }

    @Test
    fun `decrypted data wins over the encrypted archive left beside it`() {
        val paths = listOf("game.exe", "data.wolf", "data/basicdata/game.dat")
        assertEquals(Engine.WOLFRPG, EngineDetector.detect(paths))
    }

    @Test
    fun `a bare root exe goes to windows`() {
        // No native engine claims it, so Wine runs it.
        assertEquals(Engine.WINDOWS, EngineDetector.detect(listOf("game.exe", "readme.txt")))
    }

    @Test
    fun `an rpg maker 2003 game goes to windows`() {
        val paths = listOf("rpg_rt.exe", "rpg_rt.ldb", "rpg_rt.lmt", "map0001.lmu")
        assertEquals(Engine.WINDOWS, EngineDetector.detect(paths))
    }

    @Test
    fun `native engines win over the exe their windows export ships`() {
        assertEquals(Engine.RENPY, EngineDetector.detect(listOf("game.exe", "game/archive.rpa")))
        assertEquals(Engine.RPG_MAKER_MV, EngineDetector.detect(listOf("game.exe", "www/js/rpg_core.js")))
        assertEquals(Engine.KIRIKIRI, EngineDetector.detect(listOf("krkr.exe", "data.xp3")))
    }

    @Test
    fun `a root html page wins over a root exe`() {
        // An NW.js or Electron wrapper around a plain web game runs in the
        // WebView, not under Wine.
        assertEquals(Engine.HTML, EngineDetector.detect(listOf("nw.exe", "index.html")))
    }

    @Test
    fun `a nested exe does not mean windows`() {
        // Installers and tools in a subfolder are not the game.
        assertNull(EngineDetector.detect(listOf("redist/vcredist_x86.exe", "readme.txt")))
    }

    @Test
    fun `detects tyrano from scenario scripts`() {
        val paths = listOf("index.html", "data/scenario/first.ks", "tyrano/tyrano.js")
        assertEquals(Engine.TYRANO, EngineDetector.detect(paths))
    }

    @Test
    fun `detects godot from root pck`() {
        val paths = listOf("game.pck", "game.exe")
        assertEquals(Engine.GODOT, EngineDetector.detect(paths))
    }

    @Test
    fun `nested pck does not mean godot`() {
        // A stray .pck in an asset subfolder isn't a Godot layout signal.
        val paths = listOf("assets/textures/pack.pck", "index.html")
        assertEquals(Engine.HTML, EngineDetector.detect(paths))
    }

    @Test
    fun `plain html game falls back to html engine`() {
        val paths = listOf("index.html", "css/style.css", "js/game.js")
        assertEquals(Engine.HTML, EngineDetector.detect(paths))
    }

    @Test
    fun `detects flash from a root swf`() {
        assertEquals(Engine.FLASH, EngineDetector.detect(listOf("game.swf")))
    }

    @Test
    fun `root swf wins over its html wrapper page`() {
        // Distribution wrappers embed the movie for a browser Flash plugin
        // that no longer exists; the player must boot the .swf directly.
        val paths = listOf("index.html", "game.swf", "readme.txt")
        assertEquals(Engine.FLASH, EngineDetector.detect(paths))
    }

    @Test
    fun `nested swf inside an html game does not mean flash`() {
        val paths = listOf("index.html", "assets/intro.swf", "js/game.js")
        assertEquals(Engine.HTML, EngineDetector.detect(paths))
    }

    @Test
    fun `unknown folder returns null`() {
        val paths = listOf("readme.txt", "photos/img001.jpg")
        assertNull(EngineDetector.detect(paths))
    }

    @Test
    fun `empty listing returns null`() {
        assertNull(EngineDetector.detect(emptyList()))
    }

    @Test
    fun `windows separators and mixed case are normalized`() {
        val paths = listOf("Game\\Archive.RPA", "Game\\Script.rpyc")
        assertEquals(Engine.RENPY, EngineDetector.detect(paths))
    }
}
