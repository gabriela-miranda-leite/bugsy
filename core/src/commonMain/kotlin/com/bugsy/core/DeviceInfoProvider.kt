package com.bugsy.core

import com.bugsy.core.model.DeviceInfo

/** Shown for anything that can't be read (no application context, or the host app didn't provide it). */
internal const val UNKNOWN_INFO = "—"

/** What only the platform can tell: the host app, the device and the OS. */
internal data class PlatformDeviceInfo(
    val appVersionName: String = UNKNOWN_INFO,
    val appVersionCode: String = UNKNOWN_INFO,
    val buildType: String = UNKNOWN_INFO,
    val deviceModel: String = UNKNOWN_INFO,
    val osName: String = UNKNOWN_INFO,
    val osVersion: String = UNKNOWN_INFO,
    val locale: String = UNKNOWN_INFO,
)

internal expect fun readPlatformDeviceInfo(): PlatformDeviceInfo

/**
 * Joins the platform values with the ones Bugsy knows about itself. [environment] can't be discovered, so it
 * comes from [BugsyConfig.environment]; blank counts as not provided.
 */
internal fun buildDeviceInfo(platform: PlatformDeviceInfo, environment: String?): DeviceInfo = DeviceInfo(
    appVersionName = platform.appVersionName,
    appVersionCode = platform.appVersionCode,
    environment = environment?.takeIf { it.isNotBlank() } ?: UNKNOWN_INFO,
    buildType = platform.buildType,
    deviceModel = platform.deviceModel,
    osName = platform.osName,
    osVersion = platform.osVersion,
    locale = platform.locale,
    bugsyVersion = BuildKonfig.VERSION,
    kotlinVersion = KotlinVersion.CURRENT.toString(),
    composeVersion = BuildKonfig.COMPOSE_VERSION,
)
