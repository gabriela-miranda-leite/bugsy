package com.bugsy.core

data class BugsyConfig(
    val trigger: Trigger = Trigger.Shake,
    val shakeThresholdG: Float = DEFAULT_SHAKE_THRESHOLD_G
) {
    companion object {
        const val DEFAULT_SHAKE_THRESHOLD_G = 2.5f
    }
}
