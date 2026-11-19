package com.bugsy.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `bugsy-desktop` deliberately doesn't depend on `:ui`/`:core`, so `String.pathOnly()` and
 * `String.escapeRegexLiterally()` in [DesktopViewModel.kt] are hand-mirrored copies of `:ui`'s own (see
 * `DebugOverlayTest` there). These run the exact same cases against this module's copy, so a change to one
 * side's behavior without the other fails here instead of silently drifting.
 */
class DesktopViewModelParityTest {
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

    @Test
    fun `escapeRegexLiterally backslash-escapes metacharacters one by one`() {
        assertEquals("/v1/users/42", "/v1/users/42".escapeRegexLiterally())
        assertEquals("/v1\\?a=1", "/v1?a=1".escapeRegexLiterally())
        assertEquals("\\(42\\)\\[x\\]\\{y\\}", "(42)[x]{y}".escapeRegexLiterally())
    }
}
