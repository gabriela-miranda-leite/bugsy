package com.bugsy.desktop.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The version the app shows is the one the build derived from `VERSION_NAME` (DEB-70). */
class DesktopVersionResourceTest {
    private val libraryVersion: String = generateSequence(File("").canonicalFile) { it.parentFile }
        .first { File(it, "gradle.properties").exists() }
        .let { File(it, "gradle.properties").readLines().first { line -> line.startsWith("VERSION_NAME=") }.substringAfter("=").trim() }

    @Test
    fun `the library version in the app is VERSION_NAME`() {
        assertEquals(libraryVersion, BugsyDesktopVersion.library)
    }

    @Test
    fun `the app version is a plain X_Y_Z the installers accept`() {
        assertTrue(Regex("""\d+\.\d+\.\d+""").matches(BugsyDesktopVersion.app), BugsyDesktopVersion.app)
    }

    @Test
    fun `the status bar text names both versions`() {
        assertEquals("v${BugsyDesktopVersion.app} · Bugsy $libraryVersion", BugsyDesktopVersion.label)
    }
}
