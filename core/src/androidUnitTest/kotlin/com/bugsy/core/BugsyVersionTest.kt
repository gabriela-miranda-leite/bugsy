package com.bugsy.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** The library reports the version in `gradle.properties`, read from the file so a release never has a number to edit here. */
class BugsyVersionTest {
    @Test
    fun `version is VERSION_NAME from gradle properties`() {
        val root = generateSequence(File("").canonicalFile) { it.parentFile }.first { File(it, "gradle.properties").exists() }
        val versionName = File(root, "gradle.properties").readLines().first { it.startsWith("VERSION_NAME=") }.substringAfter("=").trim()

        assertEquals(versionName, Bugsy.version)
    }
}
