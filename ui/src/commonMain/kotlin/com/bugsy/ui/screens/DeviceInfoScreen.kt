package com.bugsy.ui.screens

import androidx.compose.runtime.Composable
import com.bugsy.ui.components.EmptyState

/**
 * Device info depende de acesso a Context/Application por plataforma — ainda não existe
 * um mecanismo de inicialização do Bugsy que capture isso. Ver DEB-3 para a task dedicada.
 */
@Composable
fun DeviceInfoScreen() {
    EmptyState("Device info chegando em breve")
}
