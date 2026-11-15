@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.bugsy.ui

import androidx.compose.runtime.mutableStateOf
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSArray
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.darwin.NSObject

/** Holds the last file picked for import; [com.bugsy.ui.screens.DebugOverlay] reads and clears it via Compose state. */
internal val mockRulesImportState = mutableStateOf<String?>(null)

/** Writes [json] to a temp file and presents the native share sheet with it, mirroring the Android flow. */
internal fun shareMockRulesIos(json: String) {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    val fileName = "bugsy-mocks-${NSUUID().UUIDString()}.json"
    val fileUrl = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
    val data = NSString.create(string = json).dataUsingEncoding(NSUTF8StringEncoding) ?: return
    data.writeToURL(fileUrl, atomically = true)

    val activityController = UIActivityViewController(activityItems = listOf(fileUrl), applicationActivities = null)
    root.presentViewController(activityController, animated = true, completion = null)
}

/** Presents the native document picker restricted to JSON, reading the picked file into [mockRulesImportState]. */
internal fun pickMockRulesFileIos() {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    val picker = UIDocumentPickerViewController(
        documentTypes = listOf("public.json"),
        inMode = UIDocumentPickerMode.UIDocumentPickerModeImport,
    )
    picker.delegate = mockRulesDocumentPickerDelegate
    root.presentViewController(picker, animated = true, completion = null)
}

// A singleton `object` extending NSObject with ObjC protocol conformance fails to link on Kotlin/Native
// ("should have been lowered") — kept as an ordinary class with a single top-level instance instead.
private class MockRulesDocumentPickerDelegate : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
        val didAccess = url.startAccessingSecurityScopedResource()
        try {
            mockRulesImportState.value = NSString.stringWithContentsOfURL(url, encoding = NSUTF8StringEncoding, error = null)
        } finally {
            if (didAccess) url.stopAccessingSecurityScopedResource()
        }
    }
}

private val mockRulesDocumentPickerDelegate = MockRulesDocumentPickerDelegate()
