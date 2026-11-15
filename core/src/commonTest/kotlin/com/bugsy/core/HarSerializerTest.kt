package com.bugsy.core

import com.bugsy.core.model.DebugRequest
import com.bugsy.core.model.RequestSecrets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class HarSerializerTest {

    private fun request(id: String = "1", secrets: RequestSecrets? = null) = DebugRequest(
        id = id,
        url = "https://api.bugsy.dev/v1/orders?page=2&sort=asc",
        method = "POST",
        requestHeaders = mapOf("Content-Type" to "application/json", "Authorization" to "***"),
        requestBody = """{"password":"***"}""",
        statusCode = 201,
        responseHeaders = mapOf("content-type" to "application/json"),
        responseBody = """{"token":"***"}""",
        durationMs = 128,
        timestamp = 1_700_000_000_123,
        curlCommand = "curl",
        secrets = secrets,
    )

    private val secrets = RequestSecrets(
        requestHeaders = mapOf("Content-Type" to "application/json", "Authorization" to "Bearer real-token"),
        requestBody = """{"password":"p4ss"}""",
        responseHeaders = mapOf("content-type" to "application/json"),
        responseBody = """{"token":"real-response-token"}""",
        curlCommand = "curl real",
    )

    private fun parse(har: String): JsonObject = Json.parseToJsonElement(har).jsonObject

    private fun entries(har: String): JsonArray = parse(har)["log"]!!.jsonObject["entries"]!!.jsonArray

    private fun valueOf(list: JsonArray, name: String): String? =
        list.map { it.jsonObject }.firstOrNull { it["name"]!!.jsonPrimitive.content == name }?.get("value")?.jsonPrimitive?.content

    @Test
    fun `log has the HAR 1_2 version and the Bugsy creator`() {
        val log = parse(HarSerializer.serialize(listOf(request())))["log"]!!.jsonObject

        assertEquals("1.2", log["version"]!!.jsonPrimitive.content)
        assertEquals("Bugsy", log["creator"]!!.jsonObject["name"]!!.jsonPrimitive.content)
        assertEquals(Bugsy.version, log["creator"]!!.jsonObject["version"]!!.jsonPrimitive.content)
    }

    @Test
    fun `masked header is exported masked by default even when secrets exist`() {
        val har = HarSerializer.serialize(listOf(request(secrets = secrets)))

        val headers = entries(har).single().jsonObject["request"]!!.jsonObject["headers"]!!.jsonArray
        assertEquals("***", valueOf(headers, "Authorization"))
        assertFalse(har.contains("real-token"))
        assertFalse(har.contains("p4ss"))
        assertFalse(har.contains("real-response-token"))
    }

    @Test
    fun `revealed secrets export the real values`() {
        val har = HarSerializer.serialize(listOf(request(secrets = secrets)), revealSecrets = true)

        val entry = entries(har).single().jsonObject
        val headers = entry["request"]!!.jsonObject["headers"]!!.jsonArray
        assertEquals("Bearer real-token", valueOf(headers, "Authorization"))
        assertEquals("""{"password":"p4ss"}""", entry["request"]!!.jsonObject["postData"]!!.jsonObject["text"]!!.jsonPrimitive.content)
        assertEquals(
            """{"token":"real-response-token"}""",
            entry["response"]!!.jsonObject["content"]!!.jsonObject["text"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun `revealing a request without secrets keeps its values`() {
        val har = HarSerializer.serialize(listOf(request(secrets = null)), revealSecrets = true)

        val headers = entries(har).single().jsonObject["request"]!!.jsonObject["headers"]!!.jsonArray
        assertEquals("***", valueOf(headers, "Authorization"))
    }

    @Test
    fun `entry carries timing and status plus query string and mime types`() {
        val entry = entries(HarSerializer.serialize(listOf(request()))).single().jsonObject

        assertEquals("2023-11-14T22:13:20.123Z", entry["startedDateTime"]!!.jsonPrimitive.content)
        assertEquals(128L, entry["time"]!!.jsonPrimitive.long)
        assertEquals(128L, entry["timings"]!!.jsonObject["wait"]!!.jsonPrimitive.long)

        val req = entry["request"]!!.jsonObject
        assertEquals("POST", req["method"]!!.jsonPrimitive.content)
        assertEquals("application/json", req["postData"]!!.jsonObject["mimeType"]!!.jsonPrimitive.content)
        val query = req["queryString"]!!.jsonArray
        assertEquals("2", valueOf(query, "page"))
        assertEquals("asc", valueOf(query, "sort"))

        val res = entry["response"]!!.jsonObject
        assertEquals(201, res["status"]!!.jsonPrimitive.int)
        assertEquals("application/json", res["content"]!!.jsonObject["mimeType"]!!.jsonPrimitive.content)
    }

    @Test
    fun `request without body or status has no postData and status 0`() {
        val bare = request().copy(requestBody = null, statusCode = null, responseBody = null)

        val entry = entries(HarSerializer.serialize(listOf(bare))).single().jsonObject

        assertNull(entry["request"]!!.jsonObject["postData"])
        assertEquals(0, entry["response"]!!.jsonObject["status"]!!.jsonPrimitive.int)
        assertNull(entry["response"]!!.jsonObject["content"]!!.jsonObject["text"])
    }

    @Test
    fun `several requests become several entries in order`() {
        val har = HarSerializer.serialize(listOf(request("1"), request("2").copy(method = "GET")))

        val methods = entries(har).map { it.jsonObject["request"]!!.jsonObject["method"]!!.jsonPrimitive.content }
        assertEquals(listOf("POST", "GET"), methods)
    }

    @Test
    fun `formatIso8601 handles the epoch and leap days and pre-epoch times`() {
        assertEquals("1970-01-01T00:00:00.000Z", HarSerializer.formatIso8601(0))
        assertEquals("2024-02-29T12:34:56.789Z", HarSerializer.formatIso8601(1_709_210_096_789))
        assertEquals("1969-12-31T23:59:59.999Z", HarSerializer.formatIso8601(-1))
    }
}
