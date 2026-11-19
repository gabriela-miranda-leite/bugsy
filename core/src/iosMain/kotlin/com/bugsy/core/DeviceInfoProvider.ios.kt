package com.bugsy.core

import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSProcessInfo
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.UIKit.UIDevice
import platform.posix.uname
import platform.posix.utsname

@OptIn(ExperimentalNativeApi::class)
internal actual fun readPlatformDeviceInfo(): PlatformDeviceInfo {
    val info = NSBundle.mainBundle.infoDictionary
    val device = UIDevice.currentDevice
    return PlatformDeviceInfo(
        appVersionName = (info?.get("CFBundleShortVersionString") as? String) ?: UNKNOWN_INFO,
        appVersionCode = (info?.get("CFBundleVersion") as? String) ?: UNKNOWN_INFO,
        buildType = if (kotlin.native.Platform.isDebugBinary) "debug" else "release",
        deviceModel = iosModelIdentifier(),
        osName = device.systemName,
        osVersion = device.systemVersion,
        locale = NSLocale.currentLocale.localeIdentifier,
    )
}

/** "iPhone15,2" on a device; the simulator reports the host CPU ("arm64"), so it exposes the real one in the environment. */
@OptIn(ExperimentalForeignApi::class)
private fun iosModelIdentifier(): String {
    val simulated = NSProcessInfo.processInfo.environment["SIMULATOR_MODEL_IDENTIFIER"] as? String
    if (!simulated.isNullOrBlank()) return simulated
    return memScoped {
        val system = alloc<utsname>()
        uname(system.ptr)
        system.machine.toKString()
    }.ifBlank { UIDevice.currentDevice.model }
}
