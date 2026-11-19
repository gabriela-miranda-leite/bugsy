package com.bugsy.desktop.ui.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `bugsy-desktop` deliberately doesn't depend on `:ui`/`:core` (own README), so a few pure-logic files are
 * hand-mirrored copies of their `:ui` counterparts: [JsonPrettyPrint.kt] / `:ui`'s `JsonPrettyPrint.kt`, and
 * [JsonHighlight.kt] / `:ui`'s `JsonSyntaxHighlight.kt`. Only the package, doc comments, declaration order and a
 * handful of renamed tokens (this module's own color/type names) are expected to differ; everything else —
 * every statement, in some order — should match. This compares the two copies' significant code lines as sets,
 * so an edit to the parsing/highlighting logic on one side that isn't mirrored on the other fails here instead
 * of silently drifting. (`MethodBadge.kt` mirrors `:ui`'s too, but has diverged enough in styling/preview code
 * that a line-level diff would be noise; it's covered by the cross-referencing doc comments on each copy instead.)
 */
class UiModuleParityTest {
    private val root: File = generateSequence(File("").canonicalFile) { it.parentFile }.first { File(it, "assets/brand/bugsy-mark.svg").exists() }

    @Test
    fun `JsonPrettyPrint mirrors ui's copy`() {
        assertSignificantLinesMatch(
            desktop = File(root, "bugsy-desktop/src/main/kotlin/com/bugsy/desktop/ui/theme/JsonPrettyPrint.kt"),
            ui = File(root, "ui/src/commonMain/kotlin/com/bugsy/ui/theme/JsonPrettyPrint.kt"),
        )
    }

    @Test
    fun `JsonHighlight mirrors ui's JsonSyntaxHighlight`() {
        assertSignificantLinesMatch(
            desktop = File(root, "bugsy-desktop/src/main/kotlin/com/bugsy/desktop/ui/theme/JsonHighlight.kt"),
            ui = File(root, "ui/src/commonMain/kotlin/com/bugsy/ui/theme/JsonSyntaxHighlight.kt"),
            substitutions = listOf(
                "BugsyDesktopColors" to "BugsyColors",
                "PatchText" to "OverrideBadge",
                "androidx.compose.ui.text.AnnotatedString.Builder" to "AnnotatedString.Builder",
                "private val PunctuationStyle" to "internal val PunctuationStyle",
                "private val KeyStyle" to "internal val KeyStyle",
                "private val StringStyle" to "internal val StringStyle",
                "private val NumberStyle" to "internal val NumberStyle",
                "private val KeywordStyle" to "internal val KeywordStyle",
            ),
        )
    }

    private fun assertSignificantLinesMatch(desktop: File, ui: File, substitutions: List<Pair<String, String>> = emptyList()) {
        assertEquals(
            significantLines(ui, substitutions),
            significantLines(desktop, substitutions),
            "${desktop.name} has drifted from ${ui.path}: mirror the change on both sides, or update this test's substitutions if it's only a rename.",
        )
    }

    /** Every non-blank, non-comment, non-package/import code line, trimmed and sorted — so reordering between
     * the two copies doesn't matter, only the actual statements do. */
    private fun significantLines(file: File, substitutions: List<Pair<String, String>>): List<String> =
        file.readText()
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { it.startsWith("//") || it.startsWith("package ") || it.startsWith("import ") }
            .filterNot { it.startsWith("*") || it.startsWith("/**") }
            .map { line -> line.substringBefore(" // ").trimEnd() }
            .map { line -> substitutions.fold(line) { acc, (from, to) -> acc.replace(from, to) } }
            .sorted()
}
