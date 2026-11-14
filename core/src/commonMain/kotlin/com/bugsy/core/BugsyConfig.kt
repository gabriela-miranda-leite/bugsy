package com.bugsy.core

import com.bugsy.core.model.DebugFlag

data class BugsyConfig(
    val trigger: Trigger = Trigger.Shake,
    val shakeThresholdG: Float = DEFAULT_SHAKE_THRESHOLD_G,
    val flagsProvider: () -> List<DebugFlag> = { emptyList() },
    /**
     * Permite alternar na UI entre os dados mascarados e os valores reais (tokens, senhas) e copiá-los. Os valores só
     * ficam em memória, nunca em disco. Com `false`, tudo existe apenas mascarado.
     */
    val allowRevealingSecrets: Boolean = true
) {
    companion object {
        const val DEFAULT_SHAKE_THRESHOLD_G = 2.5f
    }
}
