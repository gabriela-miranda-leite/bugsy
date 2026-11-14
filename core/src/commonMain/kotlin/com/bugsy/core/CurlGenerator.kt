package com.bugsy.core

import com.bugsy.core.model.DebugRequest

/**
 * Generates the cURL command the developer pastes into a terminal. Since URL, headers and body come from
 * untrusted traffic, **every** argument is single-quoted and escaped (a `'` can't close the quote and inject
 * commands) and control characters are stripped (ESC, `\r`, etc.: terminal paste tricks).
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

    /** Normal HTTP methods (letters) are emitted unquoted; anything else is treated as untrusted text. */
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
