package com.bugsy.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.bugsy.desktop.DesktopTab
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography

private val TabIcons = mapOf(
    DesktopTab.REQUESTS to "☰",
    DesktopTab.MOCKS to "▣",
    DesktopTab.FLAGS to "⚑",
    DesktopTab.DEVICE to "▭",
)

@Composable
fun Sidebar(selectedTab: DesktopTab, onSelectTab: (DesktopTab) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.width(BugsyDesktopSpacing.sidebarWidth).fillMaxHeight()
            .background(BugsyDesktopColors.Background)
            .padding(vertical = BugsyDesktopSpacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm),
            modifier = Modifier.padding(horizontal = BugsyDesktopSpacing.lg, vertical = BugsyDesktopSpacing.md),
        ) {
            BugsyIcon(modifier = Modifier.size(BugsyDesktopSpacing.navIconSize * 1.4f))
            Text("Bugsy", style = BugsyDesktopTypography.Heading.copy(fontWeight = FontWeight.Bold), color = BugsyDesktopColors.TextPrimary)
        }

        DesktopTab.entries.forEach { tab ->
            SidebarItem(
                label = tab.label,
                icon = TabIcons.getValue(tab),
                selected = tab == selectedTab,
                onClick = { onSelectTab(tab) },
            )
        }
    }
}

@Composable
private fun SidebarItem(label: String, icon: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.navIconTextSpacing),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BugsyDesktopSpacing.sm)
            .background(
                if (selected) BugsyDesktopColors.Brand.copy(alpha = 0.14f) else BugsyDesktopColors.Background,
                RoundedCornerShape(BugsyDesktopSpacing.controlCorner),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = BugsyDesktopSpacing.navItemPaddingHorizontal)
            .height(BugsyDesktopSpacing.navItemHeight),
    ) {
        Text(icon, color = if (selected) BugsyDesktopColors.Brand else BugsyDesktopColors.TextDisabled)
        Text(
            label,
            style = BugsyDesktopTypography.Body,
            color = if (selected) BugsyDesktopColors.Brand else BugsyDesktopColors.TextSecondary,
        )
    }
}
