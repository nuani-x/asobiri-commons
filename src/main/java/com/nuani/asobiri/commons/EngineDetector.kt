package com.nuani.asobiri.commons

/**
 * Guesses which [Engine] built a game folder from its file listing alone.
 *
 * Each rule keys off a real distribution artifact — a runtime dir, a project
 * file, or an engine-specific data extension that ships in every export of
 * that engine. Pure logic over relative paths (no filesystem, no Android) so
 * the entire decision table is exercised in host unit tests; the caller walks
 * the folder and passes paths relative to its root.
 *
 * This is a HEURISTIC, and deliberately a cheap one: it returns the first
 * plausible engine by precedence, not a confidence score. The plugin is the
 * final authority — a misdetected game simply fails to load there, and the
 * core surfaces that. So the cost of a wrong guess is a clear error, not a
 * crash, which is why a fast first-match beats an elaborate scorer here.
 *
 * RULE ORDER IS LOAD-BEARING. Game layouts overlap: every RPG Maker MV/MZ
 * export ships an index.html, so the generic [isHtml] rule must come last or
 * it would swallow them. MZ is checked before MV, and VX Ace before VX, for
 * the same reason — most specific signature first. Reorder with care.
 */
object EngineDetector {

    /**
     * How deep the caller must walk for every rule below to have its evidence.
     * Bounded by the deepest signature in the table — `www/js/rpg_core.js` and
     * a `.ks` under `data/scenario/`, both at depth 3. Adding a deeper
     * signature means bumping this AND the caller's walk, or that rule
     * silently never fires.
     */
    const val MAX_SCAN_DEPTH = 3

    fun detect(relativePaths: List<String>): Engine? {
        // Normalize once, up front, so every rule can assume forward slashes
        // and lowercase and stay a plain suffix/prefix test. Games are
        // authored on Windows (backslashes) and casing is inconsistent across
        // archives, so doing this per-rule would be both slower and error-prone.
        val paths = relativePaths.map { it.replace('\\', '/').lowercase() }
        return when {
            isRenpy(paths)        -> Engine.RENPY
            isRpgMakerMz(paths)   -> Engine.RPG_MAKER_MZ
            isRpgMakerMv(paths)   -> Engine.RPG_MAKER_MV
            isRpgMakerVxAce(paths)-> Engine.RPG_MAKER_VX_ACE
            isRpgMakerVx(paths)   -> Engine.RPG_MAKER_VX
            isRpgMakerXp(paths)   -> Engine.RPG_MAKER_XP
            isKirikiri(paths)     -> Engine.KIRIKIRI
            isWolf(paths)         -> Engine.WOLFRPG
            isTyrano(paths)       -> Engine.TYRANO
            isGodot(paths)        -> Engine.GODOT
            isFlash(paths)        -> Engine.FLASH
            isHtml(paths)         -> Engine.HTML
            else                  -> null
        }
    }

    // Ren'Py keeps scripts and archives under game/; .rpa (packed) and .rpyc
    // (compiled) are always present, .rpy (source) only in unpacked builds. A
    // bare renpy/ runtime dir identifies an SDK-style tree with no game/ yet.
    private fun isRenpy(paths: List<String>) = paths.any {
        (it.startsWith("game/") &&
            (it.endsWith(".rpa") || it.endsWith(".rpyc") || it.endsWith(".rpy"))) ||
            it.startsWith("renpy/")
    }

    // MV and MZ are indistinguishable by folder shape — both carry index.html
    // and a js/ dir, at the root (browser export) or under www/ (desktop
    // export). Only the core runtime filename separates them, so match on that.
    private fun isRpgMakerMz(paths: List<String>) = paths.any {
        it.endsWith("js/rmmz_core.js") || it.endsWith(".rmmzproject")
    }

    private fun isRpgMakerMv(paths: List<String>) = paths.any {
        it.endsWith("js/rpg_core.js") || it.endsWith(".rpgproject")
    }

