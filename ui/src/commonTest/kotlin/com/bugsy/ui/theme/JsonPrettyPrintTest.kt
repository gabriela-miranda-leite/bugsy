package com.bugsy.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonPrettyPrintTest {

    @Test
    fun `prettyPrintJson indents an object with 2 spaces`() {
        assertEquals(
            "{\n  \"id\": 42,\n  \"name\": \"Ada\"\n}",
            prettyPrintJson("""{"id":42,"name":"Ada"}"""),
        )
    }

    @Test
    fun `prettyPrintJson indents nested arrays and objects`() {
        assertEquals(
            "{\n  \"items\": [\n    {\n      \"sku\": \"BUG-1\"\n    }\n  ]\n}",
            prettyPrintJson("""{"items":[{"sku":"BUG-1"}]}"""),
        )
    }

    @Test
    fun `prettyPrintJson handles empty objects and arrays`() {
        assertEquals("{}", prettyPrintJson("{}"))
        assertEquals("[]", prettyPrintJson("[]"))
    }

    @Test
    fun `prettyPrintJson returns plain text unchanged when not valid JSON`() {
        assertEquals("not json at all", prettyPrintJson("not json at all"))
        assertEquals("<html></html>", prettyPrintJson("<html></html>"))
    }

    @Test
    fun `prettyPrintJson returns blank text unchanged`() {
        assertEquals("", prettyPrintJson(""))
        assertEquals("   ", prettyPrintJson("   "))
    }
}
