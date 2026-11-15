package com.bugsy.core.model

/**
 * An interception rule: requests whose method and URL match [urlPattern] get [statusCode]/[responseHeaders]/
 * [responseBody] instead of hitting the network. `null` [method] matches any method ("ANY" in the UI).
 *
 * @param hitCount how many times this rule has fired, shown in its detail screen; `0` for a never-fired or
 * freshly created rule, reset when the rule is edited and saved again.
 */
data class MockRule(
    val id: String,
    val urlPattern: String,
    val method: String? = null,
    val statusCode: Int = 200,
    val responseHeaders: Map<String, String> = mapOf("Content-Type" to "application/json"),
    val responseBody: String = "",
    val delayMs: Long = 0,
    val isOneShot: Boolean = false,
    val isActive: Boolean = true,
    val hitCount: Int = 0,
)

/** One time [MockRule] fired: the exact URL it matched and when, shown in the rule's "Last matches" list. */
data class MockMatch(
    val url: String,
    val timestamp: Long,
)
