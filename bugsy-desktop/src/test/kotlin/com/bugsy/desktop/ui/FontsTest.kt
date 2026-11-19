package com.bugsy.desktop.ui

import com.bugsy.desktop.ui.theme.BugsyDesktopFonts
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The desktop uses the same Roboto files as the mobile overlay (DEB-68): they must be on the classpath, and be real fonts. */
class FontsTest {
    private val files = listOf("roboto_regular.ttf", "roboto_medium.ttf", "roboto_bold.ttf", "roboto_mono_regular.ttf", "roboto_mono_medium.ttf")

    @Test
    fun `every font the typography names is bundled`() {
        files.forEach { name ->
            val stream = javaClass.getResourceAsStream("/$name")
            assertNotNull(stream, "$name is not on the classpath")
            stream.use { assertTrue(it.readNBytes(4).contentEquals(byteArrayOf(0, 1, 0, 0)), "$name is not a TrueType font") }
        }
    }

    @Test
    fun `the families are built from them`() {
        assertTrue(BugsyDesktopFonts.Roboto.toString().isNotEmpty())
        assertTrue(BugsyDesktopFonts.RobotoMono.toString().isNotEmpty())
    }
}

class AppIconTest {
    @Test
    fun `the window icon loads`() {
        val painter = appIconPainter()

        assertTrue(painter.intrinsicSize.width > 0f)
    }
}
