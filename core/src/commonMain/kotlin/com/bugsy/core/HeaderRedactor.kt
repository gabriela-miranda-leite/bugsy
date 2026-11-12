package com.bugsy.core

/**
 * Stub temporário — implementação completa (headers customizados via
 * Bugsy.addSensitiveHeader) fica para DEB-27.
 */
internal object HeaderRedactor {
    private const val MASK = "***"
    private val defaultSensitiveHeaders = setOf("Authorization", "Cookie", "X-Api-Key")

    fun redact(headers: Map<String, String>): Map<String, String> =
        headers.mapValues { (name, value) ->
            if (defaultSensitiveHeaders.any { it.equals(name, ignoreCase = true) }) MASK else value
        }
}
