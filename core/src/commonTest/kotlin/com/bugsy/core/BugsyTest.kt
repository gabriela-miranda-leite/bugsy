package com.bugsy.core

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BugsyTest {

    @AfterTest
    fun tearDown() {
        Bugsy.hide()
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
}
