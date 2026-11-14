package com.bugsy.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * API idêntica à do artefato de debug, mas sem nenhum comportamento: nada é capturado, armazenado,
 * registrado nem logado, e o overlay nunca abre.
 */
object Bugsy {
    val version: String = "noop"

    val isVisible: StateFlow<Boolean> = MutableStateFlow(false)

    val flagManager: FlagManager = FlagManager()

    fun initialize(config: BugsyConfig = BugsyConfig()) = Unit

    fun destroy() = Unit

    fun show() = Unit

    fun hide() = Unit

    fun addSensitiveHeader(name: String) = Unit

    fun removeSensitiveHeader(name: String) = Unit

    fun addSensitiveBodyField(name: String) = Unit

    fun removeSensitiveBodyField(name: String) = Unit
}
