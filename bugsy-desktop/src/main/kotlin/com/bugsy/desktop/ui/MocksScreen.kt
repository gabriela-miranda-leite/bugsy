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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography
import com.bugsy.desktop.ui.theme.statusColor
import com.bugsy.desktop.wire.WireMockRule
import com.bugsy.desktop.wire.displayName

/**
 * Mirrors `:ui`'s `MocksScreen`/`MockRuleRow` (DEB-61): no search field — mobile's own Mocks tab doesn't have
 * one either, just Import/Export/"+ New mock rule" (import/export aren't implemented here yet, see the class
 * doc on `MockRuleFormPanel`). A table layout (Method / URL pattern / Status / Delay / Options columns) matches
 * the reference design more closely than mobile's stacked card rows; the row content itself (active dot,
 * method badge, status, delay, ONCE tag, toggle, delete) is mobile's.
 */
@Composable
fun MocksScreen(
    rules: List<WireMockRule>,
    selectedRuleId: String?,
    onSelectRule: (String) -> Unit,
    onNewRule: () -> Unit,
    onToggleActive: (id: String, enabled: Boolean) -> Unit,
    onDeleteRule: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(BugsyDesktopColors.Surface)) {
        Row(Modifier.fillMaxWidth().padding(BugsyDesktopSpacing.md)) {
            Button(
                onClick = onNewRule,
                colors = ButtonDefaults.buttonColors(containerColor = BugsyDesktopColors.Brand, contentColor = BugsyDesktopColors.Surface),
                modifier = Modifier.height(BugsyDesktopSpacing.controlHeight),
            ) {
                Text("+ New mock rule", style = BugsyDesktopTypography.Body.copy(fontWeight = FontWeight.Medium))
            }
        }

        if (rules.isEmpty()) {
            Box(Modifier.fillMaxSize()) {
                Text(
                    "No mock rules yet.",
                    style = BugsyDesktopTypography.Body,
                    color = BugsyDesktopColors.TextDisabled,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = BugsyDesktopSpacing.md, vertical = BugsyDesktopSpacing.xs),
        ) {
            ColumnHeader("Method", Modifier.width(72.dp))
            ColumnHeader("URL pattern (regex)", Modifier.weight(1f))
            ColumnHeader("Status", Modifier.width(56.dp))
            ColumnHeader("Delay", Modifier.width(64.dp))
            ColumnHeader("Options", Modifier.width(136.dp))
        }
        HorizontalDivider(color = BugsyDesktopColors.BorderHeader)

        LazyColumn(Modifier.fillMaxSize()) {
            items(rules, key = { it.id }) { rule ->
                MockRuleRow(
                    rule = rule,
                    selected = rule.id == selectedRuleId,
                    onClick = { onSelectRule(rule.id) },
                    onToggleActive = { onToggleActive(rule.id, it) },
                    onDelete = { onDeleteRule(rule.id) },
                )
            }
        }
    }
}

@Composable
private fun ColumnHeader(text: String, modifier: Modifier = Modifier) {
    Text(text, style = BugsyDesktopTypography.Label, color = BugsyDesktopColors.TextDisabled, modifier = modifier)
}

@Composable
private fun MockRuleRow(
    rule: WireMockRule,
    selected: Boolean,
    onClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) BugsyDesktopColors.Brand.copy(alpha = 0.1f) else BugsyDesktopColors.Surface)
            .clickable(onClick = onClick)
            .drawBehind { drawLine(BugsyDesktopColors.BorderSubtle, Offset(0f, size.height), Offset(size.width, size.height), 1f) }
            .padding(horizontal = BugsyDesktopSpacing.md, vertical = BugsyDesktopSpacing.mockRowVertical),
    ) {
        Row(Modifier.width(72.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.xs)) {
            ActiveDot(active = rule.isActive)
            MethodBadge(rule.method ?: "ANY")
        }
        Text(
            rule.displayName,
            style = BugsyDesktopTypography.Code.copy(color = BugsyDesktopColors.TextSecondary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = BugsyDesktopSpacing.sm),
        )
        Box(Modifier.width(56.dp)) { StatusBadge(rule.statusCode) }
        Text(
            "${rule.delayMs}ms",
            style = BugsyDesktopTypography.Caption,
            color = BugsyDesktopColors.TextDisabled,
            modifier = Modifier.width(64.dp),
        )
        Row(Modifier.width(136.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.xs)) {
            if (rule.isOneShot) OnceTag()
            Switch(
                checked = rule.isActive,
                onCheckedChange = onToggleActive,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = BugsyDesktopColors.SwitchOn,
                    checkedThumbColor = BugsyDesktopColors.TextPrimary,
                    checkedBorderColor = BugsyDesktopColors.SwitchOn,
                    uncheckedTrackColor = BugsyDesktopColors.SwitchOff,
                    uncheckedThumbColor = BugsyDesktopColors.TextPrimary,
                    uncheckedBorderColor = BugsyDesktopColors.SwitchOff,
                ),
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete mock rule", tint = BugsyDesktopColors.Status4xx, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ActiveDot(active: Boolean) {
    Box(Modifier.size(BugsyDesktopSpacing.mockActiveDotSize).background(if (active) BugsyDesktopColors.SwitchOn else BugsyDesktopColors.TextDisabled, CircleShape))
}

@Composable
private fun StatusBadge(statusCode: Int) {
    Box(Modifier.background(statusColor(statusCode).copy(alpha = 0.16f), RoundedCornerShape(BugsyDesktopSpacing.badgeCorner)).padding(horizontal = BugsyDesktopSpacing.badgePaddingHorizontal, vertical = BugsyDesktopSpacing.badgePaddingVertical)) {
        Text(statusCode.toString(), style = BugsyDesktopTypography.CodeBold, color = statusColor(statusCode))
    }
}

@Composable
private fun OnceTag() {
    Box(Modifier.background(BugsyDesktopColors.Status3xx.copy(alpha = 0.16f), RoundedCornerShape(BugsyDesktopSpacing.badgeCorner)).padding(horizontal = BugsyDesktopSpacing.badgePaddingHorizontal, vertical = BugsyDesktopSpacing.badgePaddingVertical)) {
        Text("ONCE", style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.Status3xx)
    }
}
