package com.bugsy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.mocks_action_new
import bugsy.ui.generated.resources.mocks_empty_action
import bugsy.ui.generated.resources.mocks_empty_link
import bugsy.ui.generated.resources.mocks_empty_subtitle
import bugsy.ui.generated.resources.mocks_empty_title
import com.bugsy.core.model.MockMatch
import com.bugsy.core.model.MockRule
import com.bugsy.ui.PreviewFixtures
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
) {
    var viewing by remember { mutableStateOf<MockRule?>(null) }
    var editing by remember { mutableStateOf<MockRule?>(null) }
    var creating by remember { mutableStateOf(false) }

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
        )
        return
    }

    Column(Modifier.fillMaxWidth()) {
        if (rules.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().padding(horizontal = BugsySpacing.lg, vertical = BugsySpacing.searchBarVerticalMargin),
            ) {
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
            LazyColumn(Modifier.fillMaxWidth()) {
                items(rules, key = { it.id }) { rule ->
                    MockRuleRow(
                        rule = rule,
                        onClick = { viewing = rule },
                        onToggleActive = { onToggleActive(rule.id, it) },
                        onDelete = { onRemoveRule(rule.id) },
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
            MocksScreen(rules = PreviewFixtures.mockRules, onAddRule = {}, onToggleActive = { _, _ -> }, onRemoveRule = {})
        }
    }
}
