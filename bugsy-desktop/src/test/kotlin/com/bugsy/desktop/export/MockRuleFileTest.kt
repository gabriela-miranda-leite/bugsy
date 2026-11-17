package com.bugsy.desktop.export

import com.bugsy.desktop.wire.WireMockRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

        val rules = MockRuleFile.deserialize(MockRuleFile.serialize(listOf(rule)))

        assertEquals(1, rules?.size)
        val parsed = rules!!.first()
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
        val parsed = MockRuleFile.deserialize(MockRuleFile.serialize(rules))
        assertEquals(1, parsed?.size)
        assertEquals("/a", parsed!!.first().urlPattern)
    }

    @Test
    fun `rejects malformed json`() {
        assertNull(MockRuleFile.deserialize("not json"))
    }

    @Test
    fun `rejects an unknown version`() {
        assertNull(MockRuleFile.deserialize("""{"version":99,"rules":[]}"""))
    }

    @Test
    fun `rejects an entry missing urlPattern`() {
        assertNull(MockRuleFile.deserialize("""{"version":1,"rules":[{"statusCode":200}]}"""))
    }
}
