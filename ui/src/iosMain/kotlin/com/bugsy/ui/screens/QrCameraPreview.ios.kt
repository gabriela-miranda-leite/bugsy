@file:OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class, BetaInteropApi::class)

package com.bugsy.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ObjCObjectBase
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.hasTorch
import platform.AVFoundation.requestAccessForMediaType
import platform.AVFoundation.torchMode
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

/** Receives decoded QR payloads on the main queue. [AVCaptureMetadataOutput] keeps its delegate weakly, so the caller must hold on to it. */
private class QrMetadataDelegate(private val onQrCode: (String) -> Unit) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
    override fun captureOutput(output: AVCaptureOutput, didOutputMetadataObjects: List<*>, fromConnection: AVCaptureConnection) {
        val code = didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject ?: return
        code.stringValue?.let(onQrCode)
    }
}

/** A [UIView] that keeps its camera layer the size of the view, whatever Compose lays it out as. */
private class CameraPreviewView @ObjCObjectBase.OverrideInit constructor(frame: CValue<CGRect>) : UIView(frame) {
    var previewLayer: AVCaptureVideoPreviewLayer? = null

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.frame = bounds
    }
}

private class CameraHolder {
    var session: AVCaptureSession? = null
    var delegate: QrMetadataDelegate? = null
    var device: AVCaptureDevice? = null
}

@Composable
actual fun BugsyQrCameraPreview(
    modifier: Modifier,
    torchOn: Boolean,
    onQrCode: (String) -> Unit,
    onPermissionDenied: () -> Unit,
) {
    val currentOnQrCode by rememberUpdatedState(onQrCode)
    val currentOnDenied by rememberUpdatedState(onPermissionDenied)
    var granted by remember { mutableStateOf(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized) }
    val holder = remember { CameraHolder() }

    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> granted = true
            AVAuthorizationStatusNotDetermined -> AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { isGranted ->
                dispatch_async(dispatch_get_main_queue()) {
                    granted = isGranted
                    if (!isGranted) currentOnDenied()
                }
            }
            else -> currentOnDenied()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            holder.session?.let { session -> dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) { session.stopRunning() } }
            holder.session = null
        }
    }

    LaunchedEffect(torchOn, holder.device) {
        val device = holder.device ?: return@LaunchedEffect
        if (!device.hasTorch) return@LaunchedEffect
        if (device.lockForConfiguration(null)) {
            device.torchMode = if (torchOn) AVCaptureTorchModeOn else AVCaptureTorchModeOff
            device.unlockForConfiguration()
        }
    }

    if (!granted) return

    UIKitView(
        modifier = modifier,
        factory = {
            val view = CameraPreviewView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply { backgroundColor = UIColor.blackColor }
            val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
            val input = device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, null) }
            if (device != null && input != null) {
                val session = AVCaptureSession()
                val output = AVCaptureMetadataOutput()
                if (session.canAddInput(input) && session.canAddOutput(output)) {
                    session.addInput(input)
                    session.addOutput(output)
                    val delegate = QrMetadataDelegate { text -> currentOnQrCode(text) }
                    output.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
                    output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)

                    val layer = AVCaptureVideoPreviewLayer(session = session).apply { videoGravity = AVLayerVideoGravityResizeAspectFill }
                    view.layer.addSublayer(layer)

                    holder.session = session
                    view.previewLayer = layer
                    holder.delegate = delegate
                    holder.device = device
                    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) { session.startRunning() }
                }
            }
            view
        },
    )
}
