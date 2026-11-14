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

        val parsed = try {
            Json.parseToJsonElement(body)
        } catch (e: Exception) {
            null
        }
        // O parser aceita texto solto (ex: `pin=1234`) como literal; só objetos e arrays são tratados como JSON.
        if (parsed is JsonObject || parsed is JsonArray) {
            val tracker = Tracker()
            return MaskedText(maskElement(parsed, fields, tracker).toString(), tracker.redacted)
        }
        val masked = maskText(body, fields)
        return MaskedText(masked, redacted = masked != body)
    }

    internal class MaskedText(val value: String?, val redacted: Boolean)

    private class Tracker {
        var redacted = false
    }

    private fun maskElement(element: JsonElement, fields: Set<String>, tracker: Tracker): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.mapValues { (key, value) ->
                if (key.lowercase() in fields) {
                    if (value != JsonPrimitive(MASK)) tracker.redacted = true
                    JsonPrimitive(MASK)
                } else {
                    maskElement(value, fields, tracker)
                }
            }
        )
        is JsonArray -> JsonArray(element.map { maskElement(it, fields, tracker) })
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

    private fun maskText(body: String, fields: Set<String>): String {
        val names = fields.joinToString("|") { Regex.escape(it) }
        val jsonLike = Regex(
            "\"($names)\"(\\s*:\\s*)(\"(?:[^\"\\\\]|\\\\.)*(?:\"|$)|[^,}\\]\\s]+)",
            RegexOption.IGNORE_CASE
        )
        val formEncoded = Regex("(?<![\\w-])($names)=([^&\\s]*)", RegexOption.IGNORE_CASE)

        return body
            .replace(jsonLike) { "\"${it.groupValues[1]}\"${it.groupValues[2]}\"$MASK\"" }
            .replace(formEncoded) { "${it.groupValues[1]}=$MASK" }
    }
}
