package com.bugsy.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * API identical to the debug artifact's, but with no behavior at all: nothing is captured, stored,
 * recorded or logged, and the overlay never opens.
 */
object Bugsy {
    val version: String = "noop"

    val isVisible: StateFlow<Boolean> = MutableStateFlow(false)

    val flagManager: FlagManager = FlagManager()

    val wsServerToken: String? = null

    fun restartWsServer() = Unit

    fun initialize(config: BugsyConfig = BugsyConfig()) = Unit

    fun destroy() = Unit

    fun show() = Unit

    fun hide() = Unit

    fun addSensitiveHeader(name: String) = Unit

    fun removeSensitiveHeader(name: String) = Unit

    fun addSensitiveBodyField(name: String) = Unit

    fun removeSensitiveBodyField(name: String) = Unit
}
