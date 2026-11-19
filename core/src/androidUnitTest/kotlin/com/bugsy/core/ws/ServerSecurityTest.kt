package com.bugsy.core.ws

import com.bugsy.core.model.DebugRequest
import com.bugsy.core.model.DeviceInfo
import com.bugsy.core.model.RequestSecrets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerSecurityTest {
    @Test
    fun `constant time compare is true only for equal strings`() {
        assertTrue(constantTimeEquals("abc", "abc"))
        assertFalse(constantTimeEquals("abc", "abd"))
        assertFalse(constantTimeEquals("abc", "abcd"))
        assertFalse(constantTimeEquals("", "a"))
        assertTrue(constantTimeEquals("", ""))
    }

    @Test
    fun `session tokens are long, from the alphabet, and different every time`() {
        val tokens = (1..200).map { generateBugsySessionToken() }

        assertTrue(tokens.all { it.length == SESSION_TOKEN_LENGTH })
        assertTrue(tokens.all { token -> token.none { it in "0OIl1" } && token.all { it.isLetterOrDigit() } })
        assertEquals(200, tokens.toSet().size)
    }

    @Test
    fun `the guard locks an address after enough wrong tokens and frees it later`() {
        var now = 0L
        val guard = HandshakeGuard(maxFailures = 3, lockMs = 1_000, nowMs = { now })

        repeat(2) { guard.recordFailure("10.0.0.9") }
        assertFalse(guard.isLocked("10.0.0.9"))
        guard.recordFailure("10.0.0.9")
        assertTrue(guard.isLocked("10.0.0.9"))
        assertFalse(guard.isLocked("10.0.0.10"), "another address is not affected")

        now = 1_001
        assertFalse(guard.isLocked("10.0.0.9"))
    }

    @Test
    fun `a correct token clears the failures`() {
        val guard = HandshakeGuard(maxFailures = 2, lockMs = 1_000, nowMs = { 0L })
        guard.recordFailure("a")
        guard.recordSuccess("a")
        guard.recordFailure("a")

        assertFalse(guard.isLocked("a"))
    }

    private fun request(secrets: RequestSecrets?) = DebugRequest(
        id = "1", url = "https://api", method = "GET", requestHeaders = emptyMap(), requestBody = null, statusCode = 200,
        responseHeaders = emptyMap(), responseBody = null, durationMs = 1, timestamp = 1, curlCommand = "", secrets = secrets,
    )

    private val device = DeviceInfo("1", "1", "—", "debug", "m", "Android", "15", "en", "0", "2", "1")

    @Test
    fun `the real secret values are left out of a connection without TLS`() {
        val secrets = RequestSecrets(emptyMap(), "real", emptyMap(), null, "curl")
        val snapshot = BugsyEvent.Snapshot(listOf(request(secrets)), emptyList(), emptyList(), device).withoutSecrets() as BugsyEvent.Snapshot
        val recorded = BugsyEvent.RequestRecorded(request(secrets)).withoutSecrets() as BugsyEvent.RequestRecorded

        assertNull(snapshot.requests.single().secrets)
        assertNull(recorded.request.secrets)
        assertEquals("https://api", recorded.request.url)
    }
}
