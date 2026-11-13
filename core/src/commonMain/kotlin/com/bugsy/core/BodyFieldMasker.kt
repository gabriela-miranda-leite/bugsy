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

    fun mask(body: String?): String? {
        if (body == null) return null
        val fields = sensitiveFields.value
        if (fields.isEmpty() || body.isBlank()) return body

        val parsed = try {
            Json.parseToJsonElement(body)
        } catch (e: Exception) {
            null
        }
        // O parser aceita texto solto (ex: `pin=1234`) como literal; só objetos e arrays são tratados como JSON.
        return if (parsed is JsonObject || parsed is JsonArray) maskElement(parsed, fields).toString() else maskText(body, fields)
    }

    private fun maskElement(element: JsonElement, fields: Set<String>): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.mapValues { (key, value) ->
                if (key.lowercase() in fields) JsonPrimitive(MASK) else maskElement(value, fields)
            }
        )
        is JsonArray -> JsonArray(element.map { maskElement(it, fields) })
        is JsonPrimitive -> maskEmbeddedJson(element, fields)
    }

    private fun maskEmbeddedJson(primitive: JsonPrimitive, fields: Set<String>): JsonPrimitive {
        if (!primitive.isString) return primitive
        val text = primitive.content.trimStart()
        if (!text.startsWith("{") && !text.startsWith("[")) return primitive
        return JsonPrimitive(mask(primitive.content) ?: primitive.content)
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
