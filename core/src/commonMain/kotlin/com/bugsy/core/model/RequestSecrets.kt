package com.bugsy.core.model

/**
 * The real (unmasked) values of a request, so a developer or QA can toggle their display in the UI and copy them.
 * Only exists in memory and never shows up in `toString()` (logs, debuggers, `println` of a [DebugRequest]).
 */
class RequestSecrets(
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val responseHeaders: Map<String, String>,
    val responseBody: String?,
    val curlCommand: String,
) {
    override fun toString(): String = "***"

    override fun equals(other: Any?): Boolean =
        other is RequestSecrets && other.requestHeaders == requestHeaders && other.requestBody == requestBody &&
            other.responseHeaders == responseHeaders && other.responseBody == responseBody && other.curlCommand == curlCommand

    override fun hashCode(): Int = curlCommand.hashCode()
}
