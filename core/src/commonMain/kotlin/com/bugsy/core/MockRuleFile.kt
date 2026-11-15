package com.bugsy.core

import com.bugsy.core.model.MockRule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlin.random.Random

sealed class MockRuleFileResult {
    data class Success(val rules: List<MockRule>) : MockRuleFileResult()
    data class Invalid(val reason: Reason) : MockRuleFileResult()

    enum class Reason { MALFORMED_JSON, UNKNOWN_VERSION, MISSING_FIELD }
}

/**
 * Reads/writes the on-disk mock rule set described in DEB-54: `{"version": 1, "rules": [{"urlPattern": ...}]}`,
 * built by hand (like [HarSerializer]) rather than with `@Serializable` — this project doesn't carry the
 * kotlinx-serialization compiler plugin, only its runtime `Json` for manual (de)construction.
 */
object MockRuleFileSerializer {
    const val CURRENT_VERSION = 1
    private val json = Json { prettyPrint = true }

    /** Only [MockRule.isActive] rules are written — an inactive rule in the file would just confuse readers. */
    fun serialize(rules: List<MockRule>): String =
        json.encodeToString(JsonElement.serializer(), toJson(rules))

    internal fun toJson(rules: List<MockRule>): JsonObject = buildJsonObject {
        put("version", CURRENT_VERSION)
        put("rules", JsonArray(rules.filter { it.isActive }.map { entryJson(it) }))
    }

    private fun entryJson(rule: MockRule): JsonObject = buildJsonObject {
        if (rule.name != null) put("name", rule.name)
        put("urlPattern", rule.urlPattern)
        if (rule.method != null) put("method", rule.method)
        put("statusCode", rule.statusCode)
        if (rule.responseHeaders.isNotEmpty()) {
            put("headers", buildJsonObject { rule.responseHeaders.forEach { (k, v) -> put(k, v) } })
        }
        put("delay", rule.delayMs)
        put("singleUse", rule.isOneShot)
        put("body", bodyToJson(rule.responseBody))
    }

    fun deserialize(text: String): MockRuleFileResult {
        val root = try {
            Json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            return MockRuleFileResult.Invalid(MockRuleFileResult.Reason.MALFORMED_JSON)
        }

        val version = root["version"]?.jsonPrimitive?.intOrNull
            ?: return MockRuleFileResult.Invalid(MockRuleFileResult.Reason.MISSING_FIELD)
        if (version != CURRENT_VERSION) return MockRuleFileResult.Invalid(MockRuleFileResult.Reason.UNKNOWN_VERSION)

        val rulesJson = root["rules"] as? JsonArray
            ?: return MockRuleFileResult.Invalid(MockRuleFileResult.Reason.MISSING_FIELD)

        val rules = try {
            rulesJson.map { element -> entryFromJson(element.jsonObject) }
        } catch (e: MissingEntryFieldException) {
            return MockRuleFileResult.Invalid(MockRuleFileResult.Reason.MISSING_FIELD)
        }
        return MockRuleFileResult.Success(rules)
    }

    private class MissingEntryFieldException : Exception()

    private fun entryFromJson(entry: JsonObject): MockRule {
        val urlPattern = entry["urlPattern"]?.jsonPrimitive?.contentOrNull ?: throw MissingEntryFieldException()
        val headers = (entry["headers"] as? JsonObject)?.mapValues { it.value.jsonPrimitive.content }

        return MockRule(
            id = "mock-${Random.nextLong()}",
            urlPattern = urlPattern,
            name = entry["name"]?.jsonPrimitive?.contentOrNull,
            method = entry["method"]?.jsonPrimitive?.contentOrNull,
            statusCode = entry["statusCode"]?.jsonPrimitive?.intOrNull ?: 200,
            responseHeaders = headers?.ifEmpty { null } ?: mapOf("Content-Type" to "application/json"),
            responseBody = entry["body"]?.let { bodyToString(it) }.orEmpty(),
            delayMs = entry["delay"]?.jsonPrimitive?.long ?: 0L,
            isOneShot = entry["singleUse"]?.jsonPrimitive?.boolean ?: false,
        )
    }

    private fun bodyToJson(body: String): JsonElement =
        if (body.isBlank()) JsonNull else runCatching { Json.parseToJsonElement(body) }.getOrElse { JsonPrimitive(body) }

    private fun bodyToString(body: JsonElement): String = when {
        body is JsonNull -> ""
        body is JsonPrimitive && body.isString -> body.content
        body is JsonPrimitive -> body.content
        else -> body.toString()
    }
}
