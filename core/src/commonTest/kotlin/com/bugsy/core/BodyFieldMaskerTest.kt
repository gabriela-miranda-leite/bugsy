package com.bugsy.core

import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BodyFieldMaskerTest {

    @AfterTest
    fun resetSensitiveFields() {
        BodyFieldMasker.reset()
    }

    private fun maskedJson(body: String): String {
        val masked = BodyFieldMasker.mask(body)!!
        Json.parseToJsonElement(masked) // fails if the result isn't valid JSON
        return masked
    }

    @Test
    fun `masks default sensitive fields in a JSON body`() {
        val body = """{"email":"user@example.com","password":"supersecret","cpf":"123.456.789-00"}"""

        assertEquals(
            """{"email":"user@example.com","password":"***","cpf":"***"}""",
            maskedJson(body)
        )
    }

    @Test
    fun `returns null when body is null`() {
        assertNull(BodyFieldMasker.mask(null))
    }

    @Test
    fun `keys are matched case-insensitively`() {
        val masked = maskedJson("""{"Password":"a","TOKEN":"b","Secret":"c"}""")

        assertEquals("""{"Password":"***","TOKEN":"***","Secret":"***"}""", masked)
    }

    @Test
    fun `masks nested fields and fields inside arrays`() {
        val masked = maskedJson("""{"user":{"password":"abc","name":"gabi"},"items":[{"token":"t1"},{"token":"t2"}]}""")

        assertEquals("""{"user":{"password":"***","name":"gabi"},"items":[{"token":"***"},{"token":"***"}]}""", masked)
    }

    @Test
    fun `masks values that are not strings and keeps the JSON valid`() {
        val masked = maskedJson("""{"password":1234,"token":null,"secret":true,"cpf":{"a":"b"},"password2":[1,2]}""")

        assertEquals("""{"password":"***","token":"***","secret":"***","cpf":"***","password2":[1,2]}""", masked)
    }

    @Test
    fun `masks values with escaped quotes without leaking the rest`() {
        val masked = maskedJson("""{"password":"ab\"cd\\ef","ok":"x"}""")

        assertEquals("""{"password":"***","ok":"x"}""", masked)
    }

    @Test
    fun `masks JSON embedded in a string value`() {
        val masked = maskedJson("""{"payload":"{\"password\":\"abc\"}"}""")

        assertFalse("abc" in masked)
    }

    @Test
    fun `non JSON text without sensitive fields is returned unchanged`() {
        val body = "plain text response, nothing to see here"

        assertEquals(body, BodyFieldMasker.mask(body))
    }

    @Test
    fun `masks form-encoded bodies`() {
        val masked = BodyFieldMasker.mask("user=gabi&password=abc123&token=xyz&other=1")

        assertEquals("user=gabi&password=***&token=***&other=1", masked)
    }

    @Test
    fun `masks a truncated JSON body instead of letting the secret through`() {
        val masked = BodyFieldMasker.mask("""{"name":"gabi","password":"supers""")!!

        assertFalse("supers" in masked)
        assertTrue(""""password":"***"""" in masked)
    }

    @Test
    fun `masks sensitive fields in invalid JSON`() {
        val masked = BodyFieldMasker.mask("""{"name":"gabi", "token": abc123, }""")!!

        assertFalse("abc123" in masked)
    }

    @Test
    fun `does not mask names that only contain a sensitive word`() {
        val masked = maskedJson("""{"passwordHint":"blue","tokens":"x"}""")

        assertEquals("""{"passwordHint":"blue","tokens":"x"}""", masked)
    }

    @Test
    fun `masks a custom field added through Bugsy`() {
        Bugsy.addSensitiveBodyField("PIN")

        assertEquals("""{"pin":"***"}""", maskedJson("""{"pin":"1234"}"""))
        assertEquals("pin=***", BodyFieldMasker.mask("pin=1234"))
    }

    @Test
    fun `shows the real value of a field removed through Bugsy`() {
        Bugsy.removeSensitiveBodyField("token")

        assertEquals("""{"token":"abc","password":"***"}""", maskedJson("""{"token":"abc","password":"x"}"""))
    }

    @Test
    fun `reset restores the default fields`() {
        Bugsy.addSensitiveBodyField("pin")
        Bugsy.removeSensitiveBodyField("password")

        BodyFieldMasker.reset()

        assertEquals("""{"pin":"1","password":"***"}""", maskedJson("""{"pin":"1","password":"x"}"""))
    }

    @Test
    fun `maskTracked reports whether something was actually redacted`() {
        assertTrue(BodyFieldMasker.maskTracked("""{"password":"x"}""").redacted)
        assertTrue(BodyFieldMasker.maskTracked("pin=1").redacted.not())
        assertTrue(BodyFieldMasker.maskTracked("""{"user":"gabi"}""").redacted.not())
        assertTrue(BodyFieldMasker.maskTracked("password=abc").redacted)
        assertTrue(BodyFieldMasker.maskTracked("""{"password":"***"}""").redacted.not())
        assertNull(BodyFieldMasker.maskTracked(null).value)
    }

    // --- M4: hostile input never crashes the app nor passes through unmasked (fail closed) ---

    @Test
    fun `deeply nested JSON does not crash and does not leak the secret`() {
        val depth = 20_000
        val body = "[".repeat(depth) + """{"password":"leak-me"}""" + "]".repeat(depth)

        val masked = BodyFieldMasker.mask(body)!!

        assertFalse("leak-me" in masked)
    }

    @Test
    fun `deeply nested objects do not crash and do not leak the secret`() {
        val depth = 5_000
        val body = """{"a":""".repeat(depth) + """{"token":"leak-me"}""" + "}".repeat(depth)

        val masked = BodyFieldMasker.mask(body)!!

        assertFalse("leak-me" in masked)
    }

    @Test
    fun `an unmaskable body is replaced instead of being let through`() {
        // Nesting above the limit: falls back to text masking, which still hides the value.
        val body = "[".repeat(200) + """{"secret":"leak-me"}""" + "]".repeat(200)

        val tracked = BodyFieldMasker.maskTracked(body)

        assertTrue(tracked.redacted)
        assertFalse("leak-me" in tracked.value!!)
    }

    @Test
    fun `a large adversarial body is masked in bounded time`() {
        val body = """{"password":"""" + "\\\"".repeat(300_000) + "a".repeat(300_000)  // unterminated string, with lots of escapes

        val started = kotlin.time.TimeSource.Monotonic.markNow()
        val masked = BodyFieldMasker.mask(body)!!
        val elapsedMs = started.elapsedNow().inWholeMilliseconds

        assertTrue(elapsedMs < 5_000, "took ${elapsedMs}ms")
        assertFalse(masked.contains("aaaaaaaaaa"))
    }

    @Test
    fun `a 1 MB body full of sensitive fields is masked in bounded time`() {
        val body = """{"x":1, "password":"leak-me", """.repeat(25_000) // ~1 MB, invalid JSON (never closes)

        val started = kotlin.time.TimeSource.Monotonic.markNow()
        val masked = BodyFieldMasker.mask(body)!!
        val elapsedMs = started.elapsedNow().inWholeMilliseconds

        assertTrue(elapsedMs < 5_000, "took ${elapsedMs}ms")
        assertFalse("leak-me" in masked)
    }

    @Test
    fun `form-encoded values are masked without touching similar names`() {
        assertEquals("a=1&password=***&b=2", BodyFieldMasker.mask("a=1&password=x&b=2"))
        assertEquals("my-password=x&xtoken=y", BodyFieldMasker.mask("my-password=x&xtoken=y"))
    }
}
