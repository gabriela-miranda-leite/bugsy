package com.bugsy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.action_export
import bugsy.ui.generated.resources.action_import
import bugsy.ui.generated.resources.action_merge
import bugsy.ui.generated.resources.action_replace
import bugsy.ui.generated.resources.mocks_action_new
import bugsy.ui.generated.resources.mocks_empty_action
import bugsy.ui.generated.resources.mocks_empty_link
import bugsy.ui.generated.resources.mocks_empty_subtitle
import bugsy.ui.generated.resources.mocks_empty_title
import bugsy.ui.generated.resources.mocks_import_dialog_message
import bugsy.ui.generated.resources.mocks_import_dialog_title
import bugsy.ui.generated.resources.toast_import_invalid_malformed
import bugsy.ui.generated.resources.toast_import_invalid_missing_field
import bugsy.ui.generated.resources.toast_import_invalid_title
import bugsy.ui.generated.resources.toast_import_invalid_version
import bugsy.ui.generated.resources.toast_mocks_exported_subtitle
import bugsy.ui.generated.resources.toast_mocks_exported_title
import bugsy.ui.generated.resources.toast_mocks_imported_subtitle_merge
import bugsy.ui.generated.resources.toast_mocks_imported_subtitle_replace
import bugsy.ui.generated.resources.toast_mocks_imported_title
import com.bugsy.core.MockRuleFileResult
import com.bugsy.core.MockRuleFileSerializer
import com.bugsy.core.model.MockMatch
import com.bugsy.core.model.MockRule
import com.bugsy.core.model.conflictsWith
import com.bugsy.ui.PreviewFixtures
import com.bugsy.ui.components.BugsyToastAction
import com.bugsy.ui.components.BugsyToastData
import com.bugsy.ui.components.BugsyToastHost
import com.bugsy.ui.components.BugsyToastType
import com.bugsy.ui.components.EmptyState
import com.bugsy.ui.components.MockRuleRow
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import org.jetbrains.compose.resources.stringResource

/**
 * List of [MockRule]s with "+ New mock rule", pushing [MockRuleDetailScreen] on tap (and from there,
 * [MockRuleFormScreen] via its Edit button), the same way [RequestListScreen] pushes `RequestDetailScreen`.
 *
 * @param prefillRule when set (from "Create mock from this response" on a request), opens the form directly,
 * pre-filled with it; [prefillSourceLabel] is the banner text, and [onPrefillConsumed] clears both fields once
 * the form is dismissed or saved so reopening the tab doesn't reopen the same prefill.
 * @param onExport when non-null, shows an export icon button; called with the serialized JSON to share.
 * @param onPickImportFile when non-null, shows an import icon button that triggers the platform's file picker.
 * @param importedFileContent the picked file's raw text, set by the platform layer once the picker returns;
 * validated here, then — if valid — offered to the user as a merge/replace choice before [onImportRules] runs.
 */
