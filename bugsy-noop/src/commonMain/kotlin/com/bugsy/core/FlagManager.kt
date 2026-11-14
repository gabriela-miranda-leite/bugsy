package com.bugsy.core

import com.bugsy.core.model.DebugFlag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Mesma API do `FlagManager` do artefato de debug, sem comportamento: nunca há flags nem overrides, então o app
 * em release sempre cai nos seus valores padrão.
 */
class FlagManager {
    val flags: StateFlow<List<DebugFlag>> = MutableStateFlow(emptyList())

    fun override(key: String, value: Any) = Unit

    fun reset(key: String) = Unit

    fun resetAll() = Unit

    fun refresh() = Unit

    fun value(key: String): Any? = null
}
