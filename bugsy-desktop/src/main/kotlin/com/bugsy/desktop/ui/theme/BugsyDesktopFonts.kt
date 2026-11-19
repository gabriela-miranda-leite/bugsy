package com.bugsy.desktop.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

/**
 * The same Roboto and Roboto Mono the mobile overlay uses (`:ui`'s `BugsyFonts`). The files are `:ui`'s own
 * (see this module's build file), loaded from the classpath because a plain JVM module has no Compose Resources.
 * Same weights as mobile: no SemiBold or Bold mono exists, so those resolve to the nearest weight, as they do there.
 */
object BugsyDesktopFonts {
    val Roboto: FontFamily = FontFamily(
        Font(resource = "roboto_regular.ttf", weight = FontWeight.Normal),
        Font(resource = "roboto_medium.ttf", weight = FontWeight.Medium),
        Font(resource = "roboto_bold.ttf", weight = FontWeight.Bold),
    )

    val RobotoMono: FontFamily = FontFamily(
        Font(resource = "roboto_mono_regular.ttf", weight = FontWeight.Normal),
        Font(resource = "roboto_mono_medium.ttf", weight = FontWeight.Medium),
    )
}
