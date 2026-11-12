package com.bugsy.core

import com.bugsy.core.model.DebugRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CurlGeneratorTest {

    @Test
    fun `GET request without body does not include -d flag`() {
        val curl = CurlGenerator.generate(
            sampleRequest(
                method = "GET",
                url = "https://api.example.com/users?page=2",
                requestHeaders = emptyMap(),
                requestBody = null
            )
        )

        assertTrue(curl.startsWith("curl -X GET 'https://api.example.com/users?page=2'"))
        assertFalse(curl.contains("-d "))
    }

    @Test
    fun `POST request with JSON body includes -d and content-type header`() {
        val curl = CurlGenerator.generate(
            sampleRequest(
                method = "POST",
                url = "https://api.example.com/login",
                requestHeaders = mapOf("Content-Type" to "application/json"),
                requestBody = "{\"email\":\"user@example.com\",\"password\":\"***\"}"
            )
        )

        assertTrue(curl.contains("-H 'Content-Type: application/json'"))
        assertTrue(curl.contains("-d '{\"email\":\"user@example.com\",\"password\":\"***\"}'"))
    }

    @Test
    fun `sensitive headers are passed through already redacted`() {
        val curl = CurlGenerator.generate(
            sampleRequest(
                method = "GET",
                url = "https://api.example.com/profile",
                requestHeaders = mapOf("Authorization" to "***"),
                requestBody = null
            )
        )

        assertTrue(curl.contains("-H 'Authorization: ***'"))
    }

    @Test
    fun `url with query params is included in full`() {
        val url = "https://api.example.com/search?q=term&page=1"
        val curl = CurlGenerator.generate(
            sampleRequest(method = "GET", url = url, requestHeaders = emptyMap(), requestBody = null)
        )

        assertEquals("curl -X GET '$url'", curl)
    }

    private fun sampleRequest(
        method: String,
        url: String,
        requestHeaders: Map<String, String>,
        requestBody: String?
    ) = DebugRequest(
        id = "1",
        url = url,
        method = method,
        requestHeaders = requestHeaders,
        requestBody = requestBody,
        statusCode = 200,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 10,
        timestamp = 0,
        curlCommand = ""
    )
}
