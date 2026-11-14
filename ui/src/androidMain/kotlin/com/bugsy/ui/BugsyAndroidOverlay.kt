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
 * Android entry point: embed once at the top of the host app's Compose tree
 * (e.g. in the Activity's root Composable). Reacts to [Bugsy.show]/[Bugsy.hide].
 */
@Composable
fun BugsyAndroidOverlay() {
    val visible by Bugsy.isVisible.collectAsState()
    if (visible) {
        Dialog(
            onDismissRequest = { Bugsy.hide() },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            // The background dimming is drawn by DebugOverlay itself (follows the drag).
            (LocalView.current.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
            DebugOverlay(viewModel = remember { DebugViewModel() }, onDismiss = { Bugsy.hide() })
        }
    }
}
