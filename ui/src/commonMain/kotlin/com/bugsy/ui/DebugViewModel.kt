package com.bugsy.ui

import com.bugsy.core.DebugRepository
import com.bugsy.core.model.DebugRequest
import kotlinx.coroutines.flow.StateFlow

class DebugViewModel {
    val requests: StateFlow<List<DebugRequest>> = DebugRepository.requests

    fun clearRequests() {
        DebugRepository.clear()
    }
}
