package com.nuani.asobiri.commons

/**
 * One installed engine plugin, as the core sees it after scanning packages for
 * [PluginContract.ACTION_LAUNCH].
 *
 * Deliberately a plain value type with zero Android imports. The scan itself —
 * PackageManager queries, `<meta-data>` reads — is an adapter that lives in
 * :app and PRODUCES these; keeping the descriptor Android-free is what lets all
 * resolution logic below run in host unit tests without an emulator.
 */
data class PluginDescriptor(
    val packageName: String,
    val activityClassName: String,
    val engines: List<Engine>,
    val apiVersion: Int,
    /** Human-readable name for the library UI and error dialogs. */
    val label: String,
)

/**
 * Chooses which installed plugin runs a given [Engine].
 *
 * [resolve] and [diagnose] are two faces of one decision and MUST stay in sync:
 * both gate on `apiVersion == [PluginContract.API_VERSION]`. If that predicate
 * ever loosens (see below), change it in both, or a game the UI says is
 * playable will fail to launch — or vice versa.
 */
object PluginResolver {

    /**
     * The best plugin for [engine], or null if none can run it here.
     *
     * Strict version EQUALITY, not a min/max range, while the contract is at
     * version 1: a plugin built for a different [PluginContract.API_VERSION]
     * may omit an extra we now require or send one we can't parse, and failing
     * closed beats handing a game to a plugin we can't fully speak to. Revisit
     * toward a supported-range once a version 2 actually exists to range over.
     *
     * `firstOrNull` means install/query order breaks ties between two plugins
     * that both support [engine] — and that order is arbitrary from
     * PackageManager. Fine while first-party plugins don't overlap; the day a
     * deterministic tie-break is needed (e.g. prefer the first-party package
     * over a third-party one), it goes right here.
     */
    fun resolve(installed: List<PluginDescriptor>, engine: Engine): PluginDescriptor? =
        installed.firstOrNull {
            engine in it.engines && it.apiVersion == PluginContract.API_VERSION
        }

    /**
     * Why a launch of [engine] can't proceed, so the UI can say something
     * actionable instead of a bare "can't play this". Separates "no plugin for
     * this engine — go install one" from "a plugin is installed but built for
     * another contract version — update it", which are different user fixes.
     */
    fun diagnose(installed: List<PluginDescriptor>, engine: Engine): Diagnosis {
        val supporting = installed.filter { engine in it.engines }
        return when {
            supporting.isEmpty() -> Diagnosis.NO_PLUGIN
            supporting.none { it.apiVersion == PluginContract.API_VERSION } ->
                Diagnosis.INCOMPATIBLE_VERSION
            else -> Diagnosis.OK
        }
    }

    enum class Diagnosis { OK, NO_PLUGIN, INCOMPATIBLE_VERSION }
}
