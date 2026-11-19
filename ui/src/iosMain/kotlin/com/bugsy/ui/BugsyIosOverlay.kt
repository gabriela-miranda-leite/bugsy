package com.bugsy.ui

import androidx.compose.runtime.remember
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import com.bugsy.core.Bugsy
import com.bugsy.ui.screens.DebugOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIModalPresentationOverFullScreen
import platform.UIKit.UIViewController

private var presentedController: UIViewController? = null
private var overlayJob: Job? = null
private var flagsJob: Job? = null

/**
 * Posted right after the overlay finishes dismissing. The overlay uses [UIModalPresentationOverFullScreen]
 * (non-opaque, so the presenting view controller's view never leaves the hierarchy), and UIKit does not
 * reliably call viewWillAppear/viewDidAppear on it when such a presentation is dismissed — so host apps
 * that need to react to the overlay closing (e.g. to re-read an overridden flag) should observe this
 * notification instead of relying on their own view lifecycle.
 */
const val BUGSY_OVERLAY_DID_DISMISS_NOTIFICATION = "BugsyOverlayDidDismiss"

/**
 * Posted on the main thread every time a feature flag changes, whether the person toggled it in the overlay or Bugsy
 * Desktop did. Swift can't collect a Kotlin `StateFlow`, so a host app that has to react right away (for example to
 * switch a screen when `darkModeEnabled` flips) observes this and reads the flag again with
 * `Bugsy.shared.flagManager.value(key:)`. The `userInfo` carries the flag's `key`. Starts with [startBugsyIosOverlay],
 * so call that after `Bugsy.initialize`.
 */
const val BUGSY_FLAGS_DID_CHANGE_NOTIFICATION = "BugsyFlagsDidChange"

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
    // UNDISPATCHED: the subscription exists before this function returns, so a flag changed right after startup is not
    // missed (a SharedFlow keeps no history for a collector that is not there yet).
    flagsJob = CoroutineScope(Dispatchers.Main).launch(start = CoroutineStart.UNDISPATCHED) {
        Bugsy.flagManager.flagChanges.collect { flag -> postFlagsDidChange(flag.key) }
    }
}

/** Tells the host app that [key] changed. Internal so it can be tested without waiting on the main queue. */
internal fun postFlagsDidChange(key: String) {
    NSNotificationCenter.defaultCenter.postNotificationName(
        BUGSY_FLAGS_DID_CHANGE_NOTIFICATION,
        `object` = null,
        userInfo = mapOf<Any?, Any?>("key" to key),
    )
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
        DebugOverlay(
            viewModel = remember { DebugViewModel() },
            onDismiss = { Bugsy.hide() },
            onExportHar = { requests -> shareHarIos(requests) },
            onExportMockRules = { json -> shareMockRulesIos(json) },
            onPickMockRulesFile = { pickMockRulesFileIos() },
            importedMockRulesFileContent = mockRulesImportState.value,
            onImportedMockRulesFileConsumed = { mockRulesImportState.value = null },
        )
    }
    controller.modalPresentationStyle = UIModalPresentationOverFullScreen
    controller.view.backgroundColor = UIColor.clearColor
    presentedController = controller
    root.presentViewController(controller, animated = false, completion = null)
}

private fun dismissOverlay() {
    presentedController?.dismissViewControllerAnimated(false) {
        NSNotificationCenter.defaultCenter.postNotificationName(BUGSY_OVERLAY_DID_DISMISS_NOTIFICATION, `object` = null)
    }
    presentedController = null
}

private fun currentRootViewController(): UIViewController? =
    UIApplication.sharedApplication.keyWindow?.rootViewController

/**
 * Where share sheets and pickers triggered from inside the overlay (export/import mock rules, export HAR)
 * should present from. The overlay itself is already presented on top of the root view controller, so
 * presenting a second modal from the root directly would be a no-op (UIKit silently drops it when the
 * presenter already has a presented view controller) — this returns the overlay instead whenever it's up.
 */
internal fun bugsyPresentingController(): UIViewController? = presentedController ?: currentRootViewController()
