package com.bugsy.core

import com.bugsy.core.model.DebugRequest

/**
 * Ring buffer mínimo para desbloquear a DEB-13 — configuração de
 * maxEntries via Bugsy.initialize() e exposição reativa para a UI ficam
 * para DEB-14.
 */
internal object DebugRepository {
    private const val DEFAULT_MAX_ENTRIES = 200
    private val entries = ArrayDeque<DebugRequest>()

    @Synchronized
    fun add(request: DebugRequest) {
        entries.addLast(request)
        while (entries.size > DEFAULT_MAX_ENTRIES) {
            entries.removeFirst()
        }
    }

    @Synchronized
    fun getAll(): List<DebugRequest> = entries.toList()

    @Synchronized
    fun clear() {
        entries.clear()
    }
}
