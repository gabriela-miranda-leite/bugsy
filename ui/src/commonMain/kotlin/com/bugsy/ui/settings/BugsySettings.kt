package com.bugsy.ui.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Preferências de quem usa o overlay. Ficam só em memória, valem enquanto o processo vive e nunca vão para disco
 * (mesma regra dos dados capturados).
 */
object BugsySettings {
    private val _revealSensitiveByDefault = MutableStateFlow(false)

    /** Se as requests abrem mostrando os dados sensíveis (`true`) ou mascarados (`false`, o padrão). */
    val revealSensitiveByDefault: StateFlow<Boolean> = _revealSensitiveByDefault

    fun setRevealSensitiveByDefault(value: Boolean) {
        _revealSensitiveByDefault.value = value
    }

    internal fun reset() {
        _revealSensitiveByDefault.value = false
    }
}
