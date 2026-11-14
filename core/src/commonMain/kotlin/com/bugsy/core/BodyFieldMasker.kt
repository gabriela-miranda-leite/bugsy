package com.bugsy.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Mascara campos sensíveis do body antes do armazenamento. Política: na dúvida, mascara.
 *
 * - JSON válido: percorre a árvore e troca o valor (de qualquer tipo) dos campos sensíveis por `***`,
 *   em qualquer nível. Strings que contêm JSON também são tratadas.
 * - Qualquer outro texto (form-encoded, JSON truncado ou inválido): aplica padrões de texto
 *   `"campo": valor` e `campo=valor`. Texto sem nenhum campo sensível volta inalterado.
 */
internal object BodyFieldMasker {
    private const val MASK = "***"
    private const val MAX_DEPTH = 64
    internal const val UNMASKABLE_PLACEHOLDER = "[body hidden: it could not be masked safely]"
    private val defaultSensitiveFields = setOf("password", "cpf", "token", "secret")

    // Conjunto imutável trocado atomicamente: os interceptors leem em threads de rede enquanto o app pode alterar de outra.
    private val sensitiveFields = MutableStateFlow(defaultSensitiveFields)

    fun add(name: String) {
        sensitiveFields.update { it + name.trim().lowercase() }
    }

    fun remove(name: String) {
        sensitiveFields.update { it - name.trim().lowercase() }
    }

    fun reset() {
        sensitiveFields.value = defaultSensitiveFields
    }

    fun mask(body: String?): String? = maskTracked(body).value

    /** Como [mask], mas informa se algum valor sensível foi realmente substituído. */
    fun maskTracked(body: String?): MaskedText {
        if (body == null) return MaskedText(null, redacted = false)
        val fields = sensitiveFields.value
        if (fields.isEmpty() || body.isBlank()) return MaskedText(body, redacted = false)

        // Falha fechada: se algo der errado (ex.: JSON hostil aninhado que estoura a pilha), o body nunca passa sem máscara.
        return try {
            maskUnsafe(body, fields)
        } catch (e: Throwable) {
            failClosed(body, fields)
        }
    }

    private fun maskUnsafe(body: String, fields: Set<String>): MaskedText {

        // O parser de JSON é recursivo e, em Kotlin/Native, estourar a pilha é SIGSEGV (não dá para capturar), então a
        // profundidade é medida ANTES, de forma iterativa; acima do limite o body vai direto para o mascaramento por texto.
        val parsed = if (exceedsNestingLimit(body)) {
            null
        } else {
            try {
                Json.parseToJsonElement(body)
            } catch (e: Exception) {
                null
            }
        }
        // O parser aceita texto solto (ex: `pin=1234`) como literal; só objetos e arrays são tratados como JSON.
        if (parsed is JsonObject || parsed is JsonArray) {
            val tracker = Tracker()
            return MaskedText(maskElement(parsed, fields, tracker, depth = 0).toString(), tracker.redacted)
        }
        val masked = maskText(body, fields)
        return MaskedText(masked, redacted = masked != body)
    }

    internal class MaskedText(val value: String?, val redacted: Boolean)

    private class Tracker {
        var redacted = false
    }

    private class TooDeepException : RuntimeException()

