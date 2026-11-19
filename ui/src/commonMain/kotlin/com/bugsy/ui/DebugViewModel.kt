package com.bugsy.ui

import com.bugsy.core.Bugsy
import com.bugsy.core.DebugRepository
import com.bugsy.core.model.DebugFlag
import com.bugsy.core.model.DebugRequest
import com.bugsy.core.model.DeviceInfo
import kotlinx.coroutines.flow.StateFlow

class DebugViewModel {
    val requests: StateFlow<List<DebugRequest>> = DebugRepository.requests

    val flags: StateFlow<List<DebugFlag>> get() = Bugsy.flagManager.flags

    val customInfo: List<Pair<String, String>> get() = Bugsy.customInfo()

    val deviceInfo: DeviceInfo get() = Bugsy.deviceInfo()

    fun clearRequests() {
        DebugRepository.clear()
    }

    fun overrideFlag(key: String, value: Any) = Bugsy.flagManager.override(key, value)

    fun resetAllFlags() = Bugsy.flagManager.resetAll()

    fun refreshFlags() = Bugsy.flagManager.refresh()
}
