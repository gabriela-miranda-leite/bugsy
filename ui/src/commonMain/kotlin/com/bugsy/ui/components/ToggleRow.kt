package com.bugsy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.flags_action_reset
import com.bugsy.core.model.DebugFlag
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTypography
import org.jetbrains.compose.resources.stringResource

@Composable
fun ToggleRow(
    flag: DebugFlag,
    onToggle: (Boolean) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.md),
            modifier = Modifier.fillMaxWidth().padding(vertical = BugsySpacing.sm),
        ) {
            FlagLabel(flag, Modifier.weight(1f))
            if (flag.isOverridden) ResetButton(onReset)
            val checked = flag.value == true
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = BugsyColors.Brand,
                    checkedThumbColor = BugsyColors.TextPrimary,
                    uncheckedTrackColor = BugsyColors.SurfaceElevated,
                    uncheckedThumbColor = BugsyColors.TextDisabled,
                    uncheckedBorderColor = BugsyColors.BorderStrong,
                ),
            )
        }
        HorizontalDivider(thickness = BugsySpacing.strokeBorder, color = BugsyColors.BorderSubtle)
    }
}

@Composable
fun FlagValueRow(flag: DebugFlag, onReset: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.md),
            modifier = Modifier.fillMaxWidth().padding(vertical = BugsySpacing.md),
        ) {
            FlagLabel(flag, Modifier.weight(1f))
            if (flag.isOverridden) ResetButton(onReset)
            Text(
                flag.value.toString(),
                style = BugsyTypography.Code.copy(color = BugsyColors.TextSecondary, fontWeight = FontWeight.Medium),
            )
        }
        HorizontalDivider(thickness = BugsySpacing.strokeBorder, color = BugsyColors.BorderSubtle)
    }
}

@Composable
private fun FlagLabel(flag: DebugFlag, modifier: Modifier) {
    Column(modifier) {
        Text(flag.label, style = BugsyTypography.Body, color = BugsyColors.TextPrimary)
        Text(flag.key, style = BugsyTypography.CodeSmall, color = BugsyColors.TextDisabled)
    }
}

@Composable
private fun ResetButton(onReset: () -> Unit) {
    Text(
        stringResource(Res.string.flags_action_reset),
        style = BugsyTypography.Caption.copy(fontWeight = FontWeight.Medium),
        color = BugsyColors.Brand,
        modifier = Modifier.clickable(onClick = onReset).padding(BugsySpacing.xs),
    )
}
