package com.nuani.asobiri.commons

/**
 * Everything a plugin needs to run one game, before it becomes an [android
 * .content.Intent]. Kept as a plain value type (no Android) so the core can
 * build and reason about a launch in host tests; [LaunchIntent] is the single
 * point that turns this into the actual cross-process intent and back.
 */
data class LaunchRequest(
    val gameId: String,
    val gamePath: String,
    val engine: Engine,
    val config: LaunchConfig = LaunchConfig(),
)
