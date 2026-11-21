package com.bugsy.core

import com.bugsy.core.model.MockRule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MockEngineTest {

    @AfterTest
    fun tearDown() = MockEngine.clear()

    @Test
    fun `no rules means no match`() {
        assertNull(MockEngine.match("GET", "https://api.bugsy.dev/v1/users/42", timestamp = 0))
    }

    @Test
    fun `matches by regex pattern on the url`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/v1/users/\\d+"))

        val matched = MockEngine.match("GET", "https://api.bugsy.dev/v1/users/42", timestamp = 0)

        assertEquals("1", matched?.id)
    }

    @Test
    fun `an anchored exact-match pattern matches against the path and not the full scheme-plus-host url`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "^/put$"))

        // ^...$ anchors a plain path like "/put" exactly: it must never be tested against
        // "https://httpbin.org/put" as a whole, which doesn't start with "/" and would never match.
        assertEquals("1", MockEngine.match("PUT", "https://httpbin.org/put", timestamp = 0)?.id)
    }

    @Test
    fun `an anchored exact-match pattern ignores the query string`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "^/v1/feed$"))

        assertEquals("1", MockEngine.match("GET", "https://api.bugsy.dev/v1/feed?page=1", timestamp = 0)?.id)
    }

    @Test
    fun `a null method matches any method`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/v1/feed", method = null))

        assertEquals("1", MockEngine.match("GET", "https://api.bugsy.dev/v1/feed", timestamp = 0)?.id)
        assertEquals("1", MockEngine.match("POST", "https://api.bugsy.dev/v1/feed", timestamp = 0)?.id)
    }

    @Test
    fun `a non-null method only matches that method`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/v1/feed", method = "POST"))

        assertNull(MockEngine.match("GET", "https://api.bugsy.dev/v1/feed", timestamp = 0))
        assertEquals("1", MockEngine.match("POST", "https://api.bugsy.dev/v1/feed", timestamp = 0)?.id)
    }

    @Test
    fun `an inactive rule never matches`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/v1/feed", isActive = false))

        assertNull(MockEngine.match("GET", "https://api.bugsy.dev/v1/feed", timestamp = 0))
    }

    @Test
    fun `an invalid regex pattern never matches instead of crashing`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "("))

        assertNull(MockEngine.match("GET", "https://api.bugsy.dev/v1/feed", timestamp = 0))
    }

    @Test
    fun `the first matching rule wins, among non-conflicting overlapping patterns`() {
        // Different patterns, so this isn't a conflictsWith case — both stay active, and match() still
        // picks whichever comes first in rulesFlow.
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/v1/feed", statusCode = 200))
        MockEngine.addRule(MockRule(id = "2", urlPattern = "/v1/f.*", statusCode = 500))

        assertEquals("1", MockEngine.match("GET", "https://api.bugsy.dev/v1/feed", timestamp = 0)?.id)
    }

    @Test
    fun `adding an active rule auto-disables another active rule for the exact same route`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/v1/feed", statusCode = 200))
        MockEngine.addRule(MockRule(id = "2", urlPattern = "/v1/feed", statusCode = 500))

        assertFalse(MockEngine.rulesFlow.value.single { it.id == "1" }.isActive)
        assertEquals("2", MockEngine.match("GET", "https://api.bugsy.dev/v1/feed", timestamp = 0)?.id)
    }

    @Test
    fun `a one-shot rule and a non-one-shot rule for the same route are not a conflict`() {
        // Deliberate "fire once, then fall back" pairing (e.g. a one-shot 500 followed by a 200) — both
        // stay active.
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/v1/feed", statusCode = 500, isOneShot = true))
        MockEngine.addRule(MockRule(id = "2", urlPattern = "/v1/feed", statusCode = 200))

        assertTrue(MockEngine.rulesFlow.value.single { it.id == "1" }.isActive)
        assertTrue(MockEngine.rulesFlow.value.single { it.id == "2" }.isActive)
        assertEquals("1", MockEngine.match("GET", "https://api.bugsy.dev/v1/feed", timestamp = 0)?.id)
        assertEquals("2", MockEngine.match("GET", "https://api.bugsy.dev/v1/feed", timestamp = 1)?.id)
    }

    @Test
    fun `adding an inactive rule never disables anything`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/v1/feed", statusCode = 200))
        MockEngine.addRule(MockRule(id = "2", urlPattern = "/v1/feed", statusCode = 500, isActive = false))

        assertTrue(MockEngine.rulesFlow.value.single { it.id == "1" }.isActive)
    }

    @Test
    fun `a one-shot rule is removed after it fires once`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/v1/feed", isOneShot = true))

        assertEquals("1", MockEngine.match("GET", "https://api.bugsy.dev/v1/feed", timestamp = 0)?.id)
        assertNull(MockEngine.match("GET", "https://api.bugsy.dev/v1/feed", timestamp = 0))
        assertTrue(MockEngine.rulesFlow.value.isEmpty())
    }

    @Test
    fun `a repeatable rule keeps firing and its hit count keeps growing`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/v1/feed"))

        MockEngine.match("GET", "https://api.bugsy.dev/v1/feed", timestamp = 0)
        val second = MockEngine.match("GET", "https://api.bugsy.dev/v1/feed", timestamp = 1)

        assertEquals(2, second?.hitCount)
        assertEquals(2, MockEngine.rulesFlow.value.single { it.id == "1" }.hitCount)
    }

    @Test
    fun `matching logs the fired url with the most recent first`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/v1/feed"))

        MockEngine.match("GET", "https://api.bugsy.dev/v1/feed?page=1", timestamp = 100)
        MockEngine.match("GET", "https://api.bugsy.dev/v1/feed?page=2", timestamp = 200)

        val matches = MockEngine.matchesFor("1")
        assertEquals(2, matches.size)
        assertEquals("https://api.bugsy.dev/v1/feed?page=2", matches.first().url)
        assertEquals(200, matches.first().timestamp)
    }

    @Test
    fun `setActive toggles a rule without removing it`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/v1/feed"))

        MockEngine.setActive("1", false)
        assertFalse(MockEngine.rulesFlow.value.single { it.id == "1" }.isActive)
        assertNull(MockEngine.match("GET", "https://api.bugsy.dev/v1/feed", timestamp = 0))

        MockEngine.setActive("1", true)
        assertEquals("1", MockEngine.match("GET", "https://api.bugsy.dev/v1/feed", timestamp = 0)?.id)
    }

    @Test
    fun `removeRule drops the rule`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/v1/feed"))

        MockEngine.removeRule("1")

        assertTrue(MockEngine.rulesFlow.value.isEmpty())
        assertNull(MockEngine.match("GET", "https://api.bugsy.dev/v1/feed", timestamp = 0))
    }

    @Test
    fun `addRule with an existing id replaces it instead of duplicating`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/v1/feed", statusCode = 200))
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/v1/feed", statusCode = 500))

        assertEquals(1, MockEngine.rulesFlow.value.size)
        assertEquals(500, MockEngine.rulesFlow.value.single().statusCode)
    }
}
