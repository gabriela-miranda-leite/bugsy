package com.bugsy.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.roboto_bold
import bugsy.ui.generated.resources.roboto_medium
import bugsy.ui.generated.resources.roboto_mono_medium
import bugsy.ui.generated.resources.roboto_mono_regular
import bugsy.ui.generated.resources.roboto_regular
import org.jetbrains.compose.resources.Font

/** Static weight instances generated from the upstream Roboto / Roboto Mono variable fonts (see ui/licenses). */
@Composable
fun robotoFontFamily(): FontFamily = FontFamily(
    Font(Res.font.roboto_regular, FontWeight.Normal),
    Font(Res.font.roboto_medium, FontWeight.Medium),
    Font(Res.font.roboto_bold, FontWeight.Bold),
)

@Composable
fun robotoMonoFontFamily(): FontFamily = FontFamily(
    Font(Res.font.roboto_mono_regular, FontWeight.Normal),
    Font(Res.font.roboto_mono_medium, FontWeight.Medium),
)
