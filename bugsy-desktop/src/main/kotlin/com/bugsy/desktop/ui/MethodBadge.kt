package com.bugsy.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography
import com.bugsy.desktop.ui.theme.methodColors

@Composable
fun MethodBadge(method: String, modifier: Modifier = Modifier) {
    val (textColor, bgColor) = methodColors(method)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(bgColor, RoundedCornerShape(BugsyDesktopSpacing.badgeCorner))
            .defaultMinSize(minWidth = BugsyDesktopSpacing.badgeMinWidth)
            .padding(horizontal = BugsyDesktopSpacing.badgePaddingHorizontal, vertical = BugsyDesktopSpacing.badgePaddingVertical),
    ) {
        Text(text = method.uppercase(), style = BugsyDesktopTypography.CodeBold, color = textColor, maxLines = 1)
    }
}
