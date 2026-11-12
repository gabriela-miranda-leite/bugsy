package com.bugsy.ui.screens

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.device_info_coming_soon
import com.bugsy.ui.components.EmptyState
import com.bugsy.ui.theme.BugsyTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Device info depende de acesso a Context/Application por plataforma — ainda não existe
 * um mecanismo de inicialização do Bugsy que capture isso. Ver DEB-3 para a task dedicada.
 */
@Composable
fun DeviceInfoScreen() {
    EmptyState(stringResource(Res.string.device_info_coming_soon))
}

@Preview
@Composable
private fun DeviceInfoScreenPreview() {
    BugsyTheme { DeviceInfoScreen() }
}
