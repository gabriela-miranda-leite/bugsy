package com.bugsy.core

import com.bugsy.core.model.DebugFlag

data class BugsyConfig(
    val trigger: Trigger = Trigger.Shake,
    val shakeThresholdG: Float = DEFAULT_SHAKE_THRESHOLD_G,
    val flagsProvider: () -> List<DebugFlag> = { emptyList() },
    /**
     * Permite copiar o cURL com os valores reais (tokens, senhas) por uma ação explícita na UI. Os valores só ficam
     * em memória, nunca em disco. Com `false`, o cURL só existe mascarado.
     */
    val allowCopyingSecrets: Boolean = true
) {
    companion object {
        const val DEFAULT_SHAKE_THRESHOLD_G = 2.5f
    }
}
