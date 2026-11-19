package com.bugsy.core

import com.bugsy.core.model.DebugFlag

data class BugsyConfig(
    val trigger: Trigger = Trigger.Shake,
    val shakeThresholdG: Float = DEFAULT_SHAKE_THRESHOLD_G,
    val flagsProvider: () -> List<DebugFlag> = { emptyList() },
    val allowRevealingSecrets: Boolean = true,
    val customInfoProvider: () -> List<Pair<String, String>> = { emptyList() },
    val environment: String? = null,
    val slowRequestThresholdMs: Long = DEFAULT_SLOW_REQUEST_THRESHOLD_MS,
) {
    companion object {
        const val DEFAULT_SHAKE_THRESHOLD_G = 2.5f
        const val DEFAULT_SLOW_REQUEST_THRESHOLD_MS = 2_000L
    }
}
