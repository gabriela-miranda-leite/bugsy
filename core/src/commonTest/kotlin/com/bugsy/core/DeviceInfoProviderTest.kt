package com.bugsy.core

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceInfoProviderTest {
    private val platform = PlatformDeviceInfo(
        appVersionName = "2.4.1",
        appVersionCode = "241",
        buildType = "debug",
        deviceModel = "Google Pixel 8",
        osName = "Android",
        osVersion = "15 (API 35)",
        locale = "pt-BR",
    )

    @Test
    fun `platform values are passed through untouched`() {
        val info = buildDeviceInfo(platform, environment = "staging")

        assertEquals("2.4.1", info.appVersionName)
        assertEquals("241", info.appVersionCode)
        assertEquals("debug", info.buildType)
        assertEquals("Google Pixel 8", info.deviceModel)
        assertEquals("Android", info.osName)
        assertEquals("15 (API 35)", info.osVersion)
        assertEquals("pt-BR", info.locale)
        assertEquals("staging", info.environment)
    }

    @Test
    fun `the Bugsy and Kotlin versions are the real ones and not hardcoded text`() {
        val info = buildDeviceInfo(platform, environment = null)

        assertEquals(BuildKonfig.VERSION, info.bugsyVersion)
        assertEquals(KotlinVersion.CURRENT.toString(), info.kotlinVersion)
        assertEquals(BuildKonfig.COMPOSE_VERSION, info.composeVersion)
    }

    @Test
    fun `environment stays unknown unless the app provides it`() {
        assertEquals(UNKNOWN_INFO, buildDeviceInfo(platform, environment = null).environment)
        assertEquals(UNKNOWN_INFO, buildDeviceInfo(platform, environment = "  ").environment)
    }

    @Test
    fun `the platform defaults are unknown rather than invented`() {
        val info = buildDeviceInfo(PlatformDeviceInfo(), environment = null)

        assertEquals(UNKNOWN_INFO, info.appVersionName)
        assertEquals(UNKNOWN_INFO, info.deviceModel)
        assertEquals(UNKNOWN_INFO, info.locale)
    }
}
