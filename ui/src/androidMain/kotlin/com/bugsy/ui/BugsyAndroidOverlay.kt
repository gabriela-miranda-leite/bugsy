package com.bugsy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.bugsy.core.Bugsy
import com.bugsy.ui.screens.DebugOverlay

/**
 * Entry point Android: embede uma vez no topo da árvore Compose do app host
 * (ex: no Composable raiz da Activity). Reage a [Bugsy.show]/[Bugsy.hide].
 */
@Composable
fun BugsyAndroidOverlay() {
    val visible by Bugsy.isVisible.collectAsState()
    if (visible) {
        Dialog(
            onDismissRequest = { Bugsy.hide() },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            // O escurecimento do fundo é desenhado pelo próprio DebugOverlay (acompanha o arrasto).
            (LocalView.current.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
            DebugOverlay(viewModel = remember { DebugViewModel() }, onDismiss = { Bugsy.hide() })
        }
    }
}
