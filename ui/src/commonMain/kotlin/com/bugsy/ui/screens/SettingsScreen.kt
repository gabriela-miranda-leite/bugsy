package com.bugsy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.settings_bugsy_version
import bugsy.ui.generated.resources.settings_reveal_default
import bugsy.ui.generated.resources.settings_reveal_hint
import bugsy.ui.generated.resources.settings_section_about
import bugsy.ui.generated.resources.settings_section_privacy
import bugsy.ui.generated.resources.settings_title
import com.bugsy.core.Bugsy
import com.bugsy.ui.components.KeyValueRow
import com.bugsy.ui.components.SectionLabel
import org.jetbrains.compose.resources.stringResource
import com.bugsy.ui.settings.BugsySettings
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography

/** Preferências de quem usa o overlay: dados sensíveis por padrão e a versão do Bugsy. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val revealDefault by BugsySettings.revealSensitiveByDefault.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = BugsySpacing.lg, vertical = BugsySpacing.xl),
        verticalArrangement = Arrangement.spacedBy(BugsySpacing.lg),
    ) {
        item { Text(stringResource(Res.string.settings_title), style = BugsyTypography.Title, color = BugsyColors.TextPrimary) }

        item { SectionLabel(stringResource(Res.string.settings_section_privacy)) }
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BugsySpacing.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { BugsySettings.setRevealSensitiveByDefault(!revealDefault) }
                        .padding(vertical = BugsySpacing.clickableRowVertical),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BugsySpacing.xs)) {
                        Text(stringResource(Res.string.settings_reveal_default), style = BugsyTypography.Body, color = BugsyColors.TextPrimary)
                        Text(stringResource(Res.string.settings_reveal_hint), style = BugsyTypography.Caption, color = BugsyColors.TextDisabled)
                    }
                    Switch(
                        checked = revealDefault,
                        onCheckedChange = { BugsySettings.setRevealSensitiveByDefault(it) },
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
                HorizontalDivider(thickness = BugsySpacing.rowDividerThickness, color = BugsyColors.BorderHeader)
            }
        }

        item { SectionLabel(stringResource(Res.string.settings_section_about)) }
        item { KeyValueRow(stringResource(Res.string.settings_bugsy_version), Bugsy.version) }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    BugsyTheme { Box(Modifier.background(BugsyColors.Surface)) { SettingsScreen() } }
}
