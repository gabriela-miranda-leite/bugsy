package com.bugsy.desktop.wire

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A local copy of the BugsyWsServer protocol's wire shapes (DEB-58), not a dependency on `:core` — `:core`'s
 * commonMain declares mobile-only `expect` classes (`ShakeDetector`, `FloatingButtonController`, ...) that have
 * no desktop meaning, so adding a desktop target there would require noop `actual`s for things this app will
 * never use. `bugsy-noop` already duplicates its own copies of `MockRule`/`DebugFlag` for a similar reason —
 * this follows the same convention. Deliberately minimal: this MVP (DEB-59) only needs the request list, so
 * `mock_rule_changed`/`flag_changed` events (and the `mockRules`/`flags` fields of `snapshot`) are read with
 * `ignoreUnknownKeys` and otherwise ignored, not duplicated as types.
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

sealed class WireEvent {
    data class Snapshot(val requests: List<WireDebugRequest>) : WireEvent()
    data class RequestRecorded(val request: WireDebugRequest) : WireEvent()

    /** Any event this MVP doesn't render yet (`mock_rule_changed`, `flag_changed`, or an unrecognized type). */
    object Unhandled : WireEvent()
}

internal val wireJson = Json { ignoreUnknownKeys = true }

fun encodeHandshake(token: String): String = wireJson.encodeToString(WireHandshake.serializer(), WireHandshake(token))

/** Never throws: a malformed or unrecognized frame becomes [WireEvent.Unhandled] rather than crashing the client. */
fun parseWireEvent(text: String): WireEvent {
    val root = runCatching { wireJson.parseToJsonElement(text).jsonObject }.getOrNull() ?: return WireEvent.Unhandled
    return when (root["type"]?.jsonPrimitive?.contentOrNull) {
        "snapshot" -> {
            val requests = (root["requests"] as? JsonArray).orEmpty().mapNotNull { element ->
                runCatching { wireJson.decodeFromJsonElement(WireDebugRequest.serializer(), element) }.getOrNull()
            }
            WireEvent.Snapshot(requests)
        }
        "request_recorded" -> root["request"]
            ?.let { runCatching { wireJson.decodeFromJsonElement(WireDebugRequest.serializer(), it) }.getOrNull() }
            ?.let { WireEvent.RequestRecorded(it) }
            ?: WireEvent.Unhandled
        else -> WireEvent.Unhandled
    }
}

private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())
