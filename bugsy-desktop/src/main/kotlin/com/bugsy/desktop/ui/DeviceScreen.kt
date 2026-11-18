package com.bugsy.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography
import com.bugsy.desktop.wire.WireDeviceInfo

/**
 * Mirrors `:ui`'s `DeviceInfoScreen` (DEB-60): APP/DEVICE/RUNTIME sections, each row copyable. `DeviceInfo`
 * itself is mostly placeholder values today on mobile too ("Placeholder until a per-platform (Android/iOS)
 * DeviceInfo provider exists" — see DEB-3/`BugsyWsServerHandle.currentDeviceInfo`), so most rows read "—" until
 * that lands; this isn't a desktop bug, it's what mobile's own screen shows. The reference design's Package/
 * Screen/IP rows aren't included — `DeviceInfo` has no such fields, inventing values for them would be fake
 * data. "Connected since" is a real desktop-local fact (not sent by mobile); "Copy as text" is a desktop-only
 * convenience mobile's screen doesn't have.
 */
@Composable
fun DeviceScreen(info: WireDeviceInfo?, connectedAtEpochMs: Long?, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current

    Box(modifier.fillMaxSize().background(BugsyDesktopColors.Surface)) {
        if (info == null) {
            Text(
                "Waiting for device info…",
                style = BugsyDesktopTypography.Body,
                color = BugsyDesktopColors.TextDisabled,
                modifier = Modifier.align(Alignment.Center),
            )
            return@Box
        }

        val rows = buildList {
            add("APP" to listOf("Version" to "${info.appVersionName} (${info.appVersionCode})", "Environment" to info.environment, "Build" to info.buildType))
            add("DEVICE" to listOf("Model" to info.deviceModel, "OS" to "${info.osName} ${info.osVersion}", "Locale" to info.locale))
            add(
                "RUNTIME" to listOfNotNull(
                    "Bugsy" to info.bugsyVersion,
                    "Kotlin" to info.kotlinVersion,
                    "Compose" to info.composeVersion,
                    connectedAtEpochMs?.let { "Connected since" to it.formatHm() },
                ),
            )
        }

        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(BugsyDesktopSpacing.xl), verticalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.lg)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(rows.asText())) }) {
                        Text("Copy as text", style = BugsyDesktopTypography.Body, color = BugsyDesktopColors.TextSecondary)
                    }
                }
            }
            for ((section, entries) in rows) {
                item { SectionLabel(section) }
                items(entries) { (label, value) -> DeviceRow(label, value, onCopy = { clipboard.setText(AnnotatedString(value)) }) }
            }
        }
    }
}

private fun List<Pair<String, List<Pair<String, String>>>>.asText(): String =
    joinToString("\n\n") { (section, entries) -> "$section\n" + entries.joinToString("\n") { (k, v) -> "$k: $v" } }

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = BugsyDesktopTypography.Label, color = BugsyDesktopColors.Brand)
}

@Composable
private fun DeviceRow(label: String, value: String, onCopy: () -> Unit) {
    val flash = rememberCopyFlash()
    Column(
        Modifier.fillMaxWidth().clickable { onCopy(); flash.flash() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm),
            modifier = Modifier.fillMaxWidth().height(36.dp),
        ) {
            Text(label, style = BugsyDesktopTypography.Body, color = BugsyDesktopColors.TextDisabled, modifier = Modifier.weight(0.4f))
            Text(
                value,
                style = BugsyDesktopTypography.Code.copy(color = if (flash.copied) BugsyDesktopColors.Status2xx else BugsyDesktopColors.TextSecondary),
                modifier = Modifier.weight(0.6f),
            )
            Icon(
                if (flash.copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                contentDescription = "Copy",
                tint = if (flash.copied) BugsyDesktopColors.Status2xx else BugsyDesktopColors.TextDisabled,
                modifier = Modifier.size(16.dp),
            )
        }
        HorizontalDivider(color = BugsyDesktopColors.BorderSubtle)
    }
}

private fun Long.formatHm(): String {
    val totalMinutes = (this / 60_000) % (24 * 60)
    val hours = (totalMinutes / 60).toString().padStart(2, '0')
    val minutes = (totalMinutes % 60).toString().padStart(2, '0')
    return "$hours:$minutes"
}
