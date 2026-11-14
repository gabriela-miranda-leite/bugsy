package com.bugsy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
            // Scrim sobre o app + modal de altura fixa ancorado embaixo; tocar no scrim dispensa o modal.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { Bugsy.hide() },
                contentAlignment = Alignment.BottomCenter,
            ) {
                DebugOverlay(viewModel = remember { DebugViewModel() }, onDismiss = { Bugsy.hide() })
            }
        }
    }
}
