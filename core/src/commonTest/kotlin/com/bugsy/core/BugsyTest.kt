package com.bugsy.core

import kotlin.test.Test
import kotlin.test.assertEquals

class BugsyTest {

    @Test
    fun `version matches VERSION_NAME configured in gradle properties`() {
        assertEquals("0.1.0-SNAPSHOT", Bugsy.version)
    }
}
