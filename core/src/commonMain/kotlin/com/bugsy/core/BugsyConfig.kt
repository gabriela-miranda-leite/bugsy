package com.bugsy.core

import com.bugsy.core.model.DebugFlag

data class BugsyConfig(
    val trigger: Trigger = Trigger.Shake,
    val shakeThresholdG: Float = DEFAULT_SHAKE_THRESHOLD_G,
    val flagsProvider: () -> List<DebugFlag> = { emptyList() },
    /**
     * Lets the UI toggle between masked data and the real values (tokens, passwords) and copy them. The values
     * only live in memory, never on disk. With `false`, everything exists masked only.
     */
    val allowRevealingSecrets: Boolean = true,
    /**
     * Data the consuming app wants shown on the Device tab (e.g. `"user_id" to "u_42"`, `"email" to "dev@app.com"`),
     * displayed in an "Account" section alongside the device/runtime ones. Read again every time the tab is
     * opened — never cached or persisted between sessions.
     */
    val customInfoProvider: () -> List<Pair<String, String>> = { emptyList() },
) {
    companion object {
        const val DEFAULT_SHAKE_THRESHOLD_G = 2.5f
    }
}
