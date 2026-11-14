package com.bugsy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.device_section_account
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
import bugsy.ui.generated.resources.loading_device_title
import bugsy.ui.generated.resources.loading_subtitle_default
import com.bugsy.core.model.DeviceInfo
import com.bugsy.ui.PreviewFixtures
import com.bugsy.ui.components.CopyAffordance
import com.bugsy.ui.components.LoadingState
import com.bugsy.ui.components.SectionLabel
import com.bugsy.ui.components.rememberCopyFlash
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeviceInfoScreen(info: DeviceInfo?, customInfo: List<Pair<String, String>> = emptyList(), isLoading: Boolean = false) {
    if (isLoading || info == null) {
        LoadingState(
            title = stringResource(Res.string.loading_device_title),
            subtitle = stringResource(Res.string.loading_subtitle_default),
        )
        return
    }

    val clipboard = LocalClipboardManager.current
    val rows = listOfNotNull(
        if (customInfo.isEmpty()) null else stringResource(Res.string.device_section_account) to customInfo,
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
            items(entries) { (label, value) ->
                DeviceRow(label, value, onCopy = { clipboard.setText(AnnotatedString(value)) })
            }
        }
    }
}

@Composable
private fun DeviceRow(label: String, value: String, onCopy: () -> Unit) {
    val flash = rememberCopyFlash()
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCopy(); flash.flash() }
                .padding(vertical = BugsySpacing.clickableRowVertical),
        ) {
            Text(
                label,
                style = BugsyTypography.Body,
                color = BugsyColors.TextDisabled,
                modifier = Modifier.alignByBaseline().widthIn(min = BugsySpacing.keyValueKeyMinWidthCompact),
            )
            Text(
                value,
                style = BugsyTypography.Code.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                color = if (flash.copied) BugsyColors.Status2xx else BugsyColors.TextSecondary,
                modifier = Modifier.alignByBaseline().weight(1f),
            )
            CopyAffordance(copied = flash.copied, modifier = Modifier.align(Alignment.CenterVertically))
        }
        HorizontalDivider(thickness = BugsySpacing.rowDividerThickness, color = BugsyColors.BorderHeader)
    }
}

@Preview
@Composable
private fun DeviceInfoScreenPreview() {
    BugsyTheme { DeviceInfoScreen(PreviewFixtures.deviceInfo, customInfo = PreviewFixtures.customInfo) }
}

@Preview
@Composable
private fun DeviceInfoScreenLoadingPreview() {
    BugsyTheme { DeviceInfoScreen(info = null) }
}
