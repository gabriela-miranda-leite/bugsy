package com.bugsy.core

import kotlin.test.Test
import kotlin.test.assertEquals

class HeaderRedactorTest {

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
}