    // XP/VX/VXAce encode their exact version in the data extension: .rvdata2 is
    // VX Ace only, .rvdata is VX only, .rxdata is XP only. Released games
    // usually pack all data files into a single encrypted archive instead
    // (.rgssad = XP, .rgss2a = VX, .rgss3a = VX Ace), so each rule accepts
    // both forms. The suffixes don't overlap, but keeping most-specific-first
    // (Ace before VX) makes it safe to add a looser sibling rule later
    // without re-auditing the order.
    private fun isRpgMakerVxAce(paths: List<String>) = paths.any {
        it.endsWith(".rvproj2") || it.endsWith(".rvdata2") || it.endsWith(".rgss3a")
    }

    private fun isRpgMakerVx(paths: List<String>) = paths.any {
        it.endsWith(".rvproj") || it.endsWith(".rvdata") || it.endsWith(".rgss2a")
    }

    private fun isRpgMakerXp(paths: List<String>) = paths.any {
        it.endsWith(".rxproj") || it.endsWith(".rxdata") || it.endsWith(".rgssad")
    }

    // KiriKiri ships as root-level XP3 archives (data.xp3 plus patch/asset
    // packs; the extension belongs to this engine family alone, and a nested
    // archive is game data, not a game) or unpacked with the loader's
    // mandatory boot script at the project root or under data/. Must precede
    // isTyrano: Tyrano cloned KAG's layout, so an unpacked KiriKiri tree also
    // carries data/scenario/*.ks and the looser Tyrano rule would swallow it.
    private fun isKirikiri(paths: List<String>) = paths.any {
        (it.endsWith(".xp3") && '/' !in it) ||
            it == "startup.tjs" || it == "data/startup.tjs"
    }

    // WOLF RPG ships the encrypted archive as Data.wolf at the root (or split
    // into Data/*.wolf), alongside the GuruguruSMF4.dll MIDI player that no
    // other engine bundles; a decrypted game exposes BasicData/Game.dat
    // instead. The .wolf extension and that dll are WOLF's alone, so either
    // pins the engine. Deliberately NOT keyed on a bare .exe — WOLF, NW.js, and
    // classic RPG Maker all ship one, so it identifies nothing.
    private fun isWolf(paths: List<String>) = paths.any {
        it == "data.wolf" ||
            (it.startsWith("data/") && it.endsWith(".wolf")) ||
            it == "gurugurusmf4.dll" ||
            it.endsWith("basicdata/game.dat")
    }

    // Tyrano ships its runtime in tyrano/ and its script (.ks) under
    // data/scenario/. Either signal alone is enough; the scenario path is the
    // more reliable one since some builds rename the runtime dir.
    private fun isTyrano(paths: List<String>) = paths.any {
        it.startsWith("tyrano/") || (it.startsWith("data/scenario/") && it.endsWith(".ks"))
    }

    // A Godot export is a project.godot (source tree) or a single .pck pack at
    // the ROOT. The `'/' !in it` guard is the point: a .pck nested in an asset
    // folder is a texture/atlas pack, not a Godot game, so it must not match.
    private fun isGodot(paths: List<String>) = paths.any {
        it == "project.godot" || (it.endsWith(".pck") && '/' !in it)
    }

    // A Flash game is a .swf at the ROOT. Nested .swf files are common inside
    // HTML5/RPG Maker games as leftover assets, so they must not match. Wins
    // over isHtml on purpose: distribution wrappers (an index.html embedding
    // the movie) expect a browser Flash plugin that no longer exists — the
    // player boots the .swf directly instead.
    private fun isFlash(paths: List<String>) = paths.any {
        it.endsWith(".swf") && '/' !in it
    }

    // Weakest signal, and only reachable once every engine that embeds an
    // index.html has been ruled out above — hence a root index.html here really
    // is a plain web game, not an RPG Maker export in disguise.
    private fun isHtml(paths: List<String>) = "index.html" in paths
}
