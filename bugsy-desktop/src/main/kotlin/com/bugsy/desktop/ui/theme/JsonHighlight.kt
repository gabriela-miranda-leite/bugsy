package com.bugsy.desktop.ui.theme

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * A local copy of `:ui`'s `highlightJsonPretty`/`highlightJsonLenient` (DEB-60/DEB-61) — same reasoning as the
 * rest of this module not depending on `:ui`/`:core`.
 */
private val PunctuationStyle = SpanStyle(color = BugsyDesktopColors.TextDisabled)
private val KeyStyle = SpanStyle(color = BugsyDesktopColors.TextSecondary)
private val StringStyle = SpanStyle(color = BugsyDesktopColors.Status2xx)
private val NumberStyle = SpanStyle(color = BugsyDesktopColors.Status3xx)
private val KeywordStyle = SpanStyle(color = BugsyDesktopColors.PatchText)

fun highlightJsonPretty(text: String): AnnotatedString {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return AnnotatedString(text)
    return try {
        val highlighter = JsonSyntaxHighlighter(trimmed)
        val result = buildAnnotatedString { highlighter.appendValue(this, 0) }
        highlighter.skipWhitespace()
        if (!highlighter.isAtEnd()) error("Trailing content after JSON value")
        result
    } catch (e: Exception) {
        AnnotatedString(text)
    }
}

/** Colors JSON while it's still being typed, without reformatting it — one pass over [text], no parse step, so
 * invalid/in-progress JSON still highlights what it can instead of falling back to plain text. */
fun highlightJsonLenient(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            c == '"' -> {
                val start = i
                i++
                while (i < text.length && text[i] != '"') {
                    if (text[i] == '\\' && i + 1 < text.length) i++
                    i++
                }
                if (i < text.length) i++
                var lookahead = i
                while (lookahead < text.length && text[lookahead].isWhitespace()) lookahead++
                val isKey = lookahead < text.length && text[lookahead] == ':'
                withStyle(if (isKey) KeyStyle else StringStyle) { append(text, start, i) }
            }
            c in "{}[],:" -> {
                withStyle(PunctuationStyle) { append(c) }
                i++
            }
            c == '-' || c.isDigit() -> {
                val start = i
                if (c == '-') i++
                while (i < text.length && (text[i].isDigit() || text[i] in ".eE+-")) i++
                withStyle(NumberStyle) { append(text, start, i) }
            }
            text.startsWith("true", i) -> { withStyle(KeywordStyle) { append("true") }; i += 4 }
            text.startsWith("false", i) -> { withStyle(KeywordStyle) { append("false") }; i += 5 }
            text.startsWith("null", i) -> { withStyle(KeywordStyle) { append("null") }; i += 4 }
            else -> { append(c); i++ }
        }
    }
}

private class JsonSyntaxHighlighter(private val input: String) {
    private var pos = 0

    fun isAtEnd() = pos >= input.length

    fun skipWhitespace() {
        while (pos < input.length && input[pos].isWhitespace()) pos++
    }

    fun appendValue(builder: AnnotatedString.Builder, indent: Int) {
        skipWhitespace()
        if (isAtEnd()) error("Unexpected end of input")
        when (input[pos]) {
            '{' -> appendObject(builder, indent)
            '[' -> appendArray(builder, indent)
            '"' -> appendString(builder, StringStyle)
            else -> appendLiteral(builder)
        }
    }

    private fun appendObject(builder: AnnotatedString.Builder, indent: Int) {
        expect('{')
        skipWhitespace()
        if (!isAtEnd() && input[pos] == '}') {
            pos++
            builder.withStyle(PunctuationStyle) { append("{}") }
            return
        }
        builder.withStyle(PunctuationStyle) { append("{\n") }
        val childIndent = indent + 1
        var first = true
        while (true) {
            skipWhitespace()
            if (!first) builder.withStyle(PunctuationStyle) { append(",\n") }
            first = false
            builder.append(indentOf(childIndent))
            appendString(builder, KeyStyle)
            skipWhitespace()
            expect(':')
            builder.withStyle(PunctuationStyle) { append(": ") }
            skipWhitespace()
            appendValue(builder, childIndent)
            skipWhitespace()
            if (isAtEnd()) error("Unterminated object")
            when (input[pos]) {
                ',' -> pos++
                '}' -> { pos++; break }
                else -> error("Expected ',' or '}'")
            }
        }
        builder.append("\n")
        builder.append(indentOf(indent))
        builder.withStyle(PunctuationStyle) { append("}") }
    }

    private fun appendArray(builder: AnnotatedString.Builder, indent: Int) {
        expect('[')
        skipWhitespace()
        if (!isAtEnd() && input[pos] == ']') {
            pos++
            builder.withStyle(PunctuationStyle) { append("[]") }
            return
        }
        builder.withStyle(PunctuationStyle) { append("[\n") }
        val childIndent = indent + 1
        var first = true
        while (true) {
            skipWhitespace()
            if (!first) builder.withStyle(PunctuationStyle) { append(",\n") }
            first = false
            builder.append(indentOf(childIndent))
            appendValue(builder, childIndent)
            skipWhitespace()
            if (isAtEnd()) error("Unterminated array")
            when (input[pos]) {
                ',' -> pos++
                ']' -> { pos++; break }
                else -> error("Expected ',' or ']'")
            }
        }
        builder.append("\n")
        builder.append(indentOf(indent))
        builder.withStyle(PunctuationStyle) { append("]") }
    }

    private fun appendString(builder: AnnotatedString.Builder, style: SpanStyle) {
        expect('"')
        val start = pos - 1
        while (true) {
            if (isAtEnd()) error("Unterminated string")
            val c = input[pos++]
            when (c) {
                '\\' -> {
                    if (isAtEnd()) error("Unterminated escape sequence")
                    pos++
                }
                '"' -> break
            }
        }
        builder.withStyle(style) { append(input.substring(start, pos)) }
    }

    private fun appendLiteral(builder: AnnotatedString.Builder) {
        val start = pos
        while (!isAtEnd() && input[pos] !in ",]}" && !input[pos].isWhitespace()) pos++
        val literal = input.substring(start, pos)
        if (literal != "true" && literal != "false" && literal != "null" && literal.toDoubleOrNull() == null) {
            error("Invalid JSON literal: $literal")
        }
        val style = if (literal.toDoubleOrNull() != null) NumberStyle else KeywordStyle
        builder.withStyle(style) { append(literal) }
    }

    private fun expect(c: Char) {
        if (isAtEnd() || input[pos] != c) error("Expected '$c'")
        pos++
    }

    private fun indentOf(level: Int) = "  ".repeat(level)
}
