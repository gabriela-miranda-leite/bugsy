package com.bugsy.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class RequestSecretsTest {

    private fun secrets(curlCommand: String = "curl -H 'Authorization: Bearer secret-token' https://api.example.com") =
        RequestSecrets(
            requestHeaders = mapOf("Authorization" to "Bearer secret-token"),
            requestBody = "{\"password\":\"hunter2\"}",
            responseHeaders = mapOf("Set-Cookie" to "session=abc123"),
            responseBody = "{\"token\":\"xyz\"}",
            curlCommand = curlCommand,
        )

    @Test
    fun `toString never leaks any secret content`() {
        val result = secrets().toString()

        assertEquals("***", result)
        assertFalse(result.contains("secret-token"))
        assertFalse(result.contains("hunter2"))
        assertFalse(result.contains("session=abc123"))
    }

    @Test
    fun `equals compares by field values, not identity`() {
        val a = secrets()
        val b = secrets()

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `equals detects a difference in any single field`() {
        val base = secrets()

        assertNotEquals(base, base.copyWith(requestHeaders = mapOf("X-Other" to "value")))
        assertNotEquals(base, base.copyWith(requestBody = "different"))
        assertNotEquals(base, base.copyWith(responseHeaders = mapOf("X-Other" to "value")))
        assertNotEquals(base, base.copyWith(responseBody = "different"))
        assertNotEquals(base, base.copyWith(curlCommand = "curl https://other.example.com"))
    }

    @Test
    fun `equals returns false for a different type`() {
        assertFalse(secrets().equals("not a RequestSecrets"))
    }

    private fun RequestSecrets.copyWith(
        requestHeaders: Map<String, String> = this.requestHeaders,
        requestBody: String? = this.requestBody,
        responseHeaders: Map<String, String> = this.responseHeaders,
        responseBody: String? = this.responseBody,
        curlCommand: String = this.curlCommand,
    ) = RequestSecrets(requestHeaders, requestBody, responseHeaders, responseBody, curlCommand)
}
