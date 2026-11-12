package com.bugsy.core

/**
 * Stub temporário — só cobre valores string em JSON plano. Implementação
 * completa (campos aninhados, customização) fica para DEB-28.
 */
internal object BodyFieldMasker {
    private const val MASK = "***"
    private val defaultSensitiveFields = setOf("password", "cpf", "token", "secret")

    fun mask(body: String?): String? {
        if (body == null) return null
        var result: String = body
        for (field in defaultSensitiveFields) {
            val regex = Regex("\"$field\"\\s*:\\s*\"[^\"]*\"", RegexOption.IGNORE_CASE)
            result = regex.replace(result) { _ -> "\"$field\":\"$MASK\"" }
        }
        return result
    }
}
