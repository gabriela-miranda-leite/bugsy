package com.bugsy.core

import com.bugsy.core.model.DebugRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Serializes captured requests to the HAR 1.2 format (HTTP Archive), which Postman, Insomnia and Chrome
 * DevTools open natively.
 *
 * By default the export carries exactly what Bugsy shows when sensitive data is masked. Real values from
 * [DebugRequest.secrets] are only written when the caller explicitly asks for it with `revealSecrets = true`
 * (the UI does that after the user confirms), and only for requests that actually have secrets in memory.
 */
object HarSerializer {
    private const val HAR_VERSION = "1.2"
    private const val HTTP_VERSION = "HTTP/1.1"
    private val json = Json { prettyPrint = true }

    fun serialize(requests: List<DebugRequest>, revealSecrets: Boolean = false): String =
        json.encodeToString(JsonElement.serializer(), toJson(requests, revealSecrets))

    internal fun toJson(requests: List<DebugRequest>, revealSecrets: Boolean): JsonObject = buildJsonObject {
        put("log", buildJsonObject {
            put("version", HAR_VERSION)
            put("creator", buildJsonObject {
                put("name", "Bugsy")
                put("version", Bugsy.version)
            })
            put("entries", JsonArray(requests.map { entry(it, revealSecrets) }))
        })
    }

    private fun entry(request: DebugRequest, revealSecrets: Boolean): JsonObject {
        val secrets = request.secrets.takeIf { revealSecrets }
        val requestHeaders = secrets?.requestHeaders ?: request.requestHeaders
        val requestBody = if (secrets != null) secrets.requestBody else request.requestBody
        val responseHeaders = secrets?.responseHeaders ?: request.responseHeaders
        val responseBody = if (secrets != null) secrets.responseBody else request.responseBody

        return buildJsonObject {
            put("startedDateTime", formatIso8601(request.timestamp))
            put("time", request.durationMs)
            put("request", buildJsonObject {
                put("method", request.method)
                put("url", request.url)
                put("httpVersion", HTTP_VERSION)
                put("cookies", JsonArray(emptyList()))
                put("headers", nameValueList(requestHeaders))
                put("queryString", nameValueList(queryParameters(request.url)))
                if (!requestBody.isNullOrEmpty()) {
                    put("postData", buildJsonObject {
                        put("mimeType", header(requestHeaders, "Content-Type") ?: "text/plain")
                        put("text", requestBody)
                    })
                }
                put("headersSize", -1)
                put("bodySize", requestBody?.encodeToByteArray()?.size ?: 0)
            })
            put("response", buildJsonObject {
                put("status", request.statusCode ?: 0)
                put("statusText", "")
                put("httpVersion", HTTP_VERSION)
                put("cookies", JsonArray(emptyList()))
                put("headers", nameValueList(responseHeaders))
                put("content", buildJsonObject {
                    put("size", responseBody?.encodeToByteArray()?.size ?: 0)
                    put("mimeType", header(responseHeaders, "Content-Type") ?: "")
                    if (responseBody != null) put("text", responseBody)
                })
                put("redirectURL", header(responseHeaders, "Location") ?: "")
                put("headersSize", -1)
                put("bodySize", responseBody?.encodeToByteArray()?.size ?: 0)
            })
            put("cache", JsonObject(emptyMap()))
            // Bugsy only measures the total, so all of it is reported as wait time (the timings must add up to `time`).
            put("timings", buildJsonObject {
                put("send", 0)
                put("wait", request.durationMs)
                put("receive", 0)
            })
        }
    }

    private fun nameValueList(map: Map<String, String>): JsonArray = JsonArray(
        map.map { (name, value) ->
            buildJsonObject {
                put("name", name)
                put("value", value)
            }
        }
    )

    private fun header(headers: Map<String, String>, name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

    private fun queryParameters(url: String): Map<String, String> {
        val query = url.substringBefore('#').substringAfter('?', missingDelimiterValue = "")
        if (query.isEmpty()) return emptyMap()
        val result = LinkedHashMap<String, String>()
        query.split('&').filter { it.isNotEmpty() }.forEach { pair ->
            result[pair.substringBefore('=')] = pair.substringAfter('=', missingDelimiterValue = "")
        }
        return result
    }

    /** Epoch millis to `yyyy-MM-ddTHH:mm:ss.SSSZ` (UTC), with no date library in commonMain. */
    internal fun formatIso8601(epochMillis: Long): String {
        val days = epochMillis.floorDiv(86_400_000L)
        val millisOfDay = epochMillis.mod(86_400_000L)

        // Days since 1970-01-01 to civil date (Howard Hinnant's algorithm).
        val z = days + 719_468
        val era = z.floorDiv(146_097L)
        val dayOfEra = z - era * 146_097
        val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
        val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
        val monthPart = (5 * dayOfYear + 2) / 153
        val day = dayOfYear - (153 * monthPart + 2) / 5 + 1
        val month = if (monthPart < 10) monthPart + 3 else monthPart - 9
        val year = yearOfEra + era * 400 + if (month <= 2) 1 else 0

        val hours = millisOfDay / 3_600_000
        val minutes = millisOfDay / 60_000 % 60
        val seconds = millisOfDay / 1_000 % 60
        val millis = millisOfDay % 1_000
        return "${year.pad(4)}-${month.pad(2)}-${day.pad(2)}T${hours.pad(2)}:${minutes.pad(2)}:${seconds.pad(2)}.${millis.pad(3)}Z"
    }

    private fun Long.pad(length: Int) = toString().padStart(length, '0')
}
