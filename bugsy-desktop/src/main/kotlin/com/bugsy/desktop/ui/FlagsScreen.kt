package com.bugsy.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography
import com.bugsy.desktop.wire.WireDebugFlag

/**
 * Mirrors `:ui`'s `FlagsScreen` (DEB-60): boolean flags get a toggle, any other type is read-only in the UI
 * (overrides are boolean-only, same restriction mobile has — `FlagManager.override` itself accepts `Any`, but
 * neither UI exposes a way to type a new string/number value). The "Filter flags" search box is desktop-only;
 * mobile's own Flags tab has no search. A table layout (Flag / Type / Default / Current / Override) plus a
 * detail panel on selection matches the reference design more closely than mobile's single-column list.
 */
@Composable
fun FlagsScreen(
    flags: List<WireDebugFlag>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedKey: String?,
    onSelectFlag: (String) -> Unit,
    onToggle: (key: String, enabled: Boolean) -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtered = flags.filter { searchQuery.isBlank() || it.key.contains(searchQuery, ignoreCase = true) }
    val overrideCount = flags.count { it.isOverridden }

    Column(modifier.fillMaxSize().background(BugsyDesktopColors.Surface)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm),
            modifier = Modifier.fillMaxWidth().padding(BugsyDesktopSpacing.md),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Filter flags", style = BugsyDesktopTypography.Body, color = BugsyDesktopColors.TextDisabled) },
                singleLine = true,
                textStyle = BugsyDesktopTypography.Body.copy(color = BugsyDesktopColors.TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BugsyDesktopColors.Brand,
                    unfocusedBorderColor = BugsyDesktopColors.Border,
                    focusedContainerColor = BugsyDesktopColors.SurfaceCard,
                    unfocusedContainerColor = BugsyDesktopColors.SurfaceCard,
                ),
                modifier = Modifier.weight(1f),
            )
            if (overrideCount > 0) {
                OutlinedButton(onClick = onResetAll, modifier = Modifier.height(BugsyDesktopSpacing.controlHeight)) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, tint = BugsyDesktopColors.Status4xx, modifier = Modifier.size(16.dp))
                    Text(
                        "Reset all overrides ($overrideCount)",
                        style = BugsyDesktopTypography.Body,
                        color = BugsyDesktopColors.Status4xx,
                        modifier = Modifier.padding(start = BugsyDesktopSpacing.xs),
                    )
                }
            }
        }

        if (flags.isEmpty()) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm)) {
                    Icon(Icons.Outlined.Flag, contentDescription = null, tint = BugsyDesktopColors.TextDisabled, modifier = Modifier.size(40.dp))
                    Text("No feature flags", style = BugsyDesktopTypography.Heading, color = BugsyDesktopColors.TextPrimary)
                    Text(
                        "The connected app isn't exposing any flags to Bugsy.",
                        style = BugsyDesktopTypography.Body,
                        color = BugsyDesktopColors.TextDisabled,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            return@Column
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = BugsyDesktopSpacing.md, vertical = BugsyDesktopSpacing.xs)) {
            ColumnHeader("Flag", Modifier.weight(1f))
            ColumnHeader("Type", Modifier.width(80.dp))
            ColumnHeader("Default", Modifier.width(90.dp))
            ColumnHeader("Current", Modifier.width(90.dp))
            ColumnHeader("Override", Modifier.width(96.dp))
        }
        androidx.compose.material3.HorizontalDivider(color = BugsyDesktopColors.BorderHeader)

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize()) {
                Text("No flags match \"$searchQuery\".", style = BugsyDesktopTypography.Body, color = BugsyDesktopColors.TextDisabled, modifier = Modifier.align(Alignment.Center))
            }
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(filtered, key = { it.key }) { flag ->
                FlagRow(flag = flag, selected = flag.key == selectedKey, onClick = { onSelectFlag(flag.key) }, onToggle = { onToggle(flag.key, it) })
            }
        }
    }
}

@Composable
private fun ColumnHeader(text: String, modifier: Modifier = Modifier) {
    Text(text, style = BugsyDesktopTypography.Label, color = BugsyDesktopColors.TextDisabled, modifier = modifier)
}

@Composable
private fun FlagRow(flag: WireDebugFlag, selected: Boolean, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) BugsyDesktopColors.Brand.copy(alpha = 0.1f) else BugsyDesktopColors.Surface)
            .clickable(onClick = onClick)
            .drawBehind { drawLine(BugsyDesktopColors.BorderSubtle, Offset(0f, size.height), Offset(size.width, size.height), 1f) }
            .padding(horizontal = BugsyDesktopSpacing.md, vertical = BugsyDesktopSpacing.mockRowVertical),
    ) {
        Text(flag.key, style = BugsyDesktopTypography.Code.copy(color = BugsyDesktopColors.TextSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = BugsyDesktopSpacing.sm))
        Text(flag.typeLabel(), style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.TextDisabled, modifier = Modifier.width(80.dp))
        Text(flag.defaultValue.toString(), style = BugsyDesktopTypography.Code, color = BugsyDesktopColors.TextDisabled, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(90.dp))
        Box(Modifier.width(90.dp)) {
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
                Text(
                    flag.value.toString(),
                    style = BugsyDesktopTypography.Code,
                    color = if (flag.isOverridden) BugsyDesktopColors.Brand else BugsyDesktopColors.TextSecondary,
                )
            }
        }
        Box(Modifier.width(96.dp)) {
            if (flag.isOverridden) OverrideTag() else Text("—", style = BugsyDesktopTypography.Body, color = BugsyDesktopColors.TextDisabled)
        }
    }
}

@Composable
private fun OverrideTag() {
    Box(
        Modifier
            .border(BugsyDesktopSpacing.controlBorder, BugsyDesktopColors.Brand, RoundedCornerShape(BugsyDesktopSpacing.badgeCorner))
            .background(BugsyDesktopColors.Brand.copy(alpha = 0.12f), RoundedCornerShape(BugsyDesktopSpacing.badgeCorner))
            .padding(horizontal = BugsyDesktopSpacing.badgePaddingHorizontal, vertical = BugsyDesktopSpacing.badgePaddingVertical),
    ) {
        Text("OVERRIDE", style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.Brand)
    }
}

internal fun WireDebugFlag.typeLabel(): String = when (value) {
    is Boolean -> "boolean"
    is Int, is Long, is Double -> "number"
    else -> "string"
}
