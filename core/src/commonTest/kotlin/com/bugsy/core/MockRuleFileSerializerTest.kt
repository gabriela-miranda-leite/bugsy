package com.bugsy.core

import com.bugsy.core.model.MockRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockRuleFileSerializerTest {

    @Test
    fun `serialize only writes active rules`() {
        val active = MockRule(id = "1", urlPattern = "/v1/feed", isActive = true)
        val inactive = MockRule(id = "2", urlPattern = "/v1/payments", isActive = false)

        val json = MockRuleFileSerializer.serialize(listOf(active, inactive))

        assertTrue(json.contains("/v1/feed"))
        assertTrue(!json.contains("/v1/payments"))
    }

    @Test
    fun `round-trips a rule through serialize and deserialize`() {
        val rule = MockRule(
            id = "1",
            urlPattern = "/v1/auth/login",
            name = "Login failure",
            method = "POST",
            statusCode = 401,
            responseHeaders = mapOf("Content-Type" to "application/json"),
            responseBody = """{"error":"nope"}""",
            delayMs = 300,
            isOneShot = true,
        )

        val result = MockRuleFileSerializer.deserialize(MockRuleFileSerializer.serialize(listOf(rule)))

        val loaded = (result as MockRuleFileResult.Success).rules.single()
        assertEquals("/v1/auth/login", loaded.urlPattern)
        assertEquals("Login failure", loaded.name)
        assertEquals("POST", loaded.method)
        assertEquals(401, loaded.statusCode)
        assertEquals(mapOf("Content-Type" to "application/json"), loaded.responseHeaders)
        assertEquals("""{"error":"nope"}""", loaded.responseBody)
        assertEquals(300L, loaded.delayMs)
        assertTrue(loaded.isOneShot)
    }

    @Test
    fun `deserialize accepts a valid minimal file`() {
        val result = MockRuleFileSerializer.deserialize(
            """{"version": 1, "rules": [{"urlPattern": "/api/v1/catalog"}]}""",
        )

        val loaded = (result as MockRuleFileResult.Success).rules.single()
        assertEquals("/api/v1/catalog", loaded.urlPattern)
        assertEquals(null, loaded.method)
        assertEquals(200, loaded.statusCode)
        assertEquals(false, loaded.isOneShot)
    }

    @Test
    fun `deserialize parses a JSON object body into a compact string`() {
        val result = MockRuleFileSerializer.deserialize(
            """{"version": 1, "rules": [{"urlPattern": "/v1/feed", "body": {"items": []}}]}""",
        )

        val loaded = (result as MockRuleFileResult.Success).rules.single()
        assertEquals("""{"items":[]}""", loaded.responseBody)
    }

    @Test
    fun `deserialize rejects an unknown version`() {
        val result = MockRuleFileSerializer.deserialize(
            """{"version": 2, "rules": [{"urlPattern": "/api/v1/catalog"}]}""",
        )

        assertEquals(MockRuleFileResult.Invalid(MockRuleFileResult.Reason.UNKNOWN_VERSION), result)
    }

    @Test
    fun `deserialize rejects malformed JSON`() {
        val result = MockRuleFileSerializer.deserialize("{not valid json")

        assertEquals(MockRuleFileResult.Invalid(MockRuleFileResult.Reason.MALFORMED_JSON), result)
    }

    @Test
    fun `deserialize rejects a JSON array at the top level as malformed`() {
        val result = MockRuleFileSerializer.deserialize("""[{"version": 1}]""")

        assertEquals(MockRuleFileResult.Invalid(MockRuleFileResult.Reason.MALFORMED_JSON), result)
    }

    @Test
    fun `deserialize rejects a file missing the version field`() {
        val result = MockRuleFileSerializer.deserialize("""{"rules": [{"urlPattern": "/v1/feed"}]}""")

        assertEquals(MockRuleFileResult.Invalid(MockRuleFileResult.Reason.MISSING_FIELD), result)
    }

    @Test
    fun `deserialize rejects a file missing the rules field`() {
        val result = MockRuleFileSerializer.deserialize("""{"version": 1}""")

        assertEquals(MockRuleFileResult.Invalid(MockRuleFileResult.Reason.MISSING_FIELD), result)
    }

    @Test
    fun `deserialize rejects a rule missing its required urlPattern`() {
        val result = MockRuleFileSerializer.deserialize(
            """{"version": 1, "rules": [{"method": "GET"}]}""",
        )

        assertEquals(MockRuleFileResult.Invalid(MockRuleFileResult.Reason.MISSING_FIELD), result)
    }
}
