package com.bugsy.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Live camera preview that reports every QR code it decodes through [onQrCode] (repeatedly, while it stays in
 * view: the caller de-duplicates). Asks for the camera permission itself and calls [onPermissionDenied] if the
 * user refuses. [torchOn] drives the flashlight. Platform-specific: CameraX on Android, AVFoundation on iOS.
 */
@Composable
expect fun BugsyQrCameraPreview(
    modifier: Modifier,
    torchOn: Boolean,
    onQrCode: (String) -> Unit,
    onPermissionDenied: () -> Unit,
)
