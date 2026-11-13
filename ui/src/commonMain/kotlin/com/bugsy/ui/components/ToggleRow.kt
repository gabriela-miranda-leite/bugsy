package com.bugsy.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.flags_badge_override
import com.bugsy.core.model.DebugFlag
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTypography
import org.jetbrains.compose.resources.stringResource

@Composable
fun ToggleRow(flag: DebugFlag, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.md),
            modifier = Modifier.fillMaxWidth().padding(vertical = BugsySpacing.sm),
        ) {
            Column(Modifier.weight(1f)) {
                Text(flag.key, style = BugsyTypography.Body.copy(fontSize = 16.sp), color = BugsyColors.TextSecondary)
                if (flag.isOverridden) {
                    Spacer(Modifier.height(BugsySpacing.flagBadgeSpacing))
                    OverrideBadge()
                }
            }
            Switch(
                checked = flag.value == true,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = BugsyColors.SwitchOn,
                    checkedThumbColor = BugsyColors.TextPrimary,
                    checkedBorderColor = BugsyColors.SwitchOn,
                    uncheckedTrackColor = BugsyColors.SwitchOff,
                    uncheckedThumbColor = BugsyColors.TextPrimary,
                    uncheckedBorderColor = BugsyColors.SwitchOff,
                ),
            )
        }
        HorizontalDivider(thickness = BugsySpacing.strokeBorder, color = BugsyColors.BorderSubtle)
    }
}

@Composable
fun FlagValueRow(flag: DebugFlag, modifier: Modifier = Modifier) {
    val value = flag.value
    Column(modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
            modifier = Modifier.fillMaxWidth().padding(vertical = BugsySpacing.flagRowVertical),
        ) {
            Text(
                flag.key,
                style = BugsyTypography.Code.copy(color = BugsyColors.TextDisabled),
                modifier = Modifier.widthIn(min = BugsySpacing.flagValueKeyMinWidth),
            )
            Text(
                if (value is String) "\"$value\"" else value.toString(),
                style = BugsyTypography.Code.copy(color = BugsyColors.TextPrimary, fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(thickness = BugsySpacing.strokeBorder, color = BugsyColors.BorderSubtle)
    }
}

@Composable
private fun OverrideBadge() {
    Text(
        stringResource(Res.string.flags_badge_override),
        style = BugsyTypography.Caption.copy(fontWeight = FontWeight.Medium),
        color = BugsyColors.OverrideBadge,
        modifier = Modifier
            .border(BugsySpacing.strokeBorder, BugsyColors.OverrideBadge, CircleShape)
            .padding(horizontal = BugsySpacing.flagBadgePaddingHorizontal, vertical = BugsySpacing.flagBadgePaddingVertical),
    )
}
