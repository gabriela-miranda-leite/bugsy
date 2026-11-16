package com.bugsy.core

import com.bugsy.core.model.DebugRequest
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

object DebugRepository {
    private const val DEFAULT_MAX_ENTRIES = 200

    private val mutex = Mutex()
    private val entries = ArrayDeque<DebugRequest>()

    private var maxEntries: Int = DEFAULT_MAX_ENTRIES

    /** Requests at or above this duration are flagged as slow in the UI. `0` turns the highlight off. */
    var slowRequestThresholdMs: Long = BugsyConfig.DEFAULT_SLOW_REQUEST_THRESHOLD_MS
        internal set

    private val _requests = MutableStateFlow<List<DebugRequest>>(emptyList())
    val requests: StateFlow<List<DebugRequest>> = _requests

    private val _added = MutableSharedFlow<DebugRequest>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    /** Emits each request as it's recorded — the BugsyWsServer (DEB-58) broadcasts this as `request_recorded`. */
    val added: SharedFlow<DebugRequest> = _added

    fun configureMaxEntries(maxEntries: Int) {
        require(maxEntries > 0) { "maxEntries must be positive" }
        runBlocking {
            mutex.withLock {
                this@DebugRepository.maxEntries = maxEntries
                trimToMaxEntries()
                _requests.value = entries.toList()
            }
        }
    }

    fun add(request: DebugRequest) {
        runBlocking {
            mutex.withLock {
                entries.addLast(request)
                trimToMaxEntries()
                _requests.value = entries.toList()
            }
        }
        _added.tryEmit(request)
    }

    fun getAll(): List<DebugRequest> = requests.value

    fun clear() {
        runBlocking {
            mutex.withLock {
                entries.clear()
                _requests.value = emptyList()
            }
        }
    }

    private fun trimToMaxEntries() {
        while (entries.size > maxEntries) {
            entries.removeFirst()
        }
    }
}
