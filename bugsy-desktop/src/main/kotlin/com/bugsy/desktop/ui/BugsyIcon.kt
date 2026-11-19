package com.bugsy.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter

/**
 * The Bugsy mark, drawn straight from `assets/brand/bugsy-mark.svg`: the one file every platform's logo is made from,
 * so changing the brand there changes it here (DEB-68). Just the bug shape, no wordmark or background. `bugsy-desktop` is a plain JVM module without
 * the Compose Resources pipeline `:ui` uses, so this loads the SVG straight from the classpath via
 * `loadSvgPainter` instead of a generated `Res.drawable`.
 */
@Composable
fun BugsyIcon(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val painter = remember(density) {
        object {}.javaClass.getResourceAsStream("/bugsy-mark.svg")!!.use { stream -> loadSvgPainter(stream, density) }
    }
    Image(painter = painter, contentDescription = null, modifier = modifier)
}
