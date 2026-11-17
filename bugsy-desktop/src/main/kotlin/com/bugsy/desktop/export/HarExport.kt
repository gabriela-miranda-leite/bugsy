package com.bugsy.desktop.export

import com.bugsy.desktop.wire.WireDebugRequest
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * A minimal HAR 1.2 writer, hand-built the same way `:core`'s `HarSerializer` is (not shared — same reasoning
 * as the rest of `:bugsy-desktop` not depending on `:core`). Always exports the masked data, regardless of
 * whether the detail panel's reveal toggle is on: mirrors `HarSerializer`'s own default (mobile's share sheet
 * only reveals secrets when the user explicitly confirms it for that export), and avoids writing real secrets
 * to a file on disk by default.
 */
fun buildHar(requests: List<WireDebugRequest>): String {
    val json = Json { prettyPrint = true }
    val root = buildJsonObject {
        put(
            "log",
            buildJsonObject {
                put("version", "1.2")
                put("creator", buildJsonObject { put("name", "Bugsy Desktop") })
                put("entries", JsonArray(requests.map { entryJson(it) }))
            },
        )
    }
    return json.encodeToString(JsonElement.serializer(), root)
}

private fun entryJson(request: WireDebugRequest): JsonObject = buildJsonObject {
    put("startedDateTime", runCatching { Instant.ofEpochMilli(request.timestamp).toString() }.getOrDefault(""))
    put("time", request.durationMs)
    put(
        "request",
        buildJsonObject {
            put("method", request.method)
            put("url", request.url)
            put("httpVersion", "HTTP/1.1")
            put("headers", headersJson(request.requestHeaders))
            put("queryString", JsonArray(emptyList()))
            put("cookies", JsonArray(emptyList()))
            put("headersSize", -1)
            put("bodySize", request.requestBody?.length ?: -1)
            if (!request.requestBody.isNullOrEmpty()) {
                put("postData", buildJsonObject { put("mimeType", "application/json"); put("text", request.requestBody) })
            }
        },
    )
    put(
        "response",
        buildJsonObject {
            put("status", request.statusCode ?: 0)
            put("statusText", "")
            put("httpVersion", "HTTP/1.1")
            put("headers", headersJson(request.responseHeaders))
            put("cookies", JsonArray(emptyList()))
            put(
                "content",
                buildJsonObject {
                    put("size", request.responseBody?.length ?: 0)
                    put("mimeType", request.responseHeaders["Content-Type"] ?: "application/json")
                    put("text", request.responseBody ?: "")
                },
            )
            put("redirectURL", "")
            put("headersSize", -1)
            put("bodySize", request.responseBody?.length ?: -1)
        },
    )
    put("cache", buildJsonObject {})
    put("timings", buildJsonObject { put("send", 0); put("wait", request.durationMs); put("receive", 0) })
}

private fun headersJson(headers: Map<String, String>): JsonArray =
    JsonArray(headers.map { (name, value) -> buildJsonObject { put("name", name); put("value", value) } })

/** Blocks on a native AWT save dialog — fine to call directly from a Compose click handler. Returns the file
 * written, or `null` if the user cancelled. */
fun saveHarToFile(har: String, suggestedName: String = "bugsy-export.har"): File? {
    val dialog = FileDialog(null as Frame?, "Export HAR", FileDialog.SAVE)
    dialog.file = suggestedName
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val name = dialog.file ?: return null
    val file = File(directory, name)
    file.writeText(har)
    return file
}
