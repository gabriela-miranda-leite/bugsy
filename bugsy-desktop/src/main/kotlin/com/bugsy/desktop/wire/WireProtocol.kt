package com.bugsy.desktop.wire

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

/**
 * A local copy of the BugsyWsServer protocol's wire shapes (DEB-58/60/61), not a dependency on `:core` —
 * `:core`'s commonMain declares mobile-only `expect` classes (`ShakeDetector`, `FloatingButtonController`, ...)
 * that have no desktop meaning, so adding a desktop target there would require noop `actual`s for things this
 * app will never use. `bugsy-noop` already duplicates its own copies of `MockRule`/`DebugFlag` for a similar
 * reason — this follows the same convention.
 */
@Serializable
data class WireDebugRequest(
    val id: String,
    val url: String,
    val method: String,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val statusCode: Int? = null,
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBody: String? = null,
    val durationMs: Long = 0,
    val timestamp: Long = 0,
    val curlCommand: String = "",
    val metadata: Map<String, String> = emptyMap(),
    /** Mirrors `core`'s `RequestSecrets` (DEB-60): the real, unmasked values, present only when something was
     * masked and the host app's `BugsyConfig.allowRevealingSecrets` is on. */
    val secrets: WireRequestSecrets? = null,
) {
    /** Mirrors `com.bugsy.core.MockMetadata.KEY_MOCKED`/`VALUE_TRUE` — duplicated as a string here for the same
     * reason the rest of this file doesn't depend on `:core`. */
    val isMocked: Boolean get() = metadata["mocked"] == "true"
}

@Serializable
data class WireRequestSecrets(
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBody: String? = null,
    val curlCommand: String = "",
)

@Serializable
data class WireHandshake(val value: String)

/** Mirrors `core`'s `MockRule` (DEB-61): an interception rule, `method = null` meaning "ANY". */
@Serializable
data class WireMockRule(
    val id: String,
    val urlPattern: String,
    val name: String? = null,
    val method: String? = null,
    val statusCode: Int = 200,
    val responseHeaders: Map<String, String> = mapOf("Content-Type" to "application/json"),
    val responseBody: String = "",
    val delayMs: Long = 0,
    val isOneShot: Boolean = false,
    val isActive: Boolean = true,
    val hitCount: Int = 0,
)

/** Mirrors `core`'s `displayName`: [WireMockRule.name] when set, the pattern (de-anchored/unescaped) otherwise. */
val WireMockRule.displayName: String
    get() = name?.takeIf { it.isNotBlank() } ?: urlPattern.toDisplayRoute()

private fun String.toDisplayRoute(): String {
    val withoutAnchors = removePrefix("^").removeSuffix("$")
    return buildString {
        var i = 0
        while (i < withoutAnchors.length) {
            val c = withoutAnchors[i]
            if (c == '\\' && i + 1 < withoutAnchors.length && withoutAnchors[i + 1] in "\\^$.|?*+()[]{}") {
                append(withoutAnchors[i + 1])
                i += 2
            } else {
                append(c)
                i += 1
            }
        }
    }
}

/** Mirrors `core`'s `DebugFlag` (DEB-60): [value] is a `Boolean`, `String` or number set by the host app's
 * `flagsProvider`, same `Any` typing problem as `core`'s own model, same [WireFlagValueSerializer] solution. */
@Serializable
data class WireDebugFlag(
    val key: String,
    val label: String,
    @Serializable(with = WireFlagValueSerializer::class) val value: Any,
    val isOverridden: Boolean,
    @Serializable(with = WireFlagValueSerializer::class) val defaultValue: Any = value,
)

/** A local copy of `core`'s `FlagValueSerializer` — only usable with [Json], round-trips through [JsonElement]. */
internal object WireFlagValueSerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Any) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("WireFlagValueSerializer only works with Json")
        val element = when (value) {
            is Boolean -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString())
        }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): Any {
        val jsonDecoder = decoder as? JsonDecoder ?: error("WireFlagValueSerializer only works with Json")
        val primitive = jsonDecoder.decodeJsonElement() as? JsonPrimitive ?: return ""
        return when {
            primitive.isString -> primitive.content
            primitive.booleanOrNull != null -> primitive.boolean
            primitive.longOrNull != null -> primitive.long
            primitive.doubleOrNull != null -> primitive.double
            else -> primitive.content
        }
    }
}

/** Mirrors `core`'s `DeviceInfo` (DEB-60). Not an event on its own — only arrives as part of `snapshot`, mobile
 * doesn't broadcast a standalone "device info changed". */
@Serializable
data class WireDeviceInfo(
    val appVersionName: String,
    val appVersionCode: String,
    val environment: String,
    val buildType: String,
    val deviceModel: String,
    val osName: String,
    val osVersion: String,
    val locale: String,
    val bugsyVersion: String,
    val kotlinVersion: String,
    val composeVersion: String,
)

/** One row of the app's custom info (the "Account" section), same shape as `core`'s `CustomInfoEntry`. */
@Serializable
data class WireCustomInfo(val label: String, val value: String)

sealed class WireEvent {
    data class Snapshot(
        val requests: List<WireDebugRequest>,
        val mockRules: List<WireMockRule>,
        val flags: List<WireDebugFlag>,
        val deviceInfo: WireDeviceInfo?,
        val customInfo: List<WireCustomInfo> = emptyList(),
    ) : WireEvent()
    data class RequestRecorded(val request: WireDebugRequest) : WireEvent()
    data class MockRuleChanged(val rule: WireMockRule, val action: String) : WireEvent()
    data class MockRuleConsumed(val rule: WireMockRule) : WireEvent()
    data class FlagChanged(val flag: WireDebugFlag) : WireEvent()

