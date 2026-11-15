package com.bugsy.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class MockRuleFormScreenTest {

    @Test
    fun `toHeaderLine defaults to JSON content type when there are no headers`() {
        val noHeaders: Map<String, String>? = null
        assertEquals("Content-Type: application/json", noHeaders.toHeaderLine())
        assertEquals("Content-Type: application/json", emptyMap<String, String>().toHeaderLine())
    }

    @Test
    fun `toHeaderLine shows the first header as key colon value`() {
        assertEquals("X-Api-Key: secret", mapOf("X-Api-Key" to "secret").toHeaderLine())
    }

    @Test
    fun `toHeaderMap parses a single key-colon-value line and trims whitespace`() {
        assertEquals(mapOf("Content-Type" to "application/json"), "Content-Type:  application/json ".toHeaderMap())
    }

    @Test
    fun `toHeaderMap preserves a colon inside the value`() {
        assertEquals(mapOf("X-Timestamp" to "10:30:00"), "X-Timestamp: 10:30:00".toHeaderMap())
    }

    @Test
    fun `toHeaderMap returns an empty map for a line with no colon`() {
        assertEquals(emptyMap(), "not-a-header".toHeaderMap())
    }

    @Test
    fun `toHeaderMap returns an empty map when the header name is blank`() {
        assertEquals(emptyMap(), ": application/json".toHeaderMap())
    }
}
