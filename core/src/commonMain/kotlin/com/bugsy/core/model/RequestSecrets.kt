package com.bugsy.core.model

/**
 * Os valores reais (sem máscara) de uma request, para o desenvolvedor ou QA alternar a exibição na UI e copiar.
 * Só existe em memória e nunca aparece em `toString()` (logs, depuradores, `println` de um [DebugRequest]).
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
