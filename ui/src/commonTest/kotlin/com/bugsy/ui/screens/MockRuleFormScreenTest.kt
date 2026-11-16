package com.bugsy.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class MockRuleFormScreenTest {

    @Test
    fun `toHeaderLines defaults to JSON content type when there are no headers`() {
        val noHeaders: Map<String, String>? = null
        assertEquals("Content-Type: application/json", noHeaders.toHeaderLines())
        assertEquals("Content-Type: application/json", emptyMap<String, String>().toHeaderLines())
    }

    @Test
    fun `toHeaderLines shows one key colon value per line`() {
        val headers = linkedMapOf("Content-Type" to "application/json", "X-Api-Key" to "secret")
        assertEquals("Content-Type: application/json\nX-Api-Key: secret", headers.toHeaderLines())
    }

    @Test
    fun `toHeaderMap parses every line and trims whitespace`() {
        val text = "Content-Type:  application/json \nX-Api-Key: secret"
        assertEquals(mapOf("Content-Type" to "application/json", "X-Api-Key" to "secret"), text.toHeaderMap())
    }

    @Test
    fun `toHeaderMap preserves a colon inside the value`() {
        assertEquals(mapOf("X-Timestamp" to "10:30:00"), "X-Timestamp: 10:30:00".toHeaderMap())
    }

    @Test
    fun `toHeaderMap skips a line with no colon`() {
        assertEquals(mapOf("ok" to "1"), "not-a-header\nok: 1".toHeaderMap())
    }

    @Test
    fun `toHeaderMap skips a line with a blank header name`() {
        assertEquals(mapOf("ok" to "1"), ": application/json\nok: 1".toHeaderMap())
    }

    @Test
    fun `toHeaderMap skips blank lines`() {
        assertEquals(mapOf("ok" to "1"), "\n\nok: 1\n".toHeaderMap())
    }

    @Test
    fun `toHeaderLines and toHeaderMap round-trip multiple headers`() {
        val headers = linkedMapOf("Content-Type" to "application/json", "X-Api-Key" to "secret", "Set-Cookie" to "a=b; c=d")
        assertEquals(headers, headers.toHeaderLines().toHeaderMap())
    }
}
