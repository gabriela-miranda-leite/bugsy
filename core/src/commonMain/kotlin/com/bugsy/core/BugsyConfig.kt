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
    val allowRevealingSecrets: Boolean = true,
    /**
     * Dados que o app consumidor quer ver na aba Device (ex.: `"user_id" to "u_42"`, `"email" to "dev@app.com"`),
     * mostrados numa seção "Account" junto com as de device/runtime. Lidos de novo toda vez que a aba é aberta —
     * não ficam em cache nem persistem entre sessões.
     */
    val customInfoProvider: () -> List<Pair<String, String>> = { emptyList() },
) {
    companion object {
        const val DEFAULT_SHAKE_THRESHOLD_G = 2.5f
    }
}
