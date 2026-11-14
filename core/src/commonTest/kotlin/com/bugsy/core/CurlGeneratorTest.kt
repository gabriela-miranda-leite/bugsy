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
}
