package com.bugsy.core.model

data class DebugRequest(
    val id: String,
    val url: String,
    val method: String,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val statusCode: Int?,
    val responseHeaders: Map<String, String>,
    val responseBody: String?,
    val durationMs: Long,
    val timestamp: Long,
    val curlCommand: String,
    val metadata: Map<String, String> = emptyMap(),
    /** Real values, only in memory and only when something was masked. See `BugsyConfig.allowRevealingSecrets`. */
    val secrets: RequestSecrets? = null
)
