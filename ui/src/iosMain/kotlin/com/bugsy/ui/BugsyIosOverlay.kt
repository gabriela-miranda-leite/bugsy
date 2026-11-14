package com.bugsy.ui

import androidx.compose.runtime.remember
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.ExperimentalComposeUiApi
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
 * iOS entry point: call once during app startup (e.g. AppDelegate/SceneDelegate)
 * so that [Bugsy.show]/[Bugsy.hide] present/dismiss the overlay over the current root view
 * controller via [ComposeUIViewController].
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

    // The overlay is debug-only: it must not crash the host app for missing CADisableMinimumFrameDurationOnPhone in Info.plist.
    val controller = ComposeUIViewController(
        configure = {
            enforceStrictPlistSanityCheck = false
            opaque = false
        },
    ) {
        // The background dimming and animations are DebugOverlay's own.
        DebugOverlay(viewModel = remember { DebugViewModel() }, onDismiss = { Bugsy.hide() })
    }
    controller.modalPresentationStyle = UIModalPresentationOverFullScreen
    controller.view.backgroundColor = UIColor.clearColor
    presentedController = controller
    root.presentViewController(controller, animated = false, completion = null)
}

private fun dismissOverlay() {
    presentedController?.dismissViewControllerAnimated(false, completion = null)
    presentedController = null
}

private fun currentRootViewController(): UIViewController? =
    UIApplication.sharedApplication.keyWindow?.rootViewController
