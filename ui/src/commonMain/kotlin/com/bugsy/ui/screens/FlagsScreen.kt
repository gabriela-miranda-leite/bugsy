package com.bugsy.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.flags_empty_subtitle
import bugsy.ui.generated.resources.flags_empty_title
import com.bugsy.core.model.DebugFlag
import com.bugsy.ui.PreviewFixtures
import com.bugsy.ui.components.EmptyState
import com.bugsy.ui.components.FlagValueRow
import com.bugsy.ui.components.ToggleRow
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Boolean flags get a switch; any other type is shown read-only (overrides from the UI are boolean-only).
 */
@Composable
fun FlagsScreen(
    flags: List<DebugFlag>,
    onOverride: (key: String, value: Any) -> Unit,
    onReset: (key: String) -> Unit,
) {
    if (flags.isEmpty()) {
        EmptyState(
            title = stringResource(Res.string.flags_empty_title),
            subtitle = stringResource(Res.string.flags_empty_subtitle),
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = BugsySpacing.lg, vertical = BugsySpacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(flags, key = { it.key }) { flag ->
            if (flag.value is Boolean) {
                ToggleRow(
                    flag = flag,
                    onToggle = { onOverride(flag.key, it) },
                    onReset = { onReset(flag.key) },
                )
            } else {
                FlagValueRow(flag = flag, onReset = { onReset(flag.key) })
            }
        }
    }
}

@Preview
@Composable
private fun FlagsScreenPreview() {
    BugsyTheme { FlagsScreen(flags = PreviewFixtures.flags, onOverride = { _, _ -> }, onReset = {}) }
}

@Preview
@Composable
private fun FlagsScreenEmptyPreview() {
    BugsyTheme { FlagsScreen(flags = emptyList(), onOverride = { _, _ -> }, onReset = {}) }
}
