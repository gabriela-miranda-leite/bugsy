package com.bugsy.desktop.ui.theme

/** A local copy of `:ui`'s `prettyPrintJson` (DEB-60/DEB-61) — same reasoning as the rest of this module not
 * depending on `:ui`/`:core`. Pretty-prints [text] as JSON with 2-space indentation; returns it unchanged if it
 * isn't valid JSON. */
fun prettyPrintJson(text: String): String {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return text
    return try {
        val parser = JsonPrettyPrinter(trimmed)
        val result = parser.parseValue(0)
        parser.skipWhitespace()
        if (!parser.isAtEnd()) error("Trailing content after JSON value")
        result
    } catch (e: Exception) {
        text
    }
}

private class JsonPrettyPrinter(private val input: String) {
    private var pos = 0

    fun isAtEnd() = pos >= input.length

    fun skipWhitespace() {
        while (pos < input.length && input[pos].isWhitespace()) pos++
    }

    fun parseValue(indent: Int): String {
        skipWhitespace()
        if (isAtEnd()) error("Unexpected end of input")
        return when (input[pos]) {
            '{' -> parseObject(indent)
            '[' -> parseArray(indent)
            '"' -> parseString()
            else -> parseLiteral()
        }
    }

    private fun parseObject(indent: Int): String {
        expect('{')
        skipWhitespace()
        if (!isAtEnd() && input[pos] == '}') {
            pos++
            return "{}"
        }
        val childIndent = indent + 1
        val entries = mutableListOf<String>()
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            skipWhitespace()
            val value = parseValue(childIndent)
            entries += "${indentOf(childIndent)}$key: $value"
            skipWhitespace()
            if (isAtEnd()) error("Unterminated object")
            when (input[pos]) {
                ',' -> pos++
                '}' -> { pos++; break }
                else -> error("Expected ',' or '}'")
            }
        }
        return "{\n${entries.joinToString(",\n")}\n${indentOf(indent)}}"
    }

    private fun parseArray(indent: Int): String {
        expect('[')
        skipWhitespace()
        if (!isAtEnd() && input[pos] == ']') {
            pos++
            return "[]"
        }
        val childIndent = indent + 1
        val entries = mutableListOf<String>()
        while (true) {
            skipWhitespace()
            val value = parseValue(childIndent)
            entries += "${indentOf(childIndent)}$value"
            skipWhitespace()
            if (isAtEnd()) error("Unterminated array")
            when (input[pos]) {
                ',' -> pos++
                ']' -> { pos++; break }
                else -> error("Expected ',' or ']'")
            }
        }
        return "[\n${entries.joinToString(",\n")}\n${indentOf(indent)}]"
    }

    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder("\"")
        while (true) {
            if (isAtEnd()) error("Unterminated string")
            val c = input[pos++]
            when (c) {
                '\\' -> {
                    if (isAtEnd()) error("Unterminated escape sequence")
                    sb.append(c).append(input[pos++])
                }
                '"' -> {
                    sb.append(c)
                    return sb.toString()
                }
                else -> sb.append(c)
            }
        }
    }

    private fun parseLiteral(): String {
        val start = pos
        while (!isAtEnd() && input[pos] !in ",]}" && !input[pos].isWhitespace()) pos++
        val literal = input.substring(start, pos)
        if (literal != "true" && literal != "false" && literal != "null" && literal.toDoubleOrNull() == null) {
            error("Invalid JSON literal: $literal")
        }
        return literal
    }

    private fun expect(c: Char) {
        if (isAtEnd() || input[pos] != c) error("Expected '$c'")
        pos++
    }

    private fun indentOf(level: Int) = "  ".repeat(level)
}
