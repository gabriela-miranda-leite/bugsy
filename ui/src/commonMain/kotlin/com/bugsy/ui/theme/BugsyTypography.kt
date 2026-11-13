package com.bugsy.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object BugsyTypography {
    // Chrome do UI — Roboto
    val Title: TextStyle
        @Composable get() = TextStyle(fontFamily = robotoFontFamily(), fontSize = 17.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.3).sp)
    val Body: TextStyle
        @Composable get() = TextStyle(fontFamily = robotoFontFamily(), fontSize = 13.sp)
    val BodySmall: TextStyle
        @Composable get() = TextStyle(fontFamily = robotoFontFamily(), fontSize = 12.sp)
    val Label: TextStyle
        @Composable get() = TextStyle(fontFamily = robotoFontFamily(), fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
    val Caption: TextStyle
        @Composable get() = TextStyle(fontFamily = robotoFontFamily(), fontSize = 11.sp)

    // Código — URLs, headers, cURL, response body — Roboto Mono
    val Code: TextStyle
        @Composable get() = TextStyle(fontFamily = robotoMonoFontFamily(), fontSize = 12.sp, lineHeight = 20.sp)
    val CodeSmall: TextStyle
        @Composable get() = TextStyle(fontFamily = robotoMonoFontFamily(), fontSize = 11.sp, lineHeight = 18.sp)
}
