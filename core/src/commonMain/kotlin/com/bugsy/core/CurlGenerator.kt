package com.bugsy.core

import com.bugsy.core.model.DebugRequest

/**
 * Gera o cURL que o desenvolvedor cola num terminal. Como URL, headers e body vêm de tráfego não confiável, **todo**
 * argumento é entre aspas simples e escapado (um `'` não consegue fechar a aspa e injetar comandos) e os caracteres
 * de controle são removidos (ESC, `\r`, etc.: truques de terminal ao colar).
 */
internal object CurlGenerator {

    fun generate(request: DebugRequest): String {
        val parts = mutableListOf("curl -X ${method(request.method)} ${quote(request.url)}")

        request.requestHeaders.forEach { (name, value) ->
            parts += "-H ${quote("$name: $value")}"
        }

        if (!request.requestBody.isNullOrEmpty()) {
            parts += "-d ${quote(request.requestBody, keepNewlines = true)}"
        }

        return parts.joinToString(" \\\n  ")
    }

    /** Métodos HTTP normais (letras) saem sem aspas; qualquer outra coisa é tratada como texto não confiável. */
    private fun method(method: String): String =
        if (method.isNotEmpty() && method.all { it in 'A'..'Z' || it in 'a'..'z' }) method else quote(method)

    private fun quote(text: String, keepNewlines: Boolean = false): String =
        "'" + removeControlCharacters(text, keepNewlines).replace("'", "'\\''") + "'"

    private fun removeControlCharacters(text: String, keepNewlines: Boolean): String =
        text.filter { char ->
            when {
                char == '\t' -> true
                char == '\n' -> keepNewlines
                else -> char.code >= 0x20 && char.code != 0x7F
            }
        }
}
