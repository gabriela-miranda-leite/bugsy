package com.bugsy.core

import com.bugsy.core.model.DebugFlag
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BugsyTest {

    @AfterTest
    fun tearDown() {
        Bugsy.hide()
        Bugsy.destroy()
    }

    @Test
    fun `version matches VERSION_NAME configured in gradle properties`() {
        assertEquals("0.1.0-SNAPSHOT", Bugsy.version)
    }

    @Test
    fun `isVisible starts false`() {
        assertFalse(Bugsy.isVisible.value)
    }

    @Test
    fun `show sets isVisible to true`() {
        Bugsy.show()

        assertTrue(Bugsy.isVisible.value)
    }

    @Test
    fun `hide sets isVisible back to false`() {
        Bugsy.show()

        Bugsy.hide()

        assertFalse(Bugsy.isVisible.value)
    }

    @Test
    fun `initialize with the Shake trigger does not throw`() {
        Bugsy.initialize(BugsyConfig(trigger = Trigger.Shake))
    }

    @Test
    fun `initialize with the FloatingButton trigger does not start a shake detector`() {
        Bugsy.initialize(BugsyConfig(trigger = Trigger.FloatingButton))
    }

    @Test
    fun `initialize with the Notification trigger does not throw and can be destroyed`() {
        Bugsy.initialize(BugsyConfig(trigger = Trigger.Notification))
        Bugsy.destroy()
    }

    @Test
    fun `destroy can be called safely without a prior initialize`() {
        Bugsy.destroy()
    }

    @Test
    fun `calling initialize twice replaces the previous trigger instead of leaking it`() {
        Bugsy.initialize(BugsyConfig(trigger = Trigger.Shake))
        Bugsy.initialize(BugsyConfig(trigger = Trigger.FloatingButton))
    }

    @Test
    fun `initialize exposes flags from the configured flagsProvider`() {
        Bugsy.initialize(
            BugsyConfig(flagsProvider = { listOf(DebugFlag("feature_x", "Feature X", false, false)) })
        )

        assertEquals(listOf("feature_x"), Bugsy.flagManager.flags.value.map { it.key })
    }

    @Test
    fun `customInfo starts empty without a configured customInfoProvider`() {
        Bugsy.initialize(BugsyConfig())

        assertTrue(Bugsy.customInfo().isEmpty())
    }

    @Test
    fun `initialize exposes custom info from the configured customInfoProvider`() {
        Bugsy.initialize(BugsyConfig(customInfoProvider = { listOf("user_id" to "u_42") }))

        assertEquals(listOf("user_id" to "u_42"), Bugsy.customInfo())
    }
}
