package com.bugsy.desktop.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography tokens for the desktop app (DEB-60). `:ui`'s `BugsyTypography` loads bundled Roboto/Roboto Mono
 * fonts via Compose Resources, which `:bugsy-desktop` (a plain JVM module, not Compose Multiplatform resources)
 * doesn't have set up — using the platform's default sans/monospace families instead, at the same sizes/weights,
 * is the fidelity compromise called out in PR #68; swapping in the real fonts is a follow-up.
 */
object BugsyDesktopTypography {
    val Title = TextStyle(fontFamily = FontFamily.Default, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    val Heading = TextStyle(fontFamily = FontFamily.Default, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    val Body = TextStyle(fontFamily = FontFamily.Default, fontSize = 13.sp)
    val BodySmall = TextStyle(fontFamily = FontFamily.Default, fontSize = 12.sp)
    val Label = TextStyle(fontFamily = FontFamily.Default, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
    val Caption = TextStyle(fontFamily = FontFamily.Default, fontSize = 11.sp)

    val Code = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 20.sp)
    val CodeSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 18.sp)
    val CodeBold = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}
