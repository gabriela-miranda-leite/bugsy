package com.bugsy.core.model

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

data class MockMatch(
    val url: String,
    val timestamp: Long,
)
