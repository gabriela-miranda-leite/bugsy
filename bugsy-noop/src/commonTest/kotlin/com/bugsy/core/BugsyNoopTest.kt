package com.bugsy.core

import com.bugsy.core.model.DebugFlag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BugsyNoopTest {

    @Test
    fun `show does not make the overlay visible`() {
        Bugsy.show()

        assertFalse(Bugsy.isVisible.value)
    }

    @Test
    fun `initialize never invokes the flagsProvider`() {
        var invoked = false

        Bugsy.initialize(BugsyConfig(flagsProvider = { invoked = true; listOf(DebugFlag("a", "A", true, false)) }))

        assertFalse(invoked)
    }

    @Test
    fun `every entry point is a safe no-op`() {
        Bugsy.initialize(BugsyConfig(trigger = Trigger.FloatingButton))
        Bugsy.addSensitiveHeader("X-Token")
        Bugsy.removeSensitiveHeader("Authorization")
        Bugsy.addSensitiveBodyField("pin")
        Bugsy.removeSensitiveBodyField("password")
        Bugsy.show()
        Bugsy.hide()
        Bugsy.destroy()

        assertFalse(Bugsy.isVisible.value)
    }

    @Test
    fun `config defaults match the debug artifact`() {
        val config = BugsyConfig()

        assertEquals(Trigger.Shake, config.trigger)
        assertEquals(2.5f, config.shakeThresholdG)
    }

    @Test
    fun `the flag manager never has flags or overrides`() {
        Bugsy.initialize(BugsyConfig(flagsProvider = { listOf(DebugFlag("a", "A", true, false)) }))
        Bugsy.flagManager.override("a", false)
        Bugsy.flagManager.refresh()

        assertEquals(emptyList(), Bugsy.flagManager.flags.value)
        assertEquals(null, Bugsy.flagManager.value("a"))
    }

    @Test
    fun `the custom info provider is never invoked`() {
        var invoked = false

        Bugsy.initialize(BugsyConfig(customInfoProvider = { invoked = true; listOf("user_id" to "u_42") }))

        assertFalse(invoked)
    }
}