@Composable
fun MocksScreen(
    rules: List<MockRule>,
    onAddRule: (MockRule) -> Unit,
    onToggleActive: (id: String, active: Boolean) -> Unit,
    onRemoveRule: (id: String) -> Unit,
    matchesFor: (id: String) -> List<MockMatch> = { emptyList() },
    prefillRule: MockRule? = null,
    prefillSourceLabel: String? = null,
    onPrefillConsumed: () -> Unit = {},
    onNavigateToRequests: () -> Unit = {},
    onExport: ((json: String) -> Unit)? = null,
    onPickImportFile: (() -> Unit)? = null,
    importedFileContent: String? = null,
    onImportFileConsumed: () -> Unit = {},
    onImportRules: (rules: List<MockRule>, replace: Boolean) -> Unit = { _, _ -> },
) {
    var viewing by remember { mutableStateOf<MockRule?>(null) }
    var editing by remember { mutableStateOf<MockRule?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<List<MockRule>?>(null) }

    val invalidTitle = stringResource(Res.string.toast_import_invalid_title)
    val invalidMalformed = stringResource(Res.string.toast_import_invalid_malformed)
    val invalidVersion = stringResource(Res.string.toast_import_invalid_version)
    val invalidMissingField = stringResource(Res.string.toast_import_invalid_missing_field)
    val importedTitle = stringResource(Res.string.toast_mocks_imported_title)
    val importedSubtitleMerge = stringResource(Res.string.toast_mocks_imported_subtitle_merge, pendingImport?.size ?: 0)
    val importedSubtitleReplace = stringResource(Res.string.toast_mocks_imported_subtitle_replace, pendingImport?.size ?: 0)

    if (importedFileContent != null) {
        when (val result = MockRuleFileSerializer.deserialize(importedFileContent)) {
            is MockRuleFileResult.Success -> pendingImport = result.rules
            is MockRuleFileResult.Invalid -> {
                val subtitle = when (result.reason) {
                    MockRuleFileResult.Reason.MALFORMED_JSON -> invalidMalformed
                    MockRuleFileResult.Reason.UNKNOWN_VERSION -> invalidVersion
                    MockRuleFileResult.Reason.MISSING_FIELD -> invalidMissingField
                }
                BugsyToastHost.show(BugsyToastData(type = BugsyToastType.Error, title = invalidTitle, subtitle = subtitle))
            }
        }
        onImportFileConsumed()
    }

    pendingImport?.let { parsedRules ->
        ImportModeDialog(
            count = parsedRules.size,
            onMerge = {
                onImportRules(parsedRules, false)
                BugsyToastHost.show(BugsyToastData(type = BugsyToastType.Success, title = importedTitle, subtitle = importedSubtitleMerge))
                pendingImport = null
            },
            onReplace = {
                onImportRules(parsedRules, true)
                BugsyToastHost.show(BugsyToastData(type = BugsyToastType.Success, title = importedTitle, subtitle = importedSubtitleReplace))
                pendingImport = null
            },
            onDismiss = { pendingImport = null },
        )
    }

    if (prefillRule != null && !creating) {
        creating = true
    }

    if (creating) {
        MockRuleFormScreen(
            initial = editing ?: prefillRule,
            prefillSourceLabel = if (editing == null) prefillSourceLabel else null,
            onBack = {
                creating = false
                editing = null
                onPrefillConsumed()
            },
            onSave = { rule ->
                onAddRule(rule)
                creating = false
                editing = null
                viewing = null
                onPrefillConsumed()
            },
        )
        return
    }

    viewing?.let { rule ->
        // Ids can disappear while viewing (e.g. the rule was deleted elsewhere): fall back to what we have.
        val current = rules.firstOrNull { it.id == rule.id } ?: rule
        MockRuleDetailScreen(
            rule = current,
            matches = matchesFor(current.id),
            onBack = { viewing = null },
            onToggleActive = { onToggleActive(current.id, it) },
            onEdit = { editing = current; creating = true },
            onDelete = { onRemoveRule(current.id); viewing = null },
            hasConflict = rules.any { it.conflictsWith(current) },
        )
        return
    }

    val exportedTitle = stringResource(Res.string.toast_mocks_exported_title)
    val activeCount = rules.count { it.isActive }
    val exportedSubtitle = stringResource(Res.string.toast_mocks_exported_subtitle, activeCount)

    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
            modifier = Modifier.fillMaxWidth().padding(horizontal = BugsySpacing.lg, vertical = BugsySpacing.searchBarVerticalMargin),
        ) {
            if (onPickImportFile != null) {
                IconActionButton(
                    icon = Icons.Outlined.Upload,
                    label = stringResource(Res.string.action_import),
                    onClick = onPickImportFile,
                )
            }
            if (onExport != null) {
                IconActionButton(
                    icon = Icons.Outlined.Download,
                    label = stringResource(Res.string.action_export),
                    enabled = activeCount > 0,
                    onClick = {
                        onExport(MockRuleFileSerializer.serialize(rules))
                        BugsyToastHost.show(
                            BugsyToastData(type = BugsyToastType.Success, title = exportedTitle, subtitle = exportedSubtitle),
                        )
                    },
                )
            }
            Box(Modifier.weight(1f))
            if (rules.isNotEmpty()) {
                Button(
                    onClick = { creating = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BugsyColors.Brand, contentColor = BugsyColors.Surface),
                    shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge),
                    modifier = Modifier.height(BugsySpacing.controlHeight),
                ) {
                    Text(
                        stringResource(Res.string.mocks_action_new),
                        style = BugsyTypography.Body.copy(fontWeight = FontWeight.Medium),
                    )
                }
            }
        }
        if (rules.isNotEmpty()) {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(rules, key = { it.id }) { rule ->
                    MockRuleRow(
                        rule = rule,
                        onClick = { viewing = rule },
                        onToggleActive = { onToggleActive(rule.id, it) },
                        onDelete = { onRemoveRule(rule.id) },
                        hasConflict = rules.any { it.conflictsWith(rule) },
                    )
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().padding(top = BugsySpacing.xxl)) {
                EmptyState(
                    title = stringResource(Res.string.mocks_empty_title),
                    subtitle = stringResource(Res.string.mocks_empty_subtitle),
                    icon = Icons.Outlined.Shield,
                    actionLabel = stringResource(Res.string.mocks_empty_action),
                    onAction = { creating = true },
                    secondaryLabel = stringResource(Res.string.mocks_empty_link),
                    onSecondaryAction = onNavigateToRequests,
                )
            }
        }
    }
}

@Composable
private fun IconActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val tint = if (enabled) BugsyColors.Brand else BugsyColors.TextDisabled
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BugsySpacing.xs),
        modifier = Modifier
            .height(BugsySpacing.controlHeight)
            .background(BugsyColors.SurfaceCard, RoundedCornerShape(BugsySpacing.cornerCard))
            .border(BugsySpacing.strokeBorder, BugsyColors.Border, RoundedCornerShape(BugsySpacing.cornerCard))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = BugsySpacing.md),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(BugsySpacing.iconSizeMedium))
        Text(label, style = BugsyTypography.Caption.copy(fontWeight = FontWeight.Medium), color = tint)
    }
}

@Composable
private fun ImportModeDialog(count: Int, onMerge: () -> Unit, onReplace: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BugsyColors.SurfaceCard,
        title = { Text(stringResource(Res.string.mocks_import_dialog_title), style = BugsyTypography.Body, color = BugsyColors.TextPrimary) },
        text = {
            Text(
                stringResource(Res.string.mocks_import_dialog_message, count),
                style = BugsyTypography.Caption,
                color = BugsyColors.TextMuted,
            )
        },
        confirmButton = {
            TextButton(onClick = onMerge) {
                Text(stringResource(Res.string.action_merge), color = BugsyColors.Brand)
            }
        },
        dismissButton = {
            TextButton(onClick = onReplace) {
                Text(stringResource(Res.string.action_replace), color = BugsyColors.Status4xx)
            }
        },
    )
}

@Preview
@Composable
private fun MocksScreenEmptyPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            MocksScreen(rules = emptyList(), onAddRule = {}, onToggleActive = { _, _ -> }, onRemoveRule = {})
        }
    }
}

@Preview
@Composable
private fun MocksScreenWithDataPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            MocksScreen(
                rules = PreviewFixtures.mockRules,
                onAddRule = {},
                onToggleActive = { _, _ -> },
                onRemoveRule = {},
                onExport = {},
                onPickImportFile = {},
            )
        }
    }
}
