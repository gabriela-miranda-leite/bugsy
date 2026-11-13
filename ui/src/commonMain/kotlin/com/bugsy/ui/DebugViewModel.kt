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

    // Placeholder until a per-platform (Android/iOS) DeviceInfo provider exists — see DEB-3.
    val deviceInfo: DeviceInfo = DeviceInfo(
        appVersionName = "—",
        appVersionCode = "—",
        environment = "—",
        buildType = "—",
        deviceModel = "—",
        osName = "—",
        osVersion = "—",
        locale = "—",
        bugsyVersion = "0.1.0-alpha",
        kotlinVersion = "2.0.21",
        composeVersion = "1.7.0",
    )

    fun clearRequests() {
        DebugRepository.clear()
    }

    fun overrideFlag(key: String, value: Any) = Bugsy.flagManager.override(key, value)

    fun resetAllFlags() = Bugsy.flagManager.resetAll()

    fun refreshFlags() = Bugsy.flagManager.refresh()
}
