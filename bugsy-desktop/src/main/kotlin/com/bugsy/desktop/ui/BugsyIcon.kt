package com.bugsy.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter

/**
 * The real Bugsy mark (cropped from `assets/logo.svg`'s icon, as `bugsy-desktop/src/main/resources/bugsy-icon.svg`
 * — just the bug shape, no wordmark or background), not an emoji. `bugsy-desktop` is a plain JVM module without
 * the Compose Resources pipeline `:ui` uses, so this loads the SVG straight from the classpath via
 * `loadSvgPainter` instead of a generated `Res.drawable`.
 */
@Composable
fun BugsyIcon(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val painter = remember(density) {
        object {}.javaClass.getResourceAsStream("/bugsy-icon.svg")!!.use { stream -> loadSvgPainter(stream, density) }
    }
    Image(painter = painter, contentDescription = null, modifier = modifier)
}
