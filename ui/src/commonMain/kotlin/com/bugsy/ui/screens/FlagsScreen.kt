package com.bugsy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.flags_action_reset_all
import bugsy.ui.generated.resources.flags_empty_subtitle
import bugsy.ui.generated.resources.flags_empty_title
import com.bugsy.core.model.DebugFlag
import com.bugsy.ui.PreviewFixtures
import com.bugsy.ui.components.EmptyState
import com.bugsy.ui.components.FlagValueRow
import com.bugsy.ui.components.ToggleRow
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Boolean flags get a switch; any other type is shown read-only (overrides from the UI are boolean-only).
 */
@Composable
fun FlagsScreen(
    flags: List<DebugFlag>,
    onOverride: (key: String, value: Any) -> Unit,
    onResetAll: () -> Unit,
) {
    if (flags.isEmpty()) {
        EmptyState(
            title = stringResource(Res.string.flags_empty_title),
            subtitle = stringResource(Res.string.flags_empty_subtitle),
        )
        return
    }

    val overrideCount = flags.count { it.isOverridden }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = BugsySpacing.xl, vertical = BugsySpacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(flags, key = { it.key }) { flag ->
            if (flag.value is Boolean) {
                ToggleRow(flag = flag, onToggle = { onOverride(flag.key, it) })
            } else {
                FlagValueRow(flag = flag)
            }
        }
        if (overrideCount > 0) {
            item {
                OutlinedButton(
                    onClick = onResetAll,
                    border = BorderStroke(BugsySpacing.strokeBorder, BugsyColors.BorderButton),
                    shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = BugsySpacing.lg)
                        .height(BugsySpacing.resetAllButtonHeight),
                ) {
                    Text(
                        stringResource(Res.string.flags_action_reset_all, overrideCount),
                        style = BugsyTypography.Body.copy(fontSize = 16.sp),
                        color = BugsyColors.Status4xx,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun FlagsScreenPreview() {
    BugsyTheme { FlagsScreen(flags = PreviewFixtures.flags, onOverride = { _, _ -> }, onResetAll = {}) }
}

@Preview
@Composable
private fun FlagsScreenEmptyPreview() {
    BugsyTheme { FlagsScreen(flags = emptyList(), onOverride = { _, _ -> }, onResetAll = {}) }
}
