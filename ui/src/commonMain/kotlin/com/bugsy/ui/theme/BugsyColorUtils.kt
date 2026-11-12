package com.bugsy.ui.theme

import androidx.compose.ui.graphics.Color

fun methodColors(method: String): Pair<Color, Color> = when (method.uppercase()) {
    "GET" -> BugsyColors.GetText to BugsyColors.GetBg
    "POST" -> BugsyColors.PostText to BugsyColors.PostBg
    "PUT" -> BugsyColors.PutText to BugsyColors.PutBg
    "DELETE" -> BugsyColors.DeleteText to BugsyColors.DeleteBg
    "PATCH" -> BugsyColors.PatchText to BugsyColors.PatchBg
    else -> BugsyColors.HeadText to BugsyColors.HeadBg
}

fun statusColor(code: Int?): Color = when {
    code == null -> BugsyColors.TextDisabled
    code >= 500 -> BugsyColors.Status5xx
    code >= 400 -> BugsyColors.Status4xx
    code >= 300 -> BugsyColors.Status3xx
    else -> BugsyColors.Status2xx
}

fun formatDuration(ms: Long): String =
    if (ms >= 1_000) "${(ms / 1_000.0).formatOneDecimal()}s" else "${ms}ms"

private fun Double.formatOneDecimal(): String {
    val rounded = (this * 10).toLong() / 10.0
    val whole = rounded.toLong()
    val decimal = ((rounded - whole) * 10).toLong().let { if (it < 0) -it else it }
    return "$whole.$decimal"
}
