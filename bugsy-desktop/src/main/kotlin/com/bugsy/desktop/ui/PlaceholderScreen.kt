package com.bugsy.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography

/** Shown for tabs with no real content yet (Mocks/Timeline/Flags/Device) — each needs its own wire-protocol
 * work beyond what DEB-58 already sends, so they're honest placeholders rather than faked screens. */
@Composable
fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(BugsyDesktopColors.Background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.xs)) {
            Text(title, style = BugsyDesktopTypography.Heading, color = BugsyDesktopColors.TextSecondary)
            Text("Coming soon.", style = BugsyDesktopTypography.Body, color = BugsyDesktopColors.TextDisabled)
        }
    }
}
