package com.nuani.asobiri.commons

/**
 * The set of game engines Asobiri can route to a plugin.
 *
 * [id] is WIRE FORMAT and therefore contract: plugins advertise the engines
 * they run as a comma-separated list of these ids in a manifest `<meta-data>`
 * (see [PluginContract.META_ENGINES]), and the core matches a detected game
 * against them across the process boundary. Once any plugin has shipped
 * advertising an id, that id is frozen — renaming it silently unpairs every
 * installed plugin from the games it used to run. Add new engines by
 * appending entries; never repurpose an existing id.
 *
 * [displayName] is the opposite: UI-only, not on the wire, safe to reword or
 * localize at any time.
 *
 * Note the enum's declaration order carries NO meaning. Detection precedence
 * — which matters, because game layouts overlap — lives in [EngineDetector],
 * deliberately not here, so reordering this list can't perturb detection.
 */
enum class Engine(val id: String, val displayName: String) {
    RENPY("renpy", "Ren'Py"),
    RPG_MAKER_XP("rpgm-xp", "RPG Maker XP"),
    RPG_MAKER_VX("rpgm-vx", "RPG Maker VX"),
    RPG_MAKER_VX_ACE("rpgm-vxace", "RPG Maker VX Ace"),
    RPG_MAKER_MV("rpgm-mv", "RPG Maker MV"),
    RPG_MAKER_MZ("rpgm-mz", "RPG Maker MZ"),
    TYRANO("tyrano", "TyranoBuilder"),
    GODOT("godot", "Godot"),
    HTML("html", "HTML5"),
    FLASH("flash", "Flash"),
    KIRIKIRI("kirikiri", "KiriKiri"),
    WOLFRPG("wolfrpg", "WOLF RPG Editor"),

    // A Windows program, run through Wine by asobiri-plugin-wine. The id names
    // the kind of game, not the tool, so it stays true if the runtime changes.
    // EngineDetector assigns it only when no native engine claims the game.
    WINDOWS("windows", "Windows"),

    // Diagnostics-only. EngineDetector never emits it, so no real game ever
    // routes here; it exists so a stub plugin can exercise discovery and
    // hand-off without shadowing a real engine's plugin in the resolver.
    FAKE("fake", "Diagnostics");

    companion object {
        fun fromId(id: String): Engine? = entries.firstOrNull { it.id == id }

        /**
         * Parses a [META_ENGINES][PluginContract.META_ENGINES] value.
         *
         * Unknown ids are dropped, not treated as errors: a plugin built
         * against a newer contract may legitimately list engines this copy of
         * :commons predates, and one recognized engine is still worth pairing.
         * Trim + lowercase because the value is hand-authored in a plugin's
         * manifest — tolerate the whitespace and casing a human will get wrong.
         */
        fun parseList(value: String): List<Engine> =
            value.split(',').mapNotNull { fromId(it.trim().lowercase()) }
    }
}
