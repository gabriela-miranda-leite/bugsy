package com.bugsy.ui.screens

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.flags_empty_subtitle
import bugsy.ui.generated.resources.flags_empty_title
import com.bugsy.ui.components.EmptyState
import com.bugsy.ui.theme.BugsyTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Flags ainda não tem uma fonte de dados real — ver DEB-3 (próximas tasks da epic)
 * para o registro de flags e persistência de overrides.
 */
@Composable
fun FlagsScreen() {
    EmptyState(
        title = stringResource(Res.string.flags_empty_title),
        subtitle = stringResource(Res.string.flags_empty_subtitle),
    )
}

@Preview
@Composable
private fun FlagsScreenPreview() {
    BugsyTheme { FlagsScreen() }
}
