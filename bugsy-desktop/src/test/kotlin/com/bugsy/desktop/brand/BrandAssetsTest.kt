package com.bugsy.desktop.brand

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Bugsy mark lives in one file and everything else is generated from it (DEB-68). These fail when someone edits the
 * mark and forgets to regenerate, or edits a generated copy by hand: the fix is always
 * `./gradlew :bugsy-desktop:generateBrandAssets`.
 */
class BrandAssetsTest {
    private val root: File = generateSequence(File("").canonicalFile) { it.parentFile }.first { File(it, "assets/brand/bugsy-mark.svg").exists() }
    private val brand = Brand.load(root)
    private val regenerate = "run ./gradlew :bugsy-desktop:generateBrandAssets"

    @Test
    fun `every generated vector is exactly what the master mark produces`() {
        generatedTextFiles(brand).forEach { (path, expected) ->
            val actual = File(root, path).takeIf { it.exists() }?.readText()?.replace("\r\n", "\n")
            assertEquals(expected, actual, "$path is out of date: $regenerate")
        }
    }

    @Test
    fun `the generated binary icons exist and are real images`() {
        generatedBinaryFiles(brand).keys.forEach { path ->
            val file = File(root, path)
            assertTrue(file.exists() && file.length() > 100, "$path is missing: $regenerate")
        }
        assertTrue(File(root, "bugsy-desktop/packaging/icon.icns").length() > 1000, "icon.icns is missing: $regenerate")
    }

    @Test
    fun `the README logo draws the same mark as the master`() {
        // assets/logo.svg also has the wordmark and a background, so it is not generated; it must still contain this mark.
        val logo = File(root, "assets/logo.svg").readText()
        brand.paths.forEach { assertTrue(it in logo, "assets/logo.svg no longer draws the mark in assets/brand/bugsy-mark.svg") }
    }

    @Test
    fun `the colours in the mark match the ones the app uses`() {
        val source = File(root, "ui/src/commonMain/kotlin/com/bugsy/ui/theme/BugsyColors.kt").readText()
        assertTrue(brand.color.removePrefix("#").uppercase() in source.uppercase(), "the mark's colour ${brand.color} is not BugsyColors' brand colour")
        assertTrue(brand.background.removePrefix("#").uppercase() in source.uppercase(), "the icon background ${brand.background} is not a BugsyColors surface colour")
    }

    @Test
    fun `the master parses to what the vectors rely on`() {
        assertEquals(2, brand.paths.size)
        assertEquals(104.0, brand.width)
        assertEquals(92.0, brand.height)
    }
}
