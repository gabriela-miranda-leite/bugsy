package com.bugsy.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Masks sensitive fields inside a parsed JSON tree, at any depth, including JSON embedded as a string value.
 * A [TooDeepException] above [MAX_DEPTH] is left to propagate: [BodyFieldMasker] treats it the same as any
 * other failure and falls back to [TextBodyMasker].
 */
internal object JsonBodyMasker {
    private const val MASK = "***"
    private const val MAX_DEPTH = 64

    internal class TooDeepException : RuntimeException()

    private class Tracker {
        var redacted = false
    }

    /** @param maskEmbedded masks a string value that is itself JSON text; back into [BodyFieldMasker.maskTracked]. */
    fun mask(element: JsonElement, fields: Set<String>, maskEmbedded: (String) -> BodyFieldMasker.MaskedText): BodyFieldMasker.MaskedText {
        val tracker = Tracker()
        val result = maskElement(element, fields, tracker, depth = 0, maskEmbedded)
        return BodyFieldMasker.MaskedText(result.toString(), tracker.redacted)
    }

    private fun maskElement(
        element: JsonElement,
        fields: Set<String>,
        tracker: Tracker,
        depth: Int,
        maskEmbedded: (String) -> BodyFieldMasker.MaskedText,
    ): JsonElement = when {
        depth > MAX_DEPTH -> throw TooDeepException()
        else -> maskNode(element, fields, tracker, depth, maskEmbedded)
    }

    private fun maskNode(
        element: JsonElement,
        fields: Set<String>,
        tracker: Tracker,
        depth: Int,
        maskEmbedded: (String) -> BodyFieldMasker.MaskedText,
    ): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.mapValues { (key, value) ->
                if (key.lowercase() in fields) {
                    if (value != JsonPrimitive(MASK)) tracker.redacted = true
                    JsonPrimitive(MASK)
                } else {
                    maskElement(value, fields, tracker, depth + 1, maskEmbedded)
                }
            }
        )
        is JsonArray -> JsonArray(element.map { maskElement(it, fields, tracker, depth + 1, maskEmbedded) })
        is JsonPrimitive -> maskEmbeddedJson(element, tracker, maskEmbedded)
    }

    private fun maskEmbeddedJson(
        primitive: JsonPrimitive,
        tracker: Tracker,
        maskEmbedded: (String) -> BodyFieldMasker.MaskedText,
    ): JsonPrimitive {
        if (!primitive.isString) return primitive
        val text = primitive.content.trimStart()
        if (!text.startsWith("{") && !text.startsWith("[")) return primitive
        val inner = maskEmbedded(primitive.content)
        if (inner.redacted) tracker.redacted = true
        return JsonPrimitive(inner.value ?: primitive.content)
    }
}