    /** `true` se `[`/`{` aninham mais que [MAX_DEPTH] níveis (fora de strings). Iterativo: nunca estoura a pilha. */
    private fun exceedsNestingLimit(body: String): Boolean {
        var depth = 0
        var inString = false
        var escaped = false
        for (char in body) {
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                '[', '{' -> if (++depth > MAX_DEPTH) return true
                ']', '}' -> if (depth > 0) depth--
            }
        }
        return false
    }

    /** Máscara por padrões de texto (iterativa, sem recursão); se nem ela der certo, o body inteiro é descartado. */
    private fun failClosed(body: String, fields: Set<String>): MaskedText =
        try {
            MaskedText(maskText(body, fields), redacted = true)
        } catch (e: Throwable) {
            MaskedText(UNMASKABLE_PLACEHOLDER, redacted = true)
        }

    private fun maskElement(element: JsonElement, fields: Set<String>, tracker: Tracker, depth: Int): JsonElement = when {
        depth > MAX_DEPTH -> throw TooDeepException()
        else -> maskNode(element, fields, tracker, depth)
    }

    private fun maskNode(element: JsonElement, fields: Set<String>, tracker: Tracker, depth: Int): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.mapValues { (key, value) ->
                if (key.lowercase() in fields) {
                    if (value != JsonPrimitive(MASK)) tracker.redacted = true
                    JsonPrimitive(MASK)
                } else {
                    maskElement(value, fields, tracker, depth + 1)
                }
            }
        )
        is JsonArray -> JsonArray(element.map { maskElement(it, fields, tracker, depth + 1) })
        is JsonPrimitive -> maskEmbeddedJson(element, tracker)
    }

    private fun maskEmbeddedJson(primitive: JsonPrimitive, tracker: Tracker): JsonPrimitive {
        if (!primitive.isString) return primitive
        val text = primitive.content.trimStart()
        if (!text.startsWith("{") && !text.startsWith("[")) return primitive
        val inner = maskTracked(primitive.content)
        if (inner.redacted) tracker.redacted = true
        return JsonPrimitive(inner.value ?: primitive.content)
    }

    /**
     * Mascara texto que não é JSON válido (form-encoded, JSON truncado ou fundo demais): `"campo": valor` e
     * `campo=valor`. Varredura **iterativa**, sem regex: o motor de regex do Kotlin/Native é recursivo e um body de
     * 1 MB derruba o processo (SIGSEGV), o que não pode acontecer no app host.
     */
    private fun maskText(body: String, fields: Set<String>): String {
        val spans = mutableListOf<Span>()
        for (field in fields) {
            collectJsonLikeSpans(body, field, spans)
            collectFormEncodedSpans(body, field, spans)
        }
        if (spans.isEmpty()) return body

        val out = StringBuilder(body.length)
        var cursor = 0
        for (span in spans.sortedBy { it.start }) {
            if (span.start < cursor) continue // sobreposição: o trecho anterior já cobre este
            out.append(body, cursor, span.start).append(span.replacement)
            cursor = span.end
        }
        out.append(body, cursor, body.length)
        return out.toString()
    }

    private class Span(val start: Int, val end: Int, val replacement: String)

    /** `"campo"` + `:` + valor (string entre aspas, mesmo sem fechar, ou token solto) vira `"campo":"***"`. */
    private fun collectJsonLikeSpans(body: String, field: String, spans: MutableList<Span>) {
        val needle = "\"$field\""
        var from = 0
        while (true) {
            val keyStart = body.indexOf(needle, from, ignoreCase = true)
            if (keyStart < 0) return
            from = keyStart + needle.length

            var i = from
            while (i < body.length && body[i].isWhitespace()) i++
            if (i >= body.length || body[i] != ':') continue
            i++
            while (i < body.length && body[i].isWhitespace()) i++
            if (i >= body.length) continue

            val valueEnd = if (body[i] == '"') {
                var j = i + 1
                while (j < body.length && body[j] != '"') j += if (body[j] == '\\') 2 else 1
                minOf(if (j < body.length) j + 1 else body.length, body.length)
            } else {
                var j = i
                while (j < body.length && body[j] !in ",}] \t\r\n") j++
                j
            }
            if (valueEnd == i) continue

            val key = body.substring(keyStart, keyStart + needle.length)
            val separator = body.substring(keyStart + needle.length, i)
            spans += Span(keyStart, valueEnd, "$key$separator\"$MASK\"")
            from = valueEnd
        }
    }

    /** `campo=valor` (valor até `&` ou espaço) vira `campo=***`; o caractere antes não pode fazer parte de um nome. */
    private fun collectFormEncodedSpans(body: String, field: String, spans: MutableList<Span>) {
        val needle = "$field="
        var from = 0
        while (true) {
            val start = body.indexOf(needle, from, ignoreCase = true)
            if (start < 0) return
            from = start + needle.length

            val before = if (start > 0) body[start - 1] else ' '
            if (before.isLetterOrDigit() || before == '_' || before == '-') continue

            var end = from
            while (end < body.length && body[end] != '&' && !body[end].isWhitespace()) end++
            spans += Span(start, end, body.substring(start, start + field.length) + "=" + MASK)
            from = end
        }
    }
}
