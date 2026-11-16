@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.bugsy.ui

import com.bugsy.core.HarSerializer
import com.bugsy.core.model.DebugRequest
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController

/** Writes the HAR to a temp file and presents the native share sheet with it, mirroring the Android flow. */
internal fun shareHarIos(requests: List<DebugRequest>) {
    if (requests.isEmpty()) return
    val presenter = bugsyPresentingController() ?: return
    val fileName = "bugsy-${NSUUID().UUIDString()}.har"
    val fileUrl = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
    val data = NSString.create(string = HarSerializer.serialize(requests, revealSecrets = true))
        .dataUsingEncoding(NSUTF8StringEncoding) ?: return
    data.writeToURL(fileUrl, atomically = true)

    val activityController = UIActivityViewController(activityItems = listOf(fileUrl), applicationActivities = null)
    presenter.presentViewController(activityController, animated = true, completion = null)
}
