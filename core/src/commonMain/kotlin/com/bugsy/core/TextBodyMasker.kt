package com.bugsy.core

/**
 * Masks text that isn't valid JSON (form-encoded, truncated or too-deep JSON): `"field": value` and
 * `field=value`. **Iterative** scan, no regex: Kotlin/Native's regex engine is recursive and a 1 MB body
 * crashes the process (SIGSEGV), which must never happen in the host app.
 */
internal object TextBodyMasker {
    private const val MASK = "***"

    fun mask(body: String, fields: Set<String>): String {
        val spans = mutableListOf<Span>()
        for (field in fields) {
            collectJsonLikeSpans(body, field, spans)
            collectFormEncodedSpans(body, field, spans)
        }
        if (spans.isEmpty()) return body

        val out = StringBuilder(body.length)
        var cursor = 0
        for (span in spans.sortedBy { it.start }) {
            if (span.start < cursor) continue // overlap: the previous span already covers this one
            out.append(body, cursor, span.start).append(span.replacement)
            cursor = span.end
        }
        out.append(body, cursor, body.length)
        return out.toString()
    }

    private class Span(val start: Int, val end: Int, val replacement: String)

    /** `"field"` + `:` + value (quoted string, even unclosed, or a loose token) becomes `"field":"***"`. */
    private fun collectJsonLikeSpans(body: String, field: String, spans: MutableList<Span>) {
        val needle = "\"$field\""
        var from = 0
        while (true) {
            val keyStart = body.indexOf(needle, from, ignoreCase = true)
            if (keyStart < 0) return
            from = keyStart + needle.length

            var i = from
            while (i < body.length && body[i].isWhitespace()) i++
            if (i >= body.length || body[i] != ':') continue
            i++
            while (i < body.length && body[i].isWhitespace()) i++
            if (i >= body.length) continue

            val valueEnd = if (body[i] == '"') {
                var j = i + 1
                while (j < body.length && body[j] != '"') j += if (body[j] == '\\') 2 else 1
                minOf(if (j < body.length) j + 1 else body.length, body.length)
            } else {
                var j = i
                while (j < body.length && body[j] !in ",}] \t\r\n") j++
                j
            }
            if (valueEnd == i) continue

            val key = body.substring(keyStart, keyStart + needle.length)
            val separator = body.substring(keyStart + needle.length, i)
            spans += Span(keyStart, valueEnd, "$key$separator\"$MASK\"")
            from = valueEnd
        }
    }

    /** `field=value` (value up to `&` or whitespace) becomes `field=***`; the preceding character can't be part of an identifier. */
    private fun collectFormEncodedSpans(body: String, field: String, spans: MutableList<Span>) {
        val needle = "$field="
        var from = 0
        while (true) {
            val start = body.indexOf(needle, from, ignoreCase = true)
            if (start < 0) return
            from = start + needle.length

            val before = if (start > 0) body[start - 1] else ' '
            if (before.isLetterOrDigit() || before == '_' || before == '-') continue

            var end = from
            while (end < body.length && body[end] != '&' && !body[end].isWhitespace()) end++
            spans += Span(start, end, body.substring(start, start + field.length) + "=" + MASK)
            from = end
        }
    }
}
