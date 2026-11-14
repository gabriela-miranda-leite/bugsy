package com.bugsy.core

import com.bugsy.core.model.DebugFlag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlagManagerTest {

    private var providerValue: Any = false

    private fun manager() = FlagManager {
        listOf(DebugFlag(key = "feature_x", label = "Feature X", value = providerValue, isOverridden = false))
    }

    private fun FlagManager.flagX() = flags.value.single { it.key == "feature_x" }

    @Test
    fun `flags start with the provider values and no overrides`() {
        val flag = manager().flagX()

        assertEquals(false, flag.value)
        assertFalse(flag.isOverridden)
    }

    @Test
    fun `override takes precedence over the provider value`() {
        val manager = manager()

        manager.override("feature_x", true)

        assertEquals(true, manager.flagX().value)
        assertTrue(manager.flagX().isOverridden)
    }

    @Test
    fun `reset restores the provider value`() {
        val manager = manager()
        manager.override("feature_x", true)

        manager.reset("feature_x")

        assertEquals(false, manager.flagX().value)
        assertFalse(manager.flagX().isOverridden)
    }

    @Test
    fun `flags emits a new list on override and on reset`() {
        val manager = manager()
        val initial = manager.flags.value

        manager.override("feature_x", true)
        val afterOverride = manager.flags.value
        manager.reset("feature_x")
        val afterReset = manager.flags.value

        assertTrue(initial !== afterOverride)
        assertEquals(true, afterOverride.single().value)
        assertEquals(false, afterReset.single().value)
    }

    @Test
    fun `override for a key unknown to the provider does not add a flag`() {
        val manager = manager()

        manager.override("unknown", 1)

        assertEquals(listOf("feature_x"), manager.flags.value.map { it.key })
    }

    @Test
    fun `reset on a key without override is a no-op`() {
        val manager = manager()

        manager.reset("feature_x")

        assertEquals(false, manager.flagX().value)
    }

    @Test
    fun `resetAll clears every override`() {
        val manager = FlagManager {
            listOf(
                DebugFlag("a", "A", false, false),
                DebugFlag("b", "B", "x", false),
            )
        }
        manager.override("a", true)
        manager.override("b", "y")

        manager.resetAll()

        assertEquals(listOf<Any>(false, "x"), manager.flags.value.map { it.value })
        assertTrue(manager.flags.value.none { it.isOverridden })
    }

    @Test
    fun `refresh picks up provider changes while keeping overrides`() {
        val manager = manager()
        manager.override("feature_x", "local")

        providerValue = true
        manager.refresh()

        assertEquals("local", manager.flagX().value)
    }

    @Test
    fun `a new manager starts without overrides from a previous one`() {
        manager().override("feature_x", true)

        assertEquals(false, manager().flagX().value)
    }

    @Test
    fun `value returns the provider value when there is no override`() {
        val manager = manager()

        assertEquals(false, manager.value("feature_x"))
    }

    @Test
    fun `value returns the override when one is set`() {
        val manager = manager()
        manager.override("feature_x", true)

        assertEquals(true, manager.value("feature_x"))
    }

    @Test
    fun `value is null for a key unknown to the provider`() {
        val manager = manager()

        assertEquals(null, manager.value("unknown"))
    }
}
