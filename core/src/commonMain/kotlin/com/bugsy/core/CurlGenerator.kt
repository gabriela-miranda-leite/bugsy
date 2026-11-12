package com.bugsy.core

import com.bugsy.core.model.DebugRequest

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
}
