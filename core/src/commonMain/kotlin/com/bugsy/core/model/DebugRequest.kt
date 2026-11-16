package com.bugsy.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
     * Real values, only in memory and only when something was masked. See `BugsyConfig.allowRevealingSecrets`.
     * `@Transient`: never sent over the BugsyWsServer protocol (DEB-58) — the desktop client only ever sees the
     * already-masked fields above, same "when in doubt, mask it" policy as the rest of the overlay.
     */
    @Transient val secrets: RequestSecrets? = null
)
