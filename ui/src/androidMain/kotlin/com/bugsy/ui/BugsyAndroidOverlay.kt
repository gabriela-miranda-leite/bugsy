package com.bugsy.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                DebugOverlay(viewModel = remember { DebugViewModel() }, onDismiss = { Bugsy.hide() })
            }
        }
    }
}
