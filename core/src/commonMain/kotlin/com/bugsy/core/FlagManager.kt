package com.bugsy.core

import com.bugsy.core.model.DebugFlag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FlagManager(private val flagsProvider: () -> List<DebugFlag>) {
    private val mutex = Mutex()
    private val overrides = mutableMapOf<String, Any>()

    private val _flags = MutableStateFlow(emptyList<DebugFlag>())
    val flags: StateFlow<List<DebugFlag>> = _flags

    init {
        refresh()
    }

    fun override(key: String, value: Any) {
        runBlocking {
            mutex.withLock {
                overrides[key] = value
                _flags.value = effectiveFlags()
            }
        }
    }

    fun reset(key: String) {
        runBlocking {
            mutex.withLock {
                overrides.remove(key)
                _flags.value = effectiveFlags()
            }
        }
    }

    fun resetAll() {
        runBlocking {
            mutex.withLock {
                overrides.clear()
                _flags.value = effectiveFlags()
            }
        }
    }

    fun refresh() {
        runBlocking {
            mutex.withLock { _flags.value = effectiveFlags() }
        }
    }

    /**
     * Valor atual de [key] (com override do FlagsScreen aplicado, se houver), ou `null` se nenhuma flag com essa
     * chave veio do `flagsProvider`. Para um toggle no overlay realmente mudar o comportamento do app, o código que
     * checa a flag deve ler daqui — não direto da fonte original passada ao `flagsProvider`.
     */
    fun value(key: String): Any? = flags.value.firstOrNull { it.key == key }?.value

    private fun effectiveFlags(): List<DebugFlag> =
        flagsProvider().map { flag ->
            val override = overrides[flag.key]
            if (override == null) flag.copy(isOverridden = false)
            else flag.copy(value = override, isOverridden = true)
        }
}
