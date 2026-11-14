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

    // --- M4: the generated command is treated by the shell as literal arguments, never as code ---

    /** Parses a command the way a POSIX shell would (single quotes, `\\` and line continuation), returning the words. */
    private fun shellWords(command: String): List<String> {
        val words = mutableListOf<String>()
        val current = StringBuilder()
        var inWord = false
        var inQuote = false
        var i = 0
        while (i < command.length) {
            val c = command[i]
            when {
                inQuote -> if (c == '\'') inQuote = false else current.append(c)
                c == '\'' -> { inQuote = true; inWord = true }
                c == '\\' && i + 1 < command.length -> {
                    if (command[i + 1] != '\n') { current.append(command[i + 1]); inWord = true }
                    i++
                }
                c.isWhitespace() -> if (inWord) { words += current.toString(); current.clear(); inWord = false }
                else -> { current.append(c); inWord = true }
            }
            i++
        }
        if (inWord) words += current.toString()
        return words
    }

    @Test
    fun `a single quote in the URL cannot break out of its argument`() {
        val url = "https://a.dev/x'; touch /tmp/pwned; echo '"

        val words = shellWords(CurlGenerator.generate(sampleRequest("GET", url, emptyMap(), null)))

        assertEquals(listOf("curl", "-X", "GET", url), words)
    }

    @Test
    fun `a malicious header name and value stay inside one argument`() {
        val request = sampleRequest("GET", "https://a.dev", mapOf("X-A'; rm -rf ~; echo '" to "v'\$(id)`id`"), null)

        val words = shellWords(CurlGenerator.generate(request))

        assertEquals(listOf("curl", "-X", "GET", "https://a.dev", "-H", "X-A'; rm -rf ~; echo ': v'\$(id)`id`"), words)
    }

    @Test
    fun `a malicious method is quoted instead of being executed`() {
        val words = shellWords(CurlGenerator.generate(sampleRequest("GET; rm -rf ~", "https://a.dev", emptyMap(), null)))

        assertEquals(listOf("curl", "-X", "GET; rm -rf ~", "https://a.dev"), words)
    }

    @Test
    fun `the body is one literal argument even with quotes and command substitution`() {
        val body = "{\"a\":\"it's \$(whoami) `id`\"}"

        val words = shellWords(CurlGenerator.generate(sampleRequest("POST", "https://a.dev", emptyMap(), body)))

        assertEquals(listOf("curl", "-X", "POST", "https://a.dev", "-d", body), words)
    }

    @Test
    fun `control characters from untrusted traffic are removed`() {
        val request = sampleRequest("GET", "https://a.dev/\u001B[31mx\r\ny", mapOf("H" to "a\r\nb\u0007c"), "l1\nl2\u001B")

        val curl = CurlGenerator.generate(request)

        assertFalse(curl.contains('\u001B') || curl.contains('\r') || curl.contains('\u0007'))
        assertEquals(listOf("curl", "-X", "GET", "https://a.dev/[31mxy", "-H", "H: abc", "-d", "l1\nl2"), shellWords(curl))
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