    /** An unrecognized event type. */
    object Unhandled : WireEvent()
}

internal val wireJson = Json { ignoreUnknownKeys = true }

fun encodeHandshake(token: String): String = wireJson.encodeToString(WireHandshake.serializer(), WireHandshake(token))

@Serializable
private data class AddMockRuleCommand(val type: String, val rule: WireMockRule)

@Serializable
private data class ToggleMockRuleCommand(val type: String, val id: String, val enabled: Boolean)

@Serializable
private data class DeleteMockRuleCommand(val type: String, val id: String)

@Serializable
private data class ToggleFlagCommand(val type: String, val key: String, val enabled: Boolean)

@Serializable
private data class ResetFlagCommand(val type: String, val key: String)

@Serializable
private data class ResetAllFlagsCommand(val type: String)

/**
 * Mirrors `core`'s `BugsyCommand` (DEB-58/DEB-61) — only the subset `bugsy-desktop` sends today. `type` has no
 * default value on the command classes above: kotlinx.serialization's `Json` (with the default
 * `encodeDefaults = false`) drops a field that's left at its declared default, which would silently omit the
 * discriminator the server's `when (command)` needs.
 */
fun encodeAddMockRule(rule: WireMockRule): String =
    wireJson.encodeToString(AddMockRuleCommand.serializer(), AddMockRuleCommand(type = "add_mock_rule", rule = rule))

fun encodeToggleMockRule(id: String, enabled: Boolean): String =
    wireJson.encodeToString(ToggleMockRuleCommand.serializer(), ToggleMockRuleCommand(type = "toggle_mock_rule", id = id, enabled = enabled))

fun encodeDeleteMockRule(id: String): String =
    wireJson.encodeToString(DeleteMockRuleCommand.serializer(), DeleteMockRuleCommand(type = "delete_mock_rule", id = id))

fun encodeToggleFlag(key: String, enabled: Boolean): String =
    wireJson.encodeToString(ToggleFlagCommand.serializer(), ToggleFlagCommand(type = "toggle_flag", key = key, enabled = enabled))

fun encodeResetFlag(key: String): String =
    wireJson.encodeToString(ResetFlagCommand.serializer(), ResetFlagCommand(type = "reset_flag", key = key))

fun encodeResetAllFlags(): String =
    wireJson.encodeToString(ResetAllFlagsCommand.serializer(), ResetAllFlagsCommand(type = "reset_all_flags"))

/** Never throws: a malformed or unrecognized frame becomes [WireEvent.Unhandled] rather than crashing the client. */
fun parseWireEvent(text: String): WireEvent {
    val root = runCatching { wireJson.parseToJsonElement(text).jsonObject }.getOrNull() ?: return WireEvent.Unhandled
    return when (root["type"]?.jsonPrimitive?.contentOrNull) {
        "snapshot" -> {
            val requests = (root["requests"] as? JsonArray).orEmpty().mapNotNull { element ->
                runCatching { wireJson.decodeFromJsonElement(WireDebugRequest.serializer(), element) }.getOrNull()
            }
            val mockRules = (root["mockRules"] as? JsonArray).orEmpty().mapNotNull { element ->
                runCatching { wireJson.decodeFromJsonElement(WireMockRule.serializer(), element) }.getOrNull()
            }
            val flags = (root["flags"] as? JsonArray).orEmpty().mapNotNull { element ->
                runCatching { wireJson.decodeFromJsonElement(WireDebugFlag.serializer(), element) }.getOrNull()
            }
            val deviceInfo = root["deviceInfo"]
                ?.let { runCatching { wireJson.decodeFromJsonElement(WireDeviceInfo.serializer(), it) }.getOrNull() }
            val customInfo = (root["customInfo"] as? JsonArray).orEmpty().mapNotNull { element ->
                runCatching { wireJson.decodeFromJsonElement(WireCustomInfo.serializer(), element) }.getOrNull()
            }
            WireEvent.Snapshot(requests, mockRules, flags, deviceInfo, customInfo)
        }
        "request_recorded" -> root["request"]
            ?.let { runCatching { wireJson.decodeFromJsonElement(WireDebugRequest.serializer(), it) }.getOrNull() }
            ?.let { WireEvent.RequestRecorded(it) }
            ?: WireEvent.Unhandled
        "mock_rule_changed" -> root["rule"]
            ?.let { runCatching { wireJson.decodeFromJsonElement(WireMockRule.serializer(), it) }.getOrNull() }
            ?.let { rule -> root["action"]?.jsonPrimitive?.contentOrNull?.let { WireEvent.MockRuleChanged(rule, it) } }
            ?: WireEvent.Unhandled
        "mock_rule_consumed" -> root["rule"]
            ?.let { runCatching { wireJson.decodeFromJsonElement(WireMockRule.serializer(), it) }.getOrNull() }
            ?.let { WireEvent.MockRuleConsumed(it) }
            ?: WireEvent.Unhandled
        "flag_changed" -> root["flag"]
            ?.let { runCatching { wireJson.decodeFromJsonElement(WireDebugFlag.serializer(), it) }.getOrNull() }
            ?.let { WireEvent.FlagChanged(it) }
            ?: WireEvent.Unhandled
        else -> WireEvent.Unhandled
    }
}

private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())
