package com.bugsy.desktop.pairing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** `true` = dark module. Square; the quiet zone is added by [QrCodeImage]'s padding, not baked in. */
fun qrMatrix(text: String): List<List<Boolean>> {
    val hints = mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
    return List(matrix.height) { y -> List(matrix.width) { x -> matrix[x, y] } }
}

/** A real, scannable QR code (white background so phone cameras read it in a dark UI). */
@Composable
fun QrCodeImage(text: String, modifier: Modifier = Modifier, cornerRadius: Dp = 8.dp, quietZone: Dp = 10.dp) {
    val modules = remember(text) { qrMatrix(text) }
    Canvas(
        modifier
            .aspectRatio(1f)
            .background(Color.White, RoundedCornerShape(cornerRadius))
            .padding(quietZone),
    ) {
        val count = modules.size
        val cell = Size(size.width / count, size.height / count)
        for (y in 0 until count) {
            for (x in 0 until count) {
                if (modules[y][x]) drawRect(Color.Black, Offset(x * cell.width, y * cell.height), cell)
            }
        }
    }
}
