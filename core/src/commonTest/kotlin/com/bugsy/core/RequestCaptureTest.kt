package com.bugsy.core

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RequestCaptureTest {

    @AfterTest
    fun resetSettings() {
        CaptureSettings.configure(allowRevealingSecrets = true)
    }

    private fun capture(
        requestHeaders: Map<String, String> = mapOf("Authorization" to "Bearer real-token", "Accept" to "application/json"),
        requestBody: String? = """{"password":"p4ss","user":"gabi"}""",
        responseHeaders: Map<String, String> = mapOf("Set-Cookie" to "x", "Cookie" to "session=abc"),
        responseBody: String? = """{"token":"resp-token","ok":true}""",
    ) = RequestCapture.build(
        id = "1", url = "https://api.dev/login", method = "POST",
        requestHeaders = requestHeaders, requestBody = requestBody, statusCode = 200,
        responseHeaders = responseHeaders, responseBody = responseBody, durationMs = 1, timestamp = 1,
    )

    @Test
    fun `everything stored for display is masked`() {
        val request = capture()

        assertEquals("***", request.requestHeaders["Authorization"])
        assertEquals("***", request.responseHeaders["Cookie"])
        assertEquals("""{"password":"***","user":"gabi"}""", request.requestBody)
        assertEquals("""{"token":"***","ok":true}""", request.responseBody)
        assertTrue("real-token" !in request.curlCommand && "p4ss" !in request.curlCommand)
    }

    @Test
    fun `the real values are kept in memory for the reveal toggle`() {
        val secrets = capture().secrets!!

        assertEquals("Bearer real-token", secrets.requestHeaders["Authorization"])
        assertEquals("session=abc", secrets.responseHeaders["Cookie"])
        assertEquals("""{"password":"p4ss","user":"gabi"}""", secrets.requestBody)
        assertEquals("""{"token":"resp-token","ok":true}""", secrets.responseBody)
        assertTrue("Bearer real-token" in secrets.curlCommand && "p4ss" in secrets.curlCommand)
    }

    @Test
    fun `the real values never show up in toString of the request`() {
        val text = capture().toString()

        assertTrue("real-token" !in text && "p4ss" !in text && "resp-token" !in text)
    }

    @Test
    fun `no secrets are kept when copying is disabled`() {
        CaptureSettings.configure(allowRevealingSecrets = false)

        assertNull(capture().secrets)
    }

    @Test
    fun `no secrets are kept when nothing was masked`() {
        val request = capture(
            requestHeaders = mapOf("Accept" to "application/json"), requestBody = """{"user":"gabi"}""",
            responseHeaders = mapOf("Content-Type" to "application/json"), responseBody = """{"ok":true}""",
        )

        assertNull(request.secrets)
    }

    @Test
    fun `only a masked response body is enough to keep the secrets`() {
        val request = capture(
            requestHeaders = emptyMap(), requestBody = null, responseHeaders = emptyMap(),
            responseBody = """{"token":"resp-token"}""",
        )

        assertEquals("""{"token":"resp-token"}""", request.secrets!!.responseBody)
    }
}
