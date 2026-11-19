package com.bugsy.desktop.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography tokens for the desktop app (DEB-60): the same sizes and weights as `:ui`'s `BugsyTypography`, in the same
 * Roboto and Roboto Mono (see [BugsyDesktopFonts]), so the desktop reads like the mobile overlay (DEB-68).
 */
object BugsyDesktopTypography {
    val Title = TextStyle(fontFamily = BugsyDesktopFonts.Roboto, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    val Heading = TextStyle(fontFamily = BugsyDesktopFonts.Roboto, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    val Body = TextStyle(fontFamily = BugsyDesktopFonts.Roboto, fontSize = 13.sp)
    val BodySmall = TextStyle(fontFamily = BugsyDesktopFonts.Roboto, fontSize = 12.sp)
    val Label = TextStyle(fontFamily = BugsyDesktopFonts.Roboto, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
    val Caption = TextStyle(fontFamily = BugsyDesktopFonts.Roboto, fontSize = 11.sp)

    val Code = TextStyle(fontFamily = BugsyDesktopFonts.RobotoMono, fontSize = 12.sp, lineHeight = 20.sp)
    val CodeSmall = TextStyle(fontFamily = BugsyDesktopFonts.RobotoMono, fontSize = 11.sp, lineHeight = 18.sp)
    val CodeBold = TextStyle(fontFamily = BugsyDesktopFonts.RobotoMono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}
