package com.bugsy.desktop.ui

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import java.awt.Taskbar

/** The app icon (generated from `assets/brand/bugsy-mark.svg`, see `generateBrandAssets`): the window's and, below, the Dock's. */
private const val APP_ICON = "/bugsy-app-icon.png"

fun appIconPainter(): BitmapPainter =
    BitmapPainter(object {}.javaClass.getResourceAsStream(APP_ICON)!!.use { loadImageBitmap(it) })

/**
 * Compose's window icon does not reach the macOS Dock, which shows Java's own icon when the app is run from Gradle
 * (a packaged app gets its icon from the .icns). Where the platform has a taskbar icon to set, set it; elsewhere,
 * nothing to do.
 */
fun setTaskbarIcon() {
    runCatching {
        if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
            Taskbar.getTaskbar().iconImage = javax.imageio.ImageIO.read(object {}.javaClass.getResourceAsStream(APP_ICON))
        }
    }
}
