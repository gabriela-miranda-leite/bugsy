package com.bugsy.desktop.export

import com.bugsy.desktop.wire.WireMockRule
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
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

/**
 * A local copy of `core`'s `MockRuleFileSerializer` file format (DEB-60/DEB-54's `{"version": 1, "rules": [...]}`
 * shape) — not a dependency on `:core`, same reasoning as the rest of this module. Desktop's import always
 * merges (upserts each rule by sending it as `BugsyCommand.AddMockRule`); mobile's merge/replace choice dialog
 * isn't implemented here.
 */
object MockRuleFile {
    private const val CURRENT_VERSION = 1
    private val json = Json { prettyPrint = true }

    fun serialize(rules: List<WireMockRule>): String {
        val root = buildJsonObject {
            put("version", CURRENT_VERSION)
            put("rules", JsonArray(rules.filter { it.isActive }.map { entryJson(it) }))
        }
        return json.encodeToString(JsonElement.serializer(), root)
    }

    private fun entryJson(rule: WireMockRule): JsonObject = buildJsonObject {
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

    sealed class ImportResult {
        data class Success(val rules: List<WireMockRule>) : ImportResult()
        object Cancelled : ImportResult()
        data class Invalid(val reason: String) : ImportResult()
    }

    /** `Invalid` for malformed JSON, an unknown version, or an entry missing `urlPattern` — named reasons so a
     * failed import says why instead of silently doing nothing. */
    fun deserialize(text: String): ImportResult {
        val root = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull()
            ?: return ImportResult.Invalid("not valid JSON")
        val version = root["version"]?.jsonPrimitive?.intOrNull
            ?: return ImportResult.Invalid("missing or non-numeric \"version\" field")
        if (version != CURRENT_VERSION) return ImportResult.Invalid("unsupported version $version (expected $CURRENT_VERSION)")
        val rulesJson = root["rules"] as? JsonArray ?: return ImportResult.Invalid("missing \"rules\" array")
        return runCatching { ImportResult.Success(rulesJson.map { entryFromJson(it.jsonObject) }) }
            .getOrElse { ImportResult.Invalid(it.message ?: "malformed rule entry") }
    }

    private fun entryFromJson(entry: JsonObject): WireMockRule {
        val urlPattern = entry["urlPattern"]?.jsonPrimitive?.contentOrNull ?: error("missing urlPattern")
        val headers = (entry["headers"] as? JsonObject)?.mapValues { it.value.jsonPrimitive.content }
        return WireMockRule(
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
        body is JsonPrimitive -> body.content
        else -> body.toString()
    }

    fun exportToFile(rules: List<WireMockRule>, suggestedName: String = "bugsy-mocks.json"): File? {
        val dialog = FileDialog(null as Frame?, "Export mock rules", FileDialog.SAVE)
        dialog.file = suggestedName
        dialog.isVisible = true
        val directory = dialog.directory ?: return null
        val name = dialog.file ?: return null
        val file = File(directory, name)
        file.writeText(serialize(rules))
        return file
    }

    fun importFromFile(): ImportResult {
        val dialog = FileDialog(null as Frame?, "Import mock rules", FileDialog.LOAD)
        dialog.isVisible = true
        val directory = dialog.directory ?: return ImportResult.Cancelled
        val name = dialog.file ?: return ImportResult.Cancelled
        return deserialize(File(directory, name).readText())
    }
}
