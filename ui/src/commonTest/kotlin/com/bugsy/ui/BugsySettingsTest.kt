package com.bugsy.ui

import com.bugsy.ui.settings.BugsySettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BugsySettingsTest {

    @AfterTest
    fun reset() = BugsySettings.reset()

    @Test
    fun `sensitive data is masked by default`() {
        assertFalse(BugsySettings.revealSensitiveByDefault.value)
    }

    @Test
    fun `the default can be changed and reset`() {
        BugsySettings.setRevealSensitiveByDefault(true)
        assertTrue(BugsySettings.revealSensitiveByDefault.value)

        BugsySettings.reset()
        assertFalse(BugsySettings.revealSensitiveByDefault.value)
    }
}
