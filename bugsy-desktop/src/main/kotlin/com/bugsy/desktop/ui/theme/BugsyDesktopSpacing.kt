package com.bugsy.desktop.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Size/spacing design tokens for the desktop app (DEB-60), mirroring `:ui`'s `BugsySpacing` in spirit (same
 * scale) but with its own values — a desktop window has more room than a mobile overlay sheet, so rows,
 * padding and control heights are deliberately a bit larger here, not copied 1:1 like the colors are.
 */
object BugsyDesktopSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp

    // Sidebar nav
    val sidebarWidth = 200.dp
    val navItemHeight = 40.dp
    val navItemPaddingHorizontal = 16.dp
    val navIconSize = 18.dp
    val navIconTextSpacing = 10.dp

    // Request list
    val requestRowVertical = 10.dp
    val requestRowHorizontal = 12.dp
    val requestRowSpacing = 10.dp

    // Filter chips
    val filterChipPaddingHorizontal = 12.dp
    val filterChipPaddingVertical = 6.dp
    val filterChipSpacing = 6.dp
    val filterChipCorner = 50

    // Metric card
    val metricCardPadding = 10.dp
    val metricCardCorner = 8.dp
    val metricLabelSize = 10.sp
    val metricValueSize = 18.sp

    // Code block
    val codeBlockPadding = 10.dp
    val codeBlockCorner = 8.dp
    val codeBlockBorder = 0.5.dp

    // Key/value row
    val keyValueKeyWeight = 0.4f
    val keyValueValueWeight = 0.6f
    val keyValueRowVertical = 6.dp

    // Status bar (footer)
    val statusBarHeight = 32.dp
    val statusBarPaddingHorizontal = 16.dp

    // Controls
    val controlCorner = 8.dp
    val controlBorder = 1.dp
    val dividerThickness = 1.dp

    // Method badge
    val badgeMinWidth = 52.dp
    val badgePaddingHorizontal = 6.dp
    val badgePaddingVertical = 2.dp
    val badgeCorner = 4.dp

    // Form fields (mock rule form, connect screen's manual fields)
    val controlHeight = 36.dp
    val fieldLabelSpacing = 6.dp

    // Mock rule list/form (DEB-61)
    val mockRowVertical = 12.dp
    val mockActiveDotSize = 8.dp
    val mockHeaderRowSpacing = 8.dp
    val mockBodyFieldMinHeight = 140.dp
    val mockFormPanelPadding = 20.dp
}
