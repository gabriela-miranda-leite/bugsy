package com.bugsy.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal object HeaderRedactor {
    private const val MASK = "***"
    private val defaultSensitiveHeaders = setOf("authorization", "cookie", "x-api-key")

    // Immutable set swapped atomically: interceptors read it on network threads while the app may change it from another.
    private val sensitiveHeaders = MutableStateFlow(defaultSensitiveHeaders)

    fun add(name: String) {
        sensitiveHeaders.update { it + name.trim().lowercase() }
    }

    fun remove(name: String) {
        sensitiveHeaders.update { it - name.trim().lowercase() }
    }

    fun reset() {
        sensitiveHeaders.value = defaultSensitiveHeaders
    }

    fun redact(headers: Map<String, String>): Map<String, String> {
        val sensitive = sensitiveHeaders.value
        return headers.mapValues { (name, value) ->
            if (name.lowercase() in sensitive) MASK else value
        }
    }
}
