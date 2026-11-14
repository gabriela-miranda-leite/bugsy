package com.bugsy.core

import com.bugsy.core.model.DebugRequest
import com.bugsy.core.model.SecretText

internal object CurlGenerator {

    fun generate(request: DebugRequest): String {
        val parts = mutableListOf("curl -X ${request.method} '${request.url}'")

        request.requestHeaders.forEach { (name, value) ->
            parts += "-H '$name: ${value.escapeSingleQuotes()}'"
        }

        if (!request.requestBody.isNullOrEmpty()) {
            parts += "-d '${request.requestBody.escapeSingleQuotes()}'"
        }

        return parts.joinToString(" \\\n  ")
    }

    private fun String.escapeSingleQuotes(): String = replace("'", "'\\''")

    /**
     * Devolve [masked] com o cURL gerado a partir dos valores reais ([rawHeaders] e [rawBody]), para o desenvolvedor
     * ou QA reproduzir a request. Só guarda em memória (ver [SecretText]) e só quando há algo a revelar.
     */
    fun withSecrets(masked: DebugRequest, rawHeaders: Map<String, String>, rawBody: String?): DebugRequest {
        if (!CaptureSettings.allowsCopyingSecrets()) return masked
        val real = generate(masked.copy(requestHeaders = rawHeaders, requestBody = rawBody))
        return if (real == masked.curlCommand) masked else masked.copy(curlCommandWithSecrets = SecretText(real))
    }
}
