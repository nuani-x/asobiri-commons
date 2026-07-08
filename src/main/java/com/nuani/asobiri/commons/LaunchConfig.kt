package com.nuani.asobiri.commons

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Per-launch options the core hands a plugin, carried as JSON in
 * [PluginContract.EXTRA_CONFIG].
 *
 * This type is the contract's designated growth seam. It exists as a JSON blob
 * rather than a fan of individual typed extras precisely so the payload can
 * evolve WITHOUT touching [PluginContract] or moving [PluginContract.API_VERSION]:
 * both sides share this one serialization point through :commons, and the two
 * codec flags below make evolution safe in both directions across independently
 * updated APKs:
 *
 *  - `ignoreUnknownKeys` → forward compatible. A newer core adds a field; an
 *    older plugin decoding with an older :commons skips the key instead of
 *    throwing.
 *  - `encodeDefaults` → backward compatible. Every field is always written, so
 *    an older core talking to a newer plugin never leaves a field absent for
 *    the newer side to misread — the wire always carries an explicit value.
 *
 * The rule that keeps this working: add fields, each with a default, and never
 * remove one or change what its default MEANS without an [PluginContract.API_VERSION]
 * bump. An old plugin bakes in the old default; repurposing it silently changes
 * behavior on every device that hasn't updated both APKs together.
 */
@Serializable
data class LaunchConfig(
    /** BCP-47 tag the plugin should prefer for its own UI chrome. */
    val locale: String = "pt",
    /** Hold a wake lock on the display while the game runs. */
    val keepScreenOn: Boolean = true,
    /**
     * How a plugin's render surface should map its content to the screen:
     * `"fit"` preserves the original aspect ratio (letterboxed if needed),
     * `"stretch"` fills the screen ignoring aspect ratio. A plugin that
     * doesn't recognize the value (a newer mode from a future core) MUST
     * fall back to `"fit"` — the always-safe, never-distorting choice.
     */
    val displayScaling: String = "fit",
) {
    fun toJson(): String = codec.encodeToString(serializer(), this)

    companion object {
        // A private codec, not Json.Default: the two flags above are part of
        // the compatibility contract, so they must not be at the mercy of a
        // caller's Json instance. See the class KDoc for why each is required.
        private val codec = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun fromJson(json: String): LaunchConfig =
            codec.decodeFromString(serializer(), json)
    }
}
