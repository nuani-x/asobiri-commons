package com.nuani.asobiri.commons

/**
 * The wire contract between the Asobiri core app and an engine plugin.
 *
 * A plugin is a separately installed APK that exposes one launcher activity:
 *
 * ```xml
 * <activity android:name=".RenpyLauncherActivity" android:exported="true">
 *     <intent-filter>
 *         <action android:name="com.nuani.asobiri.plugin.LAUNCH" />
 *         <category android:name="android.intent.category.DEFAULT" />
 *     </intent-filter>
 *     <!-- ON THE ACTIVITY, not <application>: queryIntentActivities(GET_META_DATA)
 *          fills ActivityInfo.metaData from the matched component only. -->
 *     <meta-data android:name="com.nuani.asobiri.plugin.ENGINES"     android:value="renpy" />
 *     <meta-data android:name="com.nuani.asobiri.plugin.API_VERSION" android:value="1" />
 * </activity>
 * ```
 *
 * The core discovers plugins by resolving [ACTION_LAUNCH] (which requires a
 * matching `<queries><intent>` block in the core's own manifest for package
 * visibility on Android 11+ — without it the query silently returns nothing),
 * reads the activity's [META_ENGINES]/[META_API_VERSION], filters by version,
 * then starts the activity with the EXTRA_* payload below.
 *
 * TWO invariants hold this together:
 *
 *  1. Entries here may be ADDED but never renamed or repurposed after a plugin
 *     has shipped. Payload evolution is additive and happens inside
 *     [LaunchConfig]'s JSON, NOT by introducing new required extras — that way
 *     [API_VERSION] only has to move on a genuine break.
 *
 *  2. The payload is intentionally primitives-plus-one-JSON-blob, no
 *     Parcelable. Extras cross a Binder boundary between two independently
 *     versioned APKs; a shared Parcelable would force both sides to hold the
 *     class in lockstep, which is exactly the coupling this plugin model
 *     exists to avoid. Strings marshal the same on every Android version.
 *
 * SECURITY: the launcher activity is `exported`, so any app on the device can
 * start it with a forged [EXTRA_GAME_PATH]. A plugin MUST treat every extra as
 * untrusted — canonicalize the path and confirm it stays within the sandbox it
 * expects before reading a byte. The core sending a good value is not a
 * guarantee the plugin received one.
 */
object PluginContract {

    /**
     * Bumped ONLY on a breaking change to the launch payload (an extra removed,
     * a meaning changed). Additive change goes through [LaunchConfig] instead.
     * [PluginResolver] refuses any plugin whose [META_API_VERSION] differs.
     */
    const val API_VERSION = 1

    const val ACTION_LAUNCH = "com.nuani.asobiri.plugin.LAUNCH"

    /** `<meta-data>`: comma-separated [Engine.id] list the plugin can run. */
    const val META_ENGINES = "com.nuani.asobiri.plugin.ENGINES"

    /** `<meta-data>`: the [API_VERSION] the plugin was compiled against. */
    const val META_API_VERSION = "com.nuani.asobiri.plugin.API_VERSION"

    /** String extra — absolute path of the game folder on shared storage. */
    const val EXTRA_GAME_PATH = "com.nuani.asobiri.plugin.extra.GAME_PATH"

    /** String extra — stable id the core assigned this game; scopes saves/prefs. */
    const val EXTRA_GAME_ID = "com.nuani.asobiri.plugin.extra.GAME_ID"

    /** String extra — the detected [Engine.id] the plugin should interpret as. */
    const val EXTRA_ENGINE = "com.nuani.asobiri.plugin.extra.ENGINE"

    /** String extra — JSON-serialized [LaunchConfig]. */
    const val EXTRA_CONFIG = "com.nuani.asobiri.plugin.extra.CONFIG"

    /**
     * Int extra — the [API_VERSION] the core spoke when launching. Redundant
     * with [META_API_VERSION] on the happy path, but it lets the plugin detect
     * a downgraded/incompatible caller at runtime instead of trusting that
     * discovery already filtered correctly.
     */
    const val EXTRA_API_VERSION = "com.nuani.asobiri.plugin.extra.API_VERSION"
}
