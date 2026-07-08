package com.nuani.asobiri.commons

import android.content.Intent

/**
 * The one place a [LaunchRequest] is marshalled to the cross-process intent
 * and back. Both sides of the boundary call it — the core to [build], the
 * plugin to [parse] — so the extra keys and JSON encoding are defined once and
 * can never drift between an app and a plugin shipped at different times.
 *
 * Everything travels as string/int extras plus one JSON blob, never a
 * Parcelable: see [PluginContract] for why that coupling is the thing this
 * whole plugin model exists to avoid.
 */
object LaunchIntent {

    /**
     * Builds the explicit intent that starts [target]'s launcher activity.
     *
     * Explicit component (not just the action) on purpose: the core has
     * already resolved exactly one plugin via [PluginResolver], so it addresses
     * that package directly rather than re-opening a chooser the resolution
     * step just closed.
     */
    fun build(target: PluginDescriptor, request: LaunchRequest): Intent =
        Intent(PluginContract.ACTION_LAUNCH).apply {
            setClassName(target.packageName, target.activityClassName)
            putExtra(PluginContract.EXTRA_GAME_ID, request.gameId)
            putExtra(PluginContract.EXTRA_GAME_PATH, request.gamePath)
            putExtra(PluginContract.EXTRA_ENGINE, request.engine.id)
            putExtra(PluginContract.EXTRA_CONFIG, request.config.toJson())
            putExtra(PluginContract.EXTRA_API_VERSION, PluginContract.API_VERSION)
        }

    /**
     * Reconstructs the request on the plugin side, or null if a required extra
     * is absent or unparseable.
     *
     * Returning null rather than throwing is deliberate: the activity is
     * `exported`, so this runs on genuinely untrusted input (any app can start
     * it with garbage extras). A malformed launch is a normal case to reject
     * with a clean error, not an exception to crash on. [config] alone falls
     * back to defaults instead of failing the whole parse — a bad options blob
     * shouldn't stop a game whose identity and path are valid.
     */
    fun parse(intent: Intent): LaunchRequest? {
        val gameId = intent.getStringExtra(PluginContract.EXTRA_GAME_ID) ?: return null
        val gamePath = intent.getStringExtra(PluginContract.EXTRA_GAME_PATH) ?: return null
        val engine = intent.getStringExtra(PluginContract.EXTRA_ENGINE)
            ?.let(Engine::fromId) ?: return null
        val config = intent.getStringExtra(PluginContract.EXTRA_CONFIG)
            ?.let { runCatching { LaunchConfig.fromJson(it) }.getOrNull() }
            ?: LaunchConfig()
        return LaunchRequest(gameId, gamePath, engine, config)
    }
}
