package com.bugsy.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography
import com.bugsy.desktop.wire.WireDebugFlag

/**
 * Mirrors the reference design's flag detail panel. [WireDebugFlag]/core's `DebugFlag` carry no description
 * text (only `key`/`label`/`value`/`defaultValue`/`isOverridden`) — the mockup's descriptive sentence under the
 * flag name isn't backed by real data, so this shows [WireDebugFlag.label] instead when it differs from the key
 * (same "nothing invented" rule as the rest of this module).
 */
@Composable
fun FlagDetailPanel(flag: WireDebugFlag?, onToggle: (Boolean) -> Unit, onReset: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(BugsyDesktopColors.Background)) {
        if (flag == null) {
            Text(
                "Select a flag to see its details.",
                style = BugsyDesktopTypography.Body,
                color = BugsyDesktopColors.TextDisabled,
                modifier = Modifier.align(Alignment.Center),
            )
            return@Box
        }

        Column(Modifier.fillMaxSize().padding(BugsyDesktopSpacing.xl), verticalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.lg)) {
            Column(verticalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.xs)) {
                Text(flag.key, style = BugsyDesktopTypography.Heading, color = BugsyDesktopColors.TextPrimary)
                if (flag.label.isNotBlank() && flag.label != flag.key) {
                    Text(flag.label, style = BugsyDesktopTypography.Body, color = BugsyDesktopColors.TextDisabled)
                }
            }

            DetailRow("Type", flag.typeLabel())
            DetailRow("Default", flag.defaultValue.toString())

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.md), modifier = Modifier.fillMaxWidth()) {
                Text("Current", style = BugsyDesktopTypography.Label, color = BugsyDesktopColors.TextDisabled, modifier = Modifier.weight(1f))
                if (flag.value is Boolean) {
                    Switch(
                        checked = flag.value,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = BugsyDesktopColors.Brand,
                            checkedThumbColor = BugsyDesktopColors.TextPrimary,
                            checkedBorderColor = BugsyDesktopColors.Brand,
                            uncheckedTrackColor = BugsyDesktopColors.SwitchOff,
                            uncheckedThumbColor = BugsyDesktopColors.TextPrimary,
                            uncheckedBorderColor = BugsyDesktopColors.SwitchOff,
                        ),
                    )
                } else {
                    Text(flag.value.toString(), style = BugsyDesktopTypography.Code, color = if (flag.isOverridden) BugsyDesktopColors.Brand else BugsyDesktopColors.TextSecondary)
                }
            }

            DetailRow("Overridden", flag.isOverridden.toString(), valueColor = if (flag.isOverridden) BugsyDesktopColors.Brand else BugsyDesktopColors.TextSecondary)

            OutlinedButton(
                onClick = onReset,
                enabled = flag.isOverridden,
                modifier = Modifier.fillMaxWidth().height(44.dp).padding(top = BugsyDesktopSpacing.sm),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, tint = if (flag.isOverridden) BugsyDesktopColors.TextSecondary else BugsyDesktopColors.TextDisabled, modifier = Modifier.size(16.dp))
                Text(
                    "Reset to default",
                    style = BugsyDesktopTypography.Body,
                    color = if (flag.isOverridden) BugsyDesktopColors.TextSecondary else BugsyDesktopColors.TextDisabled,
                    modifier = Modifier.padding(start = BugsyDesktopSpacing.xs),
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = BugsyDesktopColors.TextSecondary) {
    Row(horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.md), modifier = Modifier.fillMaxWidth()) {
        Text(label, style = BugsyDesktopTypography.Label, color = BugsyDesktopColors.TextDisabled, modifier = Modifier.weight(1f))
        Text(value, style = BugsyDesktopTypography.Code, color = valueColor)
    }
}
