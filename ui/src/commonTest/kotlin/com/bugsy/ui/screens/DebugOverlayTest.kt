package com.bugsy.ui.screens

import com.bugsy.core.model.DebugRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebugOverlayTest {

    @Test
    fun `pathOnly strips scheme and host and query`() {
        assertEquals("/v1/users/42", "https://api.bugsy.dev/v1/users/42?x=1".pathOnly())
    }

    @Test
    fun `pathOnly on a bare host returns the root path`() {
        assertEquals("/", "https://api.bugsy.dev".pathOnly())
    }

    @Test
    fun `pathOnly keeps a url with no scheme as-is minus any query`() {
        assertEquals("/v1/feed", "/v1/feed?page=1".pathOnly())
    }

    private fun request(url: String, method: String = "POST", statusCode: Int? = 401, responseBody: String? = """{"ok":false}""") =
        DebugRequest(
            id = "1",
            url = url,
            method = method,
            requestHeaders = emptyMap(),
            requestBody = null,
            statusCode = statusCode,
            responseHeaders = emptyMap(),
            responseBody = responseBody,
            durationMs = 10,
            timestamp = 0,
            curlCommand = "",
        )

    @Test
    fun `toMockRulePrefill anchors an exact-match pattern on the request's path and escapes it readably`() {
        val prefill = request("https://api.bugsy.dev/v1/auth/login?x=1").toMockRulePrefill()

        // Plain character-by-character escaping, not Regex.escape()'s \Q...\E — unreadable, and this is
        // shown back to the user as an editable starting point.
        assertEquals("^/v1/auth/login$", prefill.urlPattern)
        assertTrue(Regex(prefill.urlPattern).matches("/v1/auth/login"))
        assertTrue(!Regex(prefill.urlPattern).matches("/v1/auth/login/extra"))
    }

    @Test
    fun `toMockRulePrefill escapes regex metacharacters in the path one by one`() {
        val prefill = request("https://api.bugsy.dev/v1/users/42?x=1&y=2").toMockRulePrefill()

        assertEquals("^/v1/users/42$", prefill.urlPattern)
    }

    @Test
    fun `toMockRulePrefill escapes a literal dot so it doesn't become a regex wildcard`() {
        val prefill = request("https://api.bugsy.dev/v1/files/report.pdf").toMockRulePrefill()

        assertEquals("^/v1/files/report\\.pdf$", prefill.urlPattern)
        assertTrue(Regex(prefill.urlPattern).matches("/v1/files/report.pdf"))
        assertTrue(!Regex(prefill.urlPattern).matches("/v1/files/reportXpdf"))
    }

    @Test
    fun `toMockRulePrefill carries over the method and status and body and defaults isOneShot to true`() {
        val prefill = request("https://api.bugsy.dev/v1/auth/login", method = "POST", statusCode = 401, responseBody = """{"error":"nope"}""")
            .toMockRulePrefill()

        assertEquals("POST", prefill.method)
        assertEquals(401, prefill.statusCode)
        assertEquals("""{"error":"nope"}""", prefill.responseBody)
        assertTrue(prefill.isOneShot)
    }

    @Test
    fun `toMockRulePrefill falls back to 200 and an empty body when the request has neither`() {
        val prefill = request("https://api.bugsy.dev/v1/feed", statusCode = null, responseBody = null).toMockRulePrefill()

        assertEquals(200, prefill.statusCode)
        assertEquals("", prefill.responseBody)
    }

    @Test
    fun `toMockRulePrefill defaults response headers to JSON when the request captured none`() {
        val prefill = request("https://api.bugsy.dev/v1/feed").toMockRulePrefill()

        assertEquals(mapOf("Content-Type" to "application/json"), prefill.responseHeaders)
    }
}
