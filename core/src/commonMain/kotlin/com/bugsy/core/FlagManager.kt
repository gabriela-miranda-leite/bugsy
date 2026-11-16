package com.bugsy.core

import com.bugsy.core.model.DebugFlag
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FlagManager(private val flagsProvider: () -> List<DebugFlag>) {
    private val mutex = Mutex()
    private val overrides = mutableMapOf<String, Any>()

    private val _flags = MutableStateFlow(emptyList<DebugFlag>())
    val flags: StateFlow<List<DebugFlag>> = _flags

    private val _flagChanges = MutableSharedFlow<DebugFlag>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    /** Emits the new state of a flag on every override/reset — the BugsyWsServer (DEB-58) broadcasts this as `flag_changed`. */
    val flagChanges: SharedFlow<DebugFlag> = _flagChanges

    init {
        refresh()
    }

    fun override(key: String, value: Any) {
        val changed = runBlocking {
            mutex.withLock {
                overrides[key] = value
                _flags.value = effectiveFlags()
                _flags.value.firstOrNull { it.key == key }
            }
        }
        if (changed != null) _flagChanges.tryEmit(changed)
    }

    fun reset(key: String) {
        val changed = runBlocking {
            mutex.withLock {
                overrides.remove(key)
                _flags.value = effectiveFlags()
                _flags.value.firstOrNull { it.key == key }
            }
        }
        if (changed != null) _flagChanges.tryEmit(changed)
    }

    fun resetAll() {
        val changed = runBlocking {
            mutex.withLock {
                overrides.clear()
                _flags.value = effectiveFlags()
                _flags.value
            }
        }
        changed.forEach { _flagChanges.tryEmit(it) }
    }

    fun refresh() {
        runBlocking {
            mutex.withLock { _flags.value = effectiveFlags() }
        }
    }

    /**
     * Current value of [key] (with the FlagsScreen override applied, if any), or `null` if no flag with that
     * key came from `flagsProvider`. For a toggle in the overlay to actually change the app's behavior, the code
     * that checks the flag must read from here — not directly from the original source passed to `flagsProvider`.
     */
    fun value(key: String): Any? = flags.value.firstOrNull { it.key == key }?.value

    private fun effectiveFlags(): List<DebugFlag> =
        flagsProvider().map { flag ->
            val override = overrides[flag.key]
            if (override == null) flag.copy(isOverridden = false)
            else flag.copy(value = override, isOverridden = true)
        }
}
