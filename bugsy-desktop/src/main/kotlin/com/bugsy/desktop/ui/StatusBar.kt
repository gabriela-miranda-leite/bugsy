package com.bugsy.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bugsy.desktop.ConnectionState
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography

@Composable
fun StatusBar(connectionState: ConnectionState, requestCount: Int, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.xs),
        modifier = modifier
            .fillMaxWidth()
            .height(BugsyDesktopSpacing.statusBarHeight)
            .background(BugsyDesktopColors.Surface)
            .padding(horizontal = BugsyDesktopSpacing.statusBarPaddingHorizontal),
    ) {
        val dotColor = when (connectionState) {
            ConnectionState.CONNECTED -> BugsyDesktopColors.Status2xx
            ConnectionState.DISCONNECTED -> BugsyDesktopColors.Status4xx
            ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> BugsyDesktopColors.Status3xx
        }
        Box(Modifier.size(BugsyDesktopSpacing.statusDotSize).background(dotColor, CircleShape))
        Text("$requestCount requests", style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.TextDisabled)
    }
}
