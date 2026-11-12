package com.bugsy.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object BugsyTypography {
    // Chrome do UI — sans-serif do sistema
    val Title = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.3).sp)
    val Body = TextStyle(fontSize = 13.sp)
    val BodySmall = TextStyle(fontSize = 12.sp)
    val Label = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
    val Caption = TextStyle(fontSize = 11.sp)

    // Código — URLs, headers, cURL, response body
    val Code = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 20.sp)
    val CodeSmall = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
}
