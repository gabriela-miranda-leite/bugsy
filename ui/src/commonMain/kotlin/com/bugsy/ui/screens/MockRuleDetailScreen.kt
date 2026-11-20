package com.bugsy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.action_delete
import bugsy.ui.generated.resources.action_edit
import bugsy.ui.generated.resources.label_duration
import bugsy.ui.generated.resources.label_response_body
import bugsy.ui.generated.resources.label_status
import bugsy.ui.generated.resources.mocks_empty_last_matches
import bugsy.ui.generated.resources.mocks_label_hits
import bugsy.ui.generated.resources.mocks_section_last_matches
import bugsy.ui.generated.resources.tab_mocks
import bugsy.ui.generated.resources.tag_mock
import com.bugsy.core.model.MockMatch
import com.bugsy.core.model.MockRule
import com.bugsy.core.model.displayName
import com.bugsy.core.model.displayPattern
import com.bugsy.ui.PreviewFixtures
import com.bugsy.ui.components.CodeBlock
import com.bugsy.ui.components.ExpandableSection
import com.bugsy.ui.components.MethodBadge
import com.bugsy.ui.components.MetricCard
import com.bugsy.ui.components.SectionLabel
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import com.bugsy.ui.theme.formatDuration
import com.bugsy.ui.theme.formatLocalHms
import org.jetbrains.compose.resources.stringResource

/** Read-only view of a [MockRule]: metrics, recent matches, response body, and Edit/Delete actions. */
@Composable
fun MockRuleDetailScreen(
    rule: MockRule,
    matches: List<MockMatch>,
    onBack: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .drawBehind {
                    val s = BugsySpacing.strokeBorder.toPx()
                    drawLine(BugsyColors.BorderHeader, Offset(0f, size.height), Offset(size.width, size.height), s)
                }
                .padding(start = BugsySpacing.md, end = BugsySpacing.lg, top = BugsySpacing.xs, bottom = BugsySpacing.xs),
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = BugsySpacing.md, vertical = BugsySpacing.md)) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = BugsyColors.Brand,
                    modifier = Modifier.size(BugsySpacing.iconSizeMedium),
                )
                Spacer(Modifier.width(BugsySpacing.backButtonIconSpacing))
                Text(stringResource(Res.string.tab_mocks), style = BugsyTypography.Body, color = BugsyColors.Brand)
            }
            Spacer(Modifier.weight(1f))
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
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = BugsySpacing.lg, vertical = BugsySpacing.xl),
            verticalArrangement = Arrangement.spacedBy(BugsySpacing.xl),
            modifier = Modifier.weight(1f),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm)) {
                    MethodBadge(rule.method ?: "ANY")
                    Text(
                        rule.displayName,
                        style = BugsyTypography.Code.copy(color = BugsyColors.TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (!rule.name.isNullOrBlank()) {
                item {
                    Text(
                        rule.displayPattern,
                        style = BugsyTypography.Caption,
                        color = BugsyColors.TextDisabled,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm)) {
                    MetricCard(stringResource(Res.string.label_status), rule.statusCode.toString(), modifier = Modifier.weight(1f))
                    MetricCard(
                        stringResource(Res.string.label_duration),
                        formatDuration(rule.delayMs),
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(stringResource(Res.string.mocks_label_hits), rule.hitCount.toString(), modifier = Modifier.weight(1f))
                }
            }

            item { SectionLabel(stringResource(Res.string.mocks_section_last_matches)) }
            if (matches.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.mocks_empty_last_matches),
                        style = BugsyTypography.Caption,
                        color = BugsyColors.TextDisabled,
                    )
                }
            } else {
                items(matches) { match -> MatchRow(match) }
            }

            item {
                ExpandableSection(
                    stringResource(Res.string.label_response_body),
                    initiallyExpanded = true,
                    onCopy = { clipboard.setText(AnnotatedString(rule.responseBody)) },
                ) {
                    CodeBlock(rule.responseBody, json = true)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onEdit,
                        border = BorderStroke(BugsySpacing.strokeBorder, BugsyColors.BorderButton),
                        shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge),
                        modifier = Modifier.weight(1f).height(BugsySpacing.resetAllButtonHeight),
                    ) {
                        Text(stringResource(Res.string.action_edit), style = BugsyTypography.Body, color = BugsyColors.TextPrimary)
                    }
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = BugsyColors.Status4xx, contentColor = BugsyColors.Surface),
                        shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge),
                        modifier = Modifier.weight(1f).height(BugsySpacing.resetAllButtonHeight),
                    ) {
                        Text(stringResource(Res.string.action_delete), style = BugsyTypography.Body.copy(fontWeight = FontWeight.Medium))
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchRow(match: MockMatch) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = BugsySpacing.sm),
        ) {
            Text(
                match.url,
                style = BugsyTypography.Code.copy(color = BugsyColors.TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(formatLocalHms(match.timestamp), style = BugsyTypography.Caption, color = BugsyColors.TextDisabled)
            MockTag()
        }
        HorizontalDivider(thickness = BugsySpacing.strokeBorder, color = BugsyColors.BorderSubtle)
    }
}

@Composable
private fun MockTag() {
    Box(
        modifier = Modifier
            .background(BugsyColors.OverrideBadge.copy(alpha = 0.16f), RoundedCornerShape(BugsySpacing.cornerBadge))
            .padding(horizontal = BugsySpacing.badgePaddingHorizontal, vertical = BugsySpacing.badgePaddingVertical),
    ) {
        Text(stringResource(Res.string.tag_mock), style = BugsyTypography.Caption, color = BugsyColors.OverrideBadge)
    }
}

@Preview
@Composable
private fun MockRuleDetailScreenPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            MockRuleDetailScreen(
                rule = PreviewFixtures.mockRules.first(),
                matches = listOf(
                    MockMatch(url = "/api/v1/feed?page=1", timestamp = 1_700_000_000_000),
                    MockMatch(url = "/api/v1/feed?page=2", timestamp = 1_700_000_050_000),
                ),
                onBack = {},
                onToggleActive = {},
                onEdit = {},
                onDelete = {},
            )
        }
    }
}
