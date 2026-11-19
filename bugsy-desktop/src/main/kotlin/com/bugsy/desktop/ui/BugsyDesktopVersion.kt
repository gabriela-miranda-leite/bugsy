package com.bugsy.desktop.ui

import java.util.Properties

/**
 * The version shown in the status bar: the packaged app's own, and the Bugsy library version it follows (DEB-70). Both
 * are written at build time from `VERSION_NAME` (see this module's build file), so they can't drift from the installer.
 * Without that file (an IDE run that skipped the Gradle step) it says so instead of showing an invented number.
 */
object BugsyDesktopVersion {
    private val properties = Properties().apply {
        BugsyDesktopVersion::class.java.getResourceAsStream("/bugsy-version.properties")?.use { load(it) }
    }

    val app: String = properties.getProperty("app") ?: "dev"
    val library: String = properties.getProperty("library") ?: "unknown"

    /** `v1.3.0 · Bugsy 0.3.0-alpha`. */
    val label: String = "v$app · Bugsy $library"
}
