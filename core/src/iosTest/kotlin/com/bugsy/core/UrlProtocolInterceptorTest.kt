package com.bugsy.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UrlProtocolInterceptorTest {

    @Test
    fun `intercepts http and https requests that were not handled yet`() {
        assertTrue(BugsyUrlProtocol.isInterceptable("https", handled = false))
        assertTrue(BugsyUrlProtocol.isInterceptable("HTTP", handled = false))
    }

    @Test
    fun `does not intercept requests already handled to prevent an interception loop`() {
        assertFalse(BugsyUrlProtocol.isInterceptable("https", handled = true))
    }

    @Test
    fun `does not intercept non http schemes`() {
        assertFalse(BugsyUrlProtocol.isInterceptable("file", handled = false))
        assertFalse(BugsyUrlProtocol.isInterceptable(null, handled = false))
    }

    @Test
    fun `capture redacts sensitive headers and masks body fields before storing`() {
        val request = IosRequestCapture.build(
            url = "https://api.bugsy.dev/login",
            method = "POST",
            requestHeaders = mapOf("Authorization" to "Bearer abc", "Accept" to "application/json"),
            requestBody = """{"user":"gabi","password":"1234"}""",
            statusCode = 200,
            responseHeaders = mapOf("Set-Cookie" to "x", "Cookie" to "secret"),
            responseBody = """{"token":"abc"}""",
            durationMs = 12,
        )

        assertEquals("***", request.requestHeaders["Authorization"])
        assertEquals("application/json", request.requestHeaders["Accept"])
        assertEquals("***", request.responseHeaders["Cookie"])
        assertEquals("""{"user":"gabi","password":"***"}""", request.requestBody)
        assertEquals("""{"token":"***"}""", request.responseBody)
        assertTrue("Bearer abc" !in request.curlCommand)
    }

    @Test
    fun `capture keeps a null request body as null`() {
        val request = IosRequestCapture.build("https://a.dev", "GET", emptyMap(), null, 200, emptyMap(), null, 1)

        assertNull(request.requestBody)
    }

    @Test
    fun `capture keeps the real values only in the in-memory secret curl`() {
        CaptureSettings.configure(allowCopyingSecrets = true)
        val request = IosRequestCapture.build(
            url = "https://api.bugsy.dev/login", method = "POST",
            requestHeaders = mapOf("Authorization" to "Bearer real-token"), requestBody = """{"password":"p4ss"}""",
            statusCode = 200, responseHeaders = emptyMap(), responseBody = null, durationMs = 1,
        )

        assertTrue("real-token" !in request.curlCommand && "p4ss" !in request.curlCommand)
        assertTrue("Bearer real-token" in request.curlCommandWithSecrets!!.value)
        assertTrue("real-token" !in request.toString())
    }
}
