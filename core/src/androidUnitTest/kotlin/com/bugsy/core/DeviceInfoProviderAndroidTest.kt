package com.bugsy.core

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceInfoProviderAndroidTest {
    @Test
    fun `model keeps the maker when the model name does not include it`() {
        assertEquals("Samsung SM-S918B", androidDeviceModel("samsung", "SM-S918B"))
    }

    @Test
    fun `model is not prefixed twice when it already starts with the maker`() {
        assertEquals("Google Pixel 8", androidDeviceModel("Google", "Google Pixel 8"))
    }

    @Test
    fun `model falls back to what is available`() {
        assertEquals("Pixel 8", androidDeviceModel("", "Pixel 8"))
        assertEquals("Google", androidDeviceModel("Google", null))
        assertEquals(UNKNOWN_INFO, androidDeviceModel(null, null))
    }

    @Test
    fun `without an application context the app fields are unknown but the device fields are real`() {
        BugsyContext.applicationContext = null

        val info = readPlatformDeviceInfo()

        assertEquals(UNKNOWN_INFO, info.appVersionName)
        assertEquals(UNKNOWN_INFO, info.appVersionCode)
        assertEquals(UNKNOWN_INFO, info.buildType)
        assertEquals("Android", info.osName)
    }
}
