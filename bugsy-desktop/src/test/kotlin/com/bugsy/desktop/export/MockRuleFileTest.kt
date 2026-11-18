package com.bugsy.desktop.export

import com.bugsy.desktop.wire.WireMockRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MockRuleFileTest {

    @Test
    fun `round-trips an active rule through serialize and deserialize`() {
        val rule = WireMockRule(
            id = "mock-1",
            urlPattern = "^/v1/login$",
            name = "Login failure",
            method = "POST",
            statusCode = 401,
            responseHeaders = mapOf("Content-Type" to "application/json"),
            responseBody = """{"error":"unauthorized"}""",
            delayMs = 300,
            isOneShot = true,
        )

        val result = assertIs<MockRuleFile.ImportResult.Success>(MockRuleFile.deserialize(MockRuleFile.serialize(listOf(rule))))

        assertEquals(1, result.rules.size)
        val parsed = result.rules.first()
        assertEquals(rule.urlPattern, parsed.urlPattern)
        assertEquals(rule.name, parsed.name)
        assertEquals(rule.method, parsed.method)
        assertEquals(rule.statusCode, parsed.statusCode)
        assertEquals(rule.responseBody, parsed.responseBody)
        assertEquals(rule.delayMs, parsed.delayMs)
        assertEquals(rule.isOneShot, parsed.isOneShot)
    }

    @Test
    fun `drops inactive rules on export`() {
        val rules = listOf(
            WireMockRule(id = "1", urlPattern = "/a", isActive = true),
            WireMockRule(id = "2", urlPattern = "/b", isActive = false),
        )
        val result = assertIs<MockRuleFile.ImportResult.Success>(MockRuleFile.deserialize(MockRuleFile.serialize(rules)))
        assertEquals(1, result.rules.size)
        assertEquals("/a", result.rules.first().urlPattern)
    }

    @Test
    fun `rejects malformed json`() {
        assertIs<MockRuleFile.ImportResult.Invalid>(MockRuleFile.deserialize("not json"))
    }

    @Test
    fun `rejects an unknown version`() {
        assertIs<MockRuleFile.ImportResult.Invalid>(MockRuleFile.deserialize("""{"version":99,"rules":[]}"""))
    }

    @Test
    fun `rejects an entry missing urlPattern`() {
        assertIs<MockRuleFile.ImportResult.Invalid>(MockRuleFile.deserialize("""{"version":1,"rules":[{"statusCode":200}]}"""))
    }

    @Test
    fun `an empty rules array is a success with zero rules, not an error`() {
        val result = assertIs<MockRuleFile.ImportResult.Success>(MockRuleFile.deserialize("""{"version":1,"rules":[]}"""))
        assertEquals(0, result.rules.size)
    }
}
