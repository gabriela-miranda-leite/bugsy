package com.bugsy.desktop.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.bugsy.desktop.ui.theme.BugsyDesktopColors

private val BugsyDesktopColorScheme = darkColorScheme(
    background = BugsyDesktopColors.Background,
    surface = BugsyDesktopColors.Surface,
    surfaceVariant = BugsyDesktopColors.SurfaceCard,
    primary = BugsyDesktopColors.Brand,
    onBackground = BugsyDesktopColors.TextPrimary,
    onSurface = BugsyDesktopColors.TextPrimary,
    outline = BugsyDesktopColors.Border,
    error = BugsyDesktopColors.Status4xx,
)

@Composable
fun BugsyDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BugsyDesktopColorScheme, content = content)
}
