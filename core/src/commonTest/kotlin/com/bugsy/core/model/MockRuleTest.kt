package com.bugsy.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MockRuleTest {

    @Test
    fun `displayName uses the name when it is set and non-blank`() {
        val rule = MockRule(id = "1", urlPattern = "^/v1/auth/login$", name = "Login 401")
        assertEquals("Login 401", rule.displayName)
    }

    @Test
    fun `displayName falls back to the pattern stripped of anchors when there is no name`() {
        val rule = MockRule(id = "1", urlPattern = "^/post$", name = null)
        assertEquals("/post", rule.displayName)
    }

    @Test
    fun `displayName falls back to the pattern when the name is blank`() {
        val rule = MockRule(id = "1", urlPattern = "^/put$", name = "   ")
        assertEquals("/put", rule.displayName)
    }

    @Test
    fun `displayName unescapes backslash-escaped regex metacharacters in the fallback pattern`() {
        val rule = MockRule(id = "1", urlPattern = "^/v1/files/report\\.pdf$")
        assertEquals("/v1/files/report.pdf", rule.displayName)
    }

    @Test
    fun `displayName leaves an unescaped wildcard as-is in the fallback pattern`() {
        val rule = MockRule(id = "1", urlPattern = "^/api/v1/.*$")
        assertEquals("/api/v1/.*", rule.displayName)
    }
}
