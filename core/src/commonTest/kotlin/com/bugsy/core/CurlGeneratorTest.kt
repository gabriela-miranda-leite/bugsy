package com.bugsy.core

import com.bugsy.core.model.DebugRequest
import kotlin.test.Test
import kotlin.test.assertNull
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

    @Test
    fun `request without headers does not include any -H flag`() {
        val curl = CurlGenerator.generate(
            sampleRequest(
                method = "GET",
                url = "https://api.example.com/ping",
                requestHeaders = emptyMap(),
                requestBody = null
            )
        )

        assertFalse(curl.contains("-H "))
    }

    @Test
    fun `request without body does not include -d flag`() {
        val curl = CurlGenerator.generate(
            sampleRequest(
                method = "DELETE",
                url = "https://api.example.com/users/1",
                requestHeaders = mapOf("Content-Type" to "application/json"),
                requestBody = null
            )
        )

        assertFalse(curl.contains("-d "))
    }

    @Test
    fun `sensitive cpf field in body is passed through already masked`() {
        val curl = CurlGenerator.generate(
            sampleRequest(
                method = "POST",
                url = "https://api.example.com/users",
                requestHeaders = mapOf("Content-Type" to "application/json"),
                requestBody = "{\"name\":\"Ana\",\"cpf\":\"***\"}"
            )
        )

        assertTrue(curl.contains("-d '{\"name\":\"Ana\",\"cpf\":\"***\"}'"))
    }

    @Test
    fun `form-encoded body is included in full`() {
        val curl = CurlGenerator.generate(
            sampleRequest(
                method = "POST",
                url = "https://api.example.com/login",
                requestHeaders = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                requestBody = "email=user%40example.com&password=***"
            )
        )

        assertTrue(curl.contains("-d 'email=user%40example.com&password=***'"))
    }

    @Test
    fun `plain text body is included in full`() {
        val curl = CurlGenerator.generate(
            sampleRequest(
                method = "POST",
                url = "https://api.example.com/logs",
                requestHeaders = mapOf("Content-Type" to "text/plain"),
                requestBody = "something went wrong"
            )
        )

        assertTrue(curl.contains("-d 'something went wrong'"))
    }

    @Test
    fun `PATCH request is generated with correct method`() {
        val curl = CurlGenerator.generate(
            sampleRequest(
                method = "PATCH",
                url = "https://api.example.com/users/1",
                requestHeaders = mapOf("Content-Type" to "application/json"),
                requestBody = "{\"name\":\"Ana\"}"
            )
        )

        assertTrue(curl.startsWith("curl -X PATCH 'https://api.example.com/users/1'"))
    }

    @Test
    fun `PUT request is generated with correct method`() {
        val curl = CurlGenerator.generate(
            sampleRequest(
                method = "PUT",
                url = "https://api.example.com/users/1",
                requestHeaders = mapOf("Content-Type" to "application/json"),
                requestBody = "{\"name\":\"Ana\"}"
            )
        )

        assertTrue(curl.startsWith("curl -X PUT 'https://api.example.com/users/1'"))
    }

    @Test
    fun `DELETE request is generated with correct method`() {
        val curl = CurlGenerator.generate(
            sampleRequest(
                method = "DELETE",
                url = "https://api.example.com/users/1",
                requestHeaders = emptyMap(),
                requestBody = null
            )
        )

        assertTrue(curl.startsWith("curl -X DELETE 'https://api.example.com/users/1'"))
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

    private fun maskedRequest(): DebugRequest {
        val raw = mapOf("Authorization" to "Bearer real-token")
        val masked = DebugRequest(
            id = "1", url = "https://api.dev/x", method = "POST",
            requestHeaders = HeaderRedactor.redact(raw), requestBody = BodyFieldMasker.mask("""{"password":"p4ss"}"""),
            statusCode = 200, responseHeaders = emptyMap(), responseBody = null,
            durationMs = 1, timestamp = 1, curlCommand = ""
        )
        return masked.copy(curlCommand = CurlGenerator.generate(masked))
    }

    @Test
    fun `withSecrets keeps the real values only in the secret curl`() {
        CaptureSettings.configure(allowCopyingSecrets = true)
        val result = CurlGenerator.withSecrets(
            maskedRequest(), rawHeaders = mapOf("Authorization" to "Bearer real-token"), rawBody = """{"password":"p4ss"}"""
        )

        assertTrue("real-token" !in result.curlCommand && "p4ss" !in result.curlCommand)
        val secret = result.curlCommandWithSecrets!!.value
        assertTrue("Authorization: Bearer real-token" in secret)
        assertTrue("p4ss" in secret)
    }

    @Test
    fun `the secret curl never shows up in toString of the request`() {
        CaptureSettings.configure(allowCopyingSecrets = true)
        val result = CurlGenerator.withSecrets(
            maskedRequest(), rawHeaders = mapOf("Authorization" to "Bearer real-token"), rawBody = null
        )

        assertTrue("real-token" !in result.toString())
    }

    @Test
    fun `withSecrets returns the request untouched when copying secrets is disabled`() {
        CaptureSettings.configure(allowCopyingSecrets = false)
        try {
            val masked = maskedRequest()
            val result = CurlGenerator.withSecrets(masked, mapOf("Authorization" to "Bearer real-token"), null)

            assertNull(result.curlCommandWithSecrets)
            assertEquals(masked, result)
        } finally {
            CaptureSettings.configure(allowCopyingSecrets = true)
        }
    }

    @Test
    fun `withSecrets does not store a secret curl when there is nothing to reveal`() {
        CaptureSettings.configure(allowCopyingSecrets = true)
        val plain = DebugRequest(
            id = "1", url = "https://api.dev/x", method = "GET", requestHeaders = mapOf("Accept" to "application/json"),
            requestBody = null, statusCode = 200, responseHeaders = emptyMap(), responseBody = null,
            durationMs = 1, timestamp = 1, curlCommand = ""
        ).let { it.copy(curlCommand = CurlGenerator.generate(it)) }

        val result = CurlGenerator.withSecrets(plain, mapOf("Accept" to "application/json"), null)

        assertNull(result.curlCommandWithSecrets)
    }
}
