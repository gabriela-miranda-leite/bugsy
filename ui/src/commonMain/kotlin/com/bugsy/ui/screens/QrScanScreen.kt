package com.bugsy.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.cd_back
import bugsy.ui.generated.resources.cd_bugsy_icon
import bugsy.ui.generated.resources.cd_flashlight
import bugsy.ui.generated.resources.qr_scan_cant_scan
import bugsy.ui.generated.resources.qr_scan_enter_code
import bugsy.ui.generated.resources.qr_scan_hint
import bugsy.ui.generated.resources.qr_scan_permission_denied
import bugsy.ui.generated.resources.qr_scan_title
import com.bugsy.core.pairing.parsePairingQr
import com.bugsy.ui.components.DragHandle
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private val ViewfinderSize = 240.dp

/**
 * Full-screen "Scan desktop QR" page (DEB-64). Only QR codes that Bugsy Desktop would show are handed to
 * [onQrScanned], once; any other QR in view is ignored so a stray code never ends the scan.
 *
 * @param camera the live preview, replaceable so screenshot tests don't need a camera.
 */
@Composable
fun QrScanScreen(
    onBack: () -> Unit,
    onEnterCodeManually: () -> Unit,
    onQrScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
    camera: @Composable (Modifier, Boolean, (String) -> Unit, () -> Unit) -> Unit = { m, torch, onQr, onDenied ->
        BugsyQrCameraPreview(m, torch, onQr, onDenied)
    },
) {
    var torchOn by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var handled by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().background(BugsyColors.Background)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = BugsySpacing.sm, vertical = BugsySpacing.sm),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(Res.string.cd_back), tint = BugsyColors.TextPrimary)
            }
            Text(
                stringResource(Res.string.qr_scan_title),
                style = BugsyTypography.Title,
                color = BugsyColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { torchOn = !torchOn }, enabled = !permissionDenied) {
                Icon(
                    if (torchOn) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff,
                    contentDescription = stringResource(Res.string.cd_flashlight),
                    tint = if (torchOn) BugsyColors.Brand else BugsyColors.TextPrimary,
                )
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (permissionDenied) {
                Text(
                    stringResource(Res.string.qr_scan_permission_denied),
                    style = BugsyTypography.Body,
                    color = BugsyColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = BugsySpacing.xxl),
                )
            } else {
                camera(
                    Modifier.fillMaxSize(),
                    torchOn,
                    { text ->
                        if (!handled && parsePairingQr(text) != null) {
                            handled = true
                            onQrScanned(text)
                        }
                    },
                    { permissionDenied = true },
                )
                Viewfinder(Modifier.align(Alignment.Center))
                Text(
                    stringResource(Res.string.qr_scan_hint),
                    style = BugsyTypography.Body,
                    color = BugsyColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = BugsySpacing.xxl),
                )
            }
        }

        CantScanSheet(onEnterCodeManually)
    }
}

/** Four orange corner brackets with a scan line sweeping between them, like the reference design. */
@Composable
private fun Viewfinder(modifier: Modifier = Modifier) {
    val sweep by rememberInfiniteTransition(label = "qr-scan-line").animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1_800, easing = LinearEasing), RepeatMode.Reverse),
        label = "qr-scan-line-position",
    )
    Canvas(modifier.size(ViewfinderSize)) {
        val stroke = 4.dp.toPx()
        val arm = 34.dp.toPx()
        val radius = 14.dp.toPx()
        val brand = BugsyColors.Brand
        val s = size
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            // Each bracket: a quarter-circle-ish elbow drawn as two arms meeting at the corner.
            drawLine(brand, Offset(x, y + dy * radius), Offset(x, y + dy * arm), stroke, StrokeCap.Round)
            drawLine(brand, Offset(x + dx * radius, y), Offset(x + dx * arm, y), stroke, StrokeCap.Round)
            drawArc(
                brand,
                startAngle = when {
                    dx > 0 && dy > 0 -> 180f
                    dx < 0 && dy > 0 -> 270f
                    dx < 0 && dy < 0 -> 0f
                    else -> 90f
                },
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(if (dx > 0) x else x - 2 * radius, if (dy > 0) y else y - 2 * radius),
                size = Size(2 * radius, 2 * radius),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        corner(0f, 0f, 1f, 1f)
        corner(s.width, 0f, -1f, 1f)
        corner(s.width, s.height, -1f, -1f)
        corner(0f, s.height, 1f, -1f)

        val y = s.height * sweep
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, brand, Color.Transparent)),
            topLeft = Offset(0f, y - 1.5.dp.toPx()),
            size = Size(s.width, 3.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
    }
}

@Composable
private fun CantScanSheet(onEnterCodeManually: () -> Unit) {
    val shape = RoundedCornerShape(topStart = BugsySpacing.sheetCorner, topEnd = BugsySpacing.sheetCorner)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(BugsyColors.Surface, shape)
            .navigationBarsPadding()
            .padding(horizontal = BugsySpacing.lg),
    ) {
        DragHandle()
        Spacer(Modifier.height(BugsySpacing.md))
        Icon(Icons.Outlined.BugReport, contentDescription = stringResource(Res.string.cd_bugsy_icon), tint = BugsyColors.Brand, modifier = Modifier.size(BugsySpacing.iconSizeLarge + BugsySpacing.md))
        Spacer(Modifier.height(BugsySpacing.sm))
        Text(stringResource(Res.string.qr_scan_cant_scan), style = BugsyTypography.Body, color = BugsyColors.TextPrimary)
        Spacer(Modifier.height(BugsySpacing.md))
        val buttonShape = RoundedCornerShape(BugsySpacing.cornerButtonLarge)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(BugsySpacing.resetAllButtonHeight)
                .background(BugsyColors.SurfaceCard, buttonShape)
                .border(BugsySpacing.strokeBorder, BugsyColors.BorderButton, buttonShape)
                .clickable(onClick = onEnterCodeManually),
        ) {
            Text(stringResource(Res.string.qr_scan_enter_code), style = BugsyTypography.Body, color = BugsyColors.TextPrimary)
        }
        Spacer(Modifier.height(BugsySpacing.lg))
    }
}

@Preview
@Composable
private fun QrScanScreenPreview() {
    BugsyTheme {
        QrScanScreen(onBack = {}, onEnterCodeManually = {}, onQrScanned = {}, camera = { m, _, _, _ -> Box(m.background(Color.DarkGray)) })
    }
}
