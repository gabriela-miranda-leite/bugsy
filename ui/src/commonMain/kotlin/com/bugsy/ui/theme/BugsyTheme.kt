package com.bugsy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun BugsyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = BugsyColors.Background,
            surface = BugsyColors.Surface,
            onBackground = BugsyColors.TextPrimary,
            onSurface = BugsyColors.TextPrimary,
            outline = BugsyColors.Border,
        ),
        content = content,
    )
}
