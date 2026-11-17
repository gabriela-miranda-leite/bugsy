package com.bugsy.core.model

import kotlinx.serialization.Serializable

@Serializable
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
    /**
     * Real values, only populated when something was masked and `BugsyConfig.allowRevealingSecrets` is on.
     * Sent over the BugsyWsServer protocol (DEB-58/DEB-60) so the desktop client can offer the same reveal
     * toggle as the mobile overlay — same gate, so a host app that turns `allowRevealingSecrets` off keeps
     * secrets out of both UIs and off the wire.
     */
    val secrets: RequestSecrets? = null
)
