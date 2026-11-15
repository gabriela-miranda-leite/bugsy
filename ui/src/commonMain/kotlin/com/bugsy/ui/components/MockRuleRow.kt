package com.bugsy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.cd_delete_mock_rule
import bugsy.ui.generated.resources.cd_toggle_mock_rule
import bugsy.ui.generated.resources.tag_mock_once
import com.bugsy.core.model.MockRule
import com.bugsy.core.model.displayName
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography

/** A rule row: active dot, method + URL pattern, status + body preview, a one-shot tag, a toggle and a delete icon. */
@Composable
fun MockRuleRow(
    rule: MockRule,
    onClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggleDescription = stringResource(Res.string.cd_toggle_mock_rule)
    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = BugsySpacing.rowVertical),
        ) {
            ActiveDot(active = rule.isActive)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BugsySpacing.xs)) {
                Row(horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    MethodBadge(rule.method ?: "ANY")
                    Text(
                        rule.displayName,
                        style = BugsyTypography.Code.copy(color = BugsyColors.TextSecondary, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (!rule.name.isNullOrBlank()) {
                    Text(
                        rule.urlPattern,
                        style = BugsyTypography.Caption,
                        color = BugsyColors.TextDisabled,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${rule.statusCode} · ${rule.responseBody.ifBlank { "—" }}",
                        style = BugsyTypography.Caption,
                        color = BugsyColors.TextDisabled,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (rule.isOneShot) OneShotTag()
                }
            }
            Switch(
                checked = rule.isActive,
                onCheckedChange = onToggleActive,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = BugsyColors.SwitchOn,
                    checkedThumbColor = BugsyColors.TextPrimary,
                    checkedBorderColor = BugsyColors.SwitchOn,
                    uncheckedTrackColor = BugsyColors.SwitchOff,
                    uncheckedThumbColor = BugsyColors.TextPrimary,
                    uncheckedBorderColor = BugsyColors.SwitchOff,
                ),
                modifier = Modifier.semantics { contentDescription = toggleDescription },
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(Res.string.cd_delete_mock_rule),
                    tint = BugsyColors.Status4xx,
                    modifier = Modifier.size(BugsySpacing.iconSizeMedium),
                )
            }
        }
        HorizontalDivider(thickness = BugsySpacing.strokeBorder, color = BugsyColors.BorderSubtle)
    }
}

@Composable
private fun ActiveDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(BugsySpacing.mockActiveDotSize)
            .background(if (active) BugsyColors.SwitchOn else BugsyColors.TextDisabled, CircleShape),
    )
}

@Composable
private fun OneShotTag() {
    Box(
        modifier = Modifier
            .background(BugsyColors.Status3xx.copy(alpha = 0.16f), RoundedCornerShape(BugsySpacing.cornerBadge))
            .padding(horizontal = BugsySpacing.badgePaddingHorizontal, vertical = BugsySpacing.badgePaddingVertical),
    ) {
        Text(
            stringResource(Res.string.tag_mock_once),
            style = BugsyTypography.Caption,
            color = BugsyColors.Status3xx,
        )
    }
}

@Preview
@Composable
private fun MockRuleRowPreview() {
    BugsyTheme {
        Column(Modifier.background(BugsyColors.Surface)) {
            MockRuleRow(
                rule = MockRule(id = "1", urlPattern = "/api/users.*", method = "GET", statusCode = 200, responseBody = """{"users": []}"""),
                onClick = {},
                onToggleActive = {},
                onDelete = {},
            )
            MockRuleRow(
                rule = MockRule(
                    id = "2",
                    urlPattern = "/api/login",
                    name = "Login failure",
                    method = "POST",
                    statusCode = 401,
                    responseBody = """{"error": "unauthorized"}""",
                    isOneShot = true,
                    isActive = false,
                ),
                onClick = {},
                onToggleActive = {},
                onDelete = {},
            )
        }
    }
}
