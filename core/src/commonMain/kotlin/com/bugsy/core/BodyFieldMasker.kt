package com.bugsy.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Masks sensitive body fields before storage. Policy: when in doubt, mask.
 *
 * - Valid JSON: delegates to [JsonBodyMasker], which walks the tree and replaces the value (of any type) of
 *   sensitive fields with `***`, at any depth. Strings that contain JSON are handled too.
 * - Any other text (form-encoded, truncated or invalid JSON): delegates to [TextBodyMasker], which applies text
 *   patterns `"field": value` and `field=value`. Text with no sensitive field comes back unchanged.
 */
internal object BodyFieldMasker {
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
            return JsonBodyMasker.mask(parsed, fields, maskEmbedded = ::maskTracked)
        }
        val masked = TextBodyMasker.mask(body, fields)
        return MaskedText(masked, redacted = masked != body)
    }

    internal class MaskedText(val value: String?, val redacted: Boolean)

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
            MaskedText(TextBodyMasker.mask(body, fields), redacted = true)
        } catch (e: Throwable) {
            MaskedText(UNMASKABLE_PLACEHOLDER, redacted = true)
        }
}
