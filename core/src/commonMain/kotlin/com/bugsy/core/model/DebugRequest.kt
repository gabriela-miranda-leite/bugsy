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
    /** cURL com os valores reais, só em memória e só quando difere do [curlCommand]. Ver `BugsyConfig.allowCopyingSecrets`. */
    val curlCommandWithSecrets: SecretText? = null
)
