package com.bugsy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.device_action_copy_all
import bugsy.ui.generated.resources.device_label_bugsy
import bugsy.ui.generated.resources.device_label_build
import bugsy.ui.generated.resources.device_label_compose
import bugsy.ui.generated.resources.device_label_environment
import bugsy.ui.generated.resources.device_label_kotlin
import bugsy.ui.generated.resources.device_label_locale
import bugsy.ui.generated.resources.device_label_model
import bugsy.ui.generated.resources.device_label_os
import bugsy.ui.generated.resources.device_label_version
import bugsy.ui.generated.resources.device_section_app
import bugsy.ui.generated.resources.device_section_device
import bugsy.ui.generated.resources.device_section_runtime
import com.bugsy.core.model.DeviceInfo
import com.bugsy.ui.PreviewFixtures
import com.bugsy.ui.components.SectionLabel
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeviceInfoScreen(info: DeviceInfo) {
    val clipboard = LocalClipboardManager.current
    val rows = listOf(
        stringResource(Res.string.device_section_app) to listOf(
            stringResource(Res.string.device_label_version) to "${info.appVersionName} (${info.appVersionCode})",
            stringResource(Res.string.device_label_environment) to info.environment,
            stringResource(Res.string.device_label_build) to info.buildType,
        ),
        stringResource(Res.string.device_section_device) to listOf(
            stringResource(Res.string.device_label_model) to info.deviceModel,
            stringResource(Res.string.device_label_os) to "${info.osName} ${info.osVersion}",
            stringResource(Res.string.device_label_locale) to info.locale,
        ),
        stringResource(Res.string.device_section_runtime) to listOf(
            stringResource(Res.string.device_label_bugsy) to info.bugsyVersion,
            stringResource(Res.string.device_label_kotlin) to info.kotlinVersion,
            stringResource(Res.string.device_label_compose) to info.composeVersion,
        ),
    )

    LazyColumn(
        contentPadding = PaddingValues(horizontal = BugsySpacing.lg, vertical = BugsySpacing.md),
        verticalArrangement = Arrangement.spacedBy(BugsySpacing.lg),
    ) {
        for ((section, entries) in rows) {
            item { SectionLabel(section) }
            items(entries) { (label, value) -> DeviceRow(label, value) }
        }

        item {
            OutlinedButton(
                onClick = {
                    val text = rows.joinToString("\n") { (section, entries) ->
                        "$section\n" + entries.joinToString("\n") { (label, value) -> "$label: $value" }
                    }
                    clipboard.setText(AnnotatedString(text))
                },
                border = BorderStroke(BugsySpacing.strokeBorder, BugsyColors.BorderButton),
                shape = RoundedCornerShape(BugsySpacing.cornerCard),
                modifier = Modifier.fillMaxWidth().height(BugsySpacing.copyAllButtonHeight),
            ) {
                Text(
                    stringResource(Res.string.device_action_copy_all),
                    style = BugsyTypography.Body,
                    color = BugsyColors.TextDisabled,
                )
            }
        }
    }
}

@Composable
private fun DeviceRow(label: String, value: String) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
            modifier = Modifier.fillMaxWidth().padding(vertical = BugsySpacing.keyValueRowVertical),
        ) {
            Text(
                label,
                style = BugsyTypography.Body,
                color = BugsyColors.TextDisabled,
                modifier = Modifier.widthIn(min = BugsySpacing.keyValueKeyMinWidthCompact),
            )
            Text(
                value,
                style = BugsyTypography.Code.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                color = BugsyColors.TextSecondary,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(thickness = BugsySpacing.strokeBorder, color = BugsyColors.BorderSubtle)
    }
}

@Preview
@Composable
private fun DeviceInfoScreenPreview() {
    BugsyTheme { DeviceInfoScreen(PreviewFixtures.deviceInfo) }
}
