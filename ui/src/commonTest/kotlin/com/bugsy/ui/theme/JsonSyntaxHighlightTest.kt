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

    @Test
    fun `highlightJsonPretty indents a top-level array with multiple items`() {
        val result = highlightJsonPretty("""[1,2,3]""")

        assertEquals(prettyPrintJson("""[1,2,3]"""), result.text)
    }

    @Test
    fun `highlightJsonPretty renders an empty object and an empty array compactly`() {
        assertEquals("""{}""", highlightJsonPretty("""{}""").text)
        assertEquals("""[]""", highlightJsonPretty("""[]""").text)
    }

    @Test
    fun `highlightJsonPretty keeps a negative decimal with an exponent as one number token`() {
        val result = highlightJsonPretty("""{"n":-1.5e10}""")

        assertEquals(prettyPrintJson("""{"n":-1.5e10}"""), result.text)
        val numberSpan = result.spanStyles.first { result.text.substring(it.start, it.end) == "-1.5e10" }
        assertEquals(NumberStyle, numberSpan.item)
    }

    @Test
    fun `highlightJsonPretty keeps an escaped quote inside a string as part of that string`() {
        val input = """{"msg":"say \"hi\""}"""
        val result = highlightJsonPretty(input)

        assertEquals(prettyPrintJson(input), result.text)
    }

    @Test
    fun `highlightJsonPretty falls back unstyled on trailing content after a valid value`() {
        val result = highlightJsonPretty("""{"a":1} garbage""")

        assertEquals("""{"a":1} garbage""", result.text)
        assertEquals(0, result.spanStyles.size)
    }

    @Test
    fun `highlightJsonPretty highlights a bare top-level literal`() {
        val trueResult = highlightJsonPretty("true")
        val numberResult = highlightJsonPretty("42")

        assertEquals(KeywordStyle, trueResult.spanStyles.single().item)
        assertEquals(NumberStyle, numberResult.spanStyles.single().item)
    }

    @Test
    fun `highlightJsonLenient colors a negative number`() {
        val result = highlightJsonLenient("""{"n":-3.5}""")

        val numberSpan = result.spanStyles.first { result.text.substring(it.start, it.end) == "-3.5" }
        assertEquals(NumberStyle, numberSpan.item)
    }

    @Test
    fun `highlightJsonLenient colors false and null keywords`() {
        val result = highlightJsonLenient("""{"a":false,"b":null}""")

        val falseSpan = result.spanStyles.first { result.text.substring(it.start, it.end) == "false" }
        val nullSpan = result.spanStyles.first { result.text.substring(it.start, it.end) == "null" }
        assertEquals(KeywordStyle, falseSpan.item)
        assertEquals(KeywordStyle, nullSpan.item)
    }

    @Test
    fun `highlightJsonLenient does not color an escaped quote as the end of the string`() {
        val input = """{"msg":"say \"hi\""}"""
        val result = highlightJsonLenient(input)

        assertEquals(input, result.text)
        val msgValueSpan = result.spanStyles.first { result.text.substring(it.start, it.end) == "\"say \\\"hi\\\"\"" }
        assertEquals(StringStyle, msgValueSpan.item)
    }

    @Test
    fun `highlightJsonLenient passes through whitespace and other characters unstyled`() {
        val result = highlightJsonLenient("  \n")

        assertEquals("  \n", result.text)
        assertEquals(0, result.spanStyles.size)
    }
}
