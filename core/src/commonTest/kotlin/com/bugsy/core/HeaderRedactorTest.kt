package com.bugsy.core

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HeaderRedactorTest {

    @AfterTest
    fun resetSensitiveHeaders() {
        HeaderRedactor.reset()
    }

    @Test
    fun `redacts default sensitive headers regardless of case`() {
        val redacted = HeaderRedactor.redact(
            mapOf(
                "authorization" to "Bearer secret-token",
                "Cookie" to "session=abc",
                "X-Api-Key" to "key-123",
                "Content-Type" to "application/json"
            )
        )

        assertEquals("***", redacted["authorization"])
        assertEquals("***", redacted["Cookie"])
        assertEquals("***", redacted["X-Api-Key"])
        assertEquals("application/json", redacted["Content-Type"])
    }

    @Test
    fun `keeps the header name and replaces only the value`() {
        val redacted = HeaderRedactor.redact(mapOf("Authorization" to "Bearer token123"))

        assertEquals(mapOf("Authorization" to "***"), redacted)
    }

    @Test
    fun `redacts a custom header added through Bugsy`() {
        Bugsy.addSensitiveHeader("X-Custom-Token")

        val redacted = HeaderRedactor.redact(mapOf("x-custom-token" to "abc", "Accept" to "application/json"))

        assertEquals("***", redacted["x-custom-token"])
        assertEquals("application/json", redacted["Accept"])
    }

    @Test
    fun `shows the real value of a header removed through Bugsy`() {
        Bugsy.removeSensitiveHeader("cookie")

        val redacted = HeaderRedactor.redact(mapOf("Cookie" to "session=abc", "Authorization" to "Bearer x"))

        assertEquals("session=abc", redacted["Cookie"])
        assertEquals("***", redacted["Authorization"])
    }

    @Test
    fun `adding the same header twice and with different case does not break redaction`() {
        Bugsy.addSensitiveHeader("X-Trace")
        Bugsy.addSensitiveHeader("x-trace")
        Bugsy.addSensitiveHeader("  X-TRACE  ")

        assertEquals("***", HeaderRedactor.redact(mapOf("X-Trace" to "1"))["X-Trace"])
    }

    @Test
    fun `removing a header that is not in the list is a no-op`() {
        Bugsy.removeSensitiveHeader("X-Unknown")

        assertEquals("***", HeaderRedactor.redact(mapOf("Authorization" to "x"))["Authorization"])
    }

    @Test
    fun `reset restores the default list`() {
        Bugsy.addSensitiveHeader("X-Custom-Token")
        Bugsy.removeSensitiveHeader("Authorization")

        HeaderRedactor.reset()

        val redacted = HeaderRedactor.redact(mapOf("X-Custom-Token" to "abc", "Authorization" to "Bearer x"))
        assertEquals("abc", redacted["X-Custom-Token"])
        assertEquals("***", redacted["Authorization"])
    }
}
