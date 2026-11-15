package com.bugsy.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonSyntaxHighlightTest {

    @Test
    fun `highlightJsonPretty indents like prettyPrintJson`() {
        val result = highlightJsonPretty("""{"a":1,"b":[true,null]}""")

        assertEquals(prettyPrintJson("""{"a":1,"b":[true,null]}"""), result.text)
    }

    @Test
    fun `highlightJsonPretty falls back to the original text unstyled when invalid`() {
        val result = highlightJsonPretty("not json")

        assertEquals("not json", result.text)
        assertEquals(0, result.spanStyles.size)
    }

    @Test
    fun `highlightJsonPretty styles a key differently from a string value`() {
        val result = highlightJsonPretty("""{"key":"value"}""")

        val keySpan = result.spanStyles.first { result.text.substring(it.start, it.end) == "\"key\"" }
        val valueSpan = result.spanStyles.first { result.text.substring(it.start, it.end) == "\"value\"" }
        assertTrue(keySpan.item.color != valueSpan.item.color)
    }

    @Test
    fun `highlightJsonLenient preserves the exact input even when it is invalid JSON`() {
        val input = """{"a": 1, "b": tru"""
        assertEquals(input, highlightJsonLenient(input).text)
    }

    @Test
    fun `highlightJsonLenient colors a key and a string value and a number and a boolean differently`() {
        val result = highlightJsonLenient("""{"id":1,"ok":true,"name":"Ada"}""")

        val idKey = result.spanStyles.first { result.text.substring(it.start, it.end) == "\"id\"" }
        val number = result.spanStyles.first { result.text.substring(it.start, it.end) == "1" }
        val boolean = result.spanStyles.first { result.text.substring(it.start, it.end) == "true" }
        val stringValue = result.spanStyles.first { result.text.substring(it.start, it.end) == "\"Ada\"" }

        val colors = setOf(idKey.item.color, number.item.color, boolean.item.color, stringValue.item.color)
        assertEquals(4, colors.size)
    }

    @Test
    fun `highlightJsonLenient treats a quoted string followed by a colon as a key`() {
        val result = highlightJsonLenient("""{"name": "value"}""")

        val nameSpan = result.spanStyles.first { result.text.substring(it.start, it.end) == "\"name\"" }
        val valueSpan = result.spanStyles.first { result.text.substring(it.start, it.end) == "\"value\"" }
        assertTrue(nameSpan.item.color != valueSpan.item.color)
    }
}
