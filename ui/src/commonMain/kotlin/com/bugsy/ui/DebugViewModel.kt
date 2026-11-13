package com.bugsy.ui

import com.bugsy.core.DebugRepository
import com.bugsy.core.model.DebugRequest
import com.bugsy.core.model.DeviceInfo
import kotlinx.coroutines.flow.StateFlow

class DebugViewModel {
    val requests: StateFlow<List<DebugRequest>> = DebugRepository.requests

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
}
