package com.bugsy.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Masks sensitive body fields before storage. Policy: when in doubt, mask.
 *
 * - Valid JSON: walks the tree and replaces the value (of any type) of sensitive fields with `***`,
 *   at any depth. Strings that contain JSON are handled too.
 * - Any other text (form-encoded, truncated or invalid JSON): applies text patterns
 *   `"field": value` and `field=value`. Text with no sensitive field comes back unchanged.
 */
internal object BodyFieldMasker {
    private const val MASK = "***"
    private const val MAX_DEPTH = 64
    internal const val UNMASKABLE_PLACEHOLDER = "[body hidden: it could not be masked safely]"
    private val defaultSensitiveFields = setOf("password", "cpf", "token", "secret")

    // Immutable set swapped atomically: interceptors read it on network threads while the app may change it from another.
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

    /** Like [mask], but also reports whether any sensitive value was actually replaced. */
    fun maskTracked(body: String?): MaskedText {
        if (body == null) return MaskedText(null, redacted = false)
        val fields = sensitiveFields.value
        if (fields.isEmpty() || body.isBlank()) return MaskedText(body, redacted = false)

        // Fail closed: if anything goes wrong (e.g. hostile nested JSON that overflows the stack), the body never passes unmasked.
        return try {
            maskUnsafe(body, fields)
        } catch (e: Throwable) {
            failClosed(body, fields)
        }
    }

    private fun maskUnsafe(body: String, fields: Set<String>): MaskedText {

        // The JSON parser is recursive and, on Kotlin/Native, a stack overflow is a SIGSEGV (it can't be caught), so
        // depth is measured FIRST, iteratively; above the limit the body goes straight to text-based masking.
        val parsed = if (exceedsNestingLimit(body)) {
            null
        } else {
            try {
                Json.parseToJsonElement(body)
            } catch (e: Exception) {
                null
            }
        }
        // The parser accepts loose text (e.g. `pin=1234`) as a literal; only objects and arrays are treated as JSON.
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

    /** `true` if `[`/`{` nest deeper than [MAX_DEPTH] levels (outside strings). Iterative: never overflows the stack. */
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

    /** Text-pattern masking (iterative, no recursion); if even this fails, the whole body is discarded. */
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
     * Masks text that isn't valid JSON (form-encoded, truncated or too-deep JSON): `"field": value` and
     * `field=value`. **Iterative** scan, no regex: Kotlin/Native's regex engine is recursive and a 1 MB body
     * crashes the process (SIGSEGV), which must never happen in the host app.
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
            if (span.start < cursor) continue // overlap: the previous span already covers this one
            out.append(body, cursor, span.start).append(span.replacement)
            cursor = span.end
        }
        out.append(body, cursor, body.length)
        return out.toString()
    }

    private class Span(val start: Int, val end: Int, val replacement: String)

    /** `"field"` + `:` + value (quoted string, even unclosed, or a loose token) becomes `"field":"***"`. */
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

    /** `field=value` (value up to `&` or whitespace) becomes `field=***`; the preceding character can't be part of an identifier. */
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
