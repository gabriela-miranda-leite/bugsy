package com.bugsy.core.model

/**
 * An interception rule: requests whose method and URL match [urlPattern] get [statusCode]/[responseHeaders]/
 * [responseBody] instead of hitting the network. `null` [method] matches any method ("ANY" in the UI).
 *
 * @param name optional label shown instead of [urlPattern] in the UI (e.g. "Login 401" instead of
 * "^/api/v1/auth/login$"); blank or `null` falls back to the pattern.
 * @param hitCount how many times this rule has fired, shown in its detail screen; `0` for a never-fired or
 * freshly created rule, reset when the rule is edited and saved again.
 */
data class MockRule(
    val id: String,
    val urlPattern: String,
    val name: String? = null,
    val method: String? = null,
    val statusCode: Int = 200,
    val responseHeaders: Map<String, String> = mapOf("Content-Type" to "application/json"),
    val responseBody: String = "",
    val delayMs: Long = 0,
    val isOneShot: Boolean = false,
    val isActive: Boolean = true,
    val hitCount: Int = 0,
)

/** [MockRule.name] when set and non-blank, [MockRule.urlPattern] otherwise — what the UI shows as the rule's label. */
val MockRule.displayName: String
    get() = name?.takeIf { it.isNotBlank() } ?: urlPattern

/** One time [MockRule] fired: the exact URL it matched and when, shown in the rule's "Last matches" list. */
data class MockMatch(
    val url: String,
    val timestamp: Long,
)
