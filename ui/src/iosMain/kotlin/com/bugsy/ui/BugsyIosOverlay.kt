package com.bugsy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController
import com.bugsy.core.Bugsy
import com.bugsy.ui.screens.DebugOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIModalPresentationOverFullScreen
import platform.UIKit.UIViewController

private var presentedController: UIViewController? = null
private var overlayJob: Job? = null

/**
 * Entry point iOS: chame uma vez na inicialização do app (ex: AppDelegate/SceneDelegate)
 * para que [Bugsy.show]/[Bugsy.hide] apresentem/dispensem o overlay sobre o root view
 * controller atual via [ComposeUIViewController].
 */
fun startBugsyIosOverlay() {
    if (overlayJob != null) return
    overlayJob = Bugsy.isVisible
        .onEach { visible -> if (visible) presentOverlay() else dismissOverlay() }
        .launchIn(CoroutineScope(Dispatchers.Main))
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeApi::class)
private fun presentOverlay() {
    if (presentedController != null) return
    val root = currentRootViewController() ?: return

    // O overlay é só de debug: não deve derrubar o app host por falta de CADisableMinimumFrameDurationOnPhone no Info.plist.
    val controller = ComposeUIViewController(
        configure = {
            enforceStrictPlistSanityCheck = false
            opaque = false
        },
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
    controller.modalPresentationStyle = UIModalPresentationOverFullScreen
    controller.view.backgroundColor = UIColor.clearColor
    presentedController = controller
    root.presentViewController(controller, animated = true, completion = null)
}

private fun dismissOverlay() {
    presentedController?.dismissViewControllerAnimated(true, completion = null)
    presentedController = null
}

private fun currentRootViewController(): UIViewController? =
    UIApplication.sharedApplication.keyWindow?.rootViewController
