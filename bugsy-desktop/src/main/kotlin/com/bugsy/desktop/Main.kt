package com.bugsy.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.bugsy.desktop.ui.BugsyDesktopTheme
import com.bugsy.desktop.ui.MainLayout

fun main() = application {
    val viewModel = remember { DesktopViewModel() }
    Window(
        onCloseRequest = {
            viewModel.onCloseWindow()
            exitApplication()
        },
        title = "Bugsy Desktop",
        state = rememberWindowState(width = 1200.dp, height = 800.dp),
    ) {
        BugsyDesktopTheme {
            MainLayout(viewModel)
        }
    }
}
