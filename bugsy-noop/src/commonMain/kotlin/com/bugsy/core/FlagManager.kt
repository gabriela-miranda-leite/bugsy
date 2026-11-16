package com.bugsy.core

import com.bugsy.core.model.DebugFlag
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Same API as the debug artifact's `FlagManager`, with no behavior: there are never any flags or overrides, so the
 * app in release always falls back to its default values.
 */
class FlagManager {
    val flags: StateFlow<List<DebugFlag>> = MutableStateFlow(emptyList())
    val flagChanges: SharedFlow<DebugFlag> = MutableSharedFlow()

    fun override(key: String, value: Any) = Unit

    fun reset(key: String) = Unit

    fun resetAll() = Unit

    fun refresh() = Unit

    fun value(key: String): Any? = null
}
