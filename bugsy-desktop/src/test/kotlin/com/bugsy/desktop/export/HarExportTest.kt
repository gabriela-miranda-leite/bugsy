package com.bugsy.desktop.export

import com.bugsy.desktop.wire.WireDebugRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class HarExportTest {
    private val json = Json

    @Test
    fun `builds a HAR with one entry per request`() {
        val requests = listOf(
            WireDebugRequest(id = "1", url = "https://api.bugsy.dev/v1/users", method = "GET", statusCode = 200, durationMs = 42),
            WireDebugRequest(id = "2", url = "https://api.bugsy.dev/v1/cart", method = "POST", statusCode = 201, durationMs = 10),
        )

        val har = json.parseToJsonElement(buildHar(requests)).jsonObject
        val entries = har["log"]!!.jsonObject["entries"]!!.jsonArray

        assertEquals(2, entries.size)
        assertEquals("GET", entries[0].jsonObject["request"]!!.jsonObject["method"]!!.jsonPrimitive.content)
        assertEquals(200, entries[0].jsonObject["response"]!!.jsonObject["status"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun `includes request and response bodies when present`() {
        val requests = listOf(
            WireDebugRequest(
                id = "1",
                url = "https://api.bugsy.dev/v1/login",
                method = "POST",
                requestBody = """{"email":"a@b.com"}""",
                responseBody = """{"token":"abc"}""",
                statusCode = 200,
            ),
        )

        val entry = json.parseToJsonElement(buildHar(requests)).jsonObject["log"]!!.jsonObject["entries"]!!.jsonArray[0].jsonObject

        assertEquals(
            """{"email":"a@b.com"}""",
            entry["request"]!!.jsonObject["postData"]!!.jsonObject["text"]!!.jsonPrimitive.content,
        )
        assertEquals("""{"token":"abc"}""", entry["response"]!!.jsonObject["content"]!!.jsonObject["text"]!!.jsonPrimitive.content)
    }

    @Test
    fun `builds a valid empty log for no requests`() {
        val entries = json.parseToJsonElement(buildHar(emptyList())).jsonObject["log"]!!.jsonObject["entries"]!!.jsonArray
        assertEquals(0, entries.size)
    }
}
