package com.bugsy.desktop.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * A local copy of `:ui`'s `BugsyColors` (DEB-60) — not a dependency on `:ui`, for the same reason
 * `wire/WireProtocol.kt` doesn't depend on `:core`: `:ui` has no desktop target (it depends on `:core`, whose
 * `expect` classes are mobile-only). Values copied 1:1 so the desktop app visually matches the mobile overlay,
 * not approximated.
 */
object BugsyDesktopColors {
    val Background = Color(0xFF09090D)
    val Surface = Color(0xFF0C0C10)
    val SurfaceCard = Color(0xFF121216)
    val SurfaceElevated = Color(0xFF17171D)

    val Border = Color(0xFF27272E)
    val BorderSubtle = Color(0xFF202027)
    val BorderHeader = Color(0xFF25252D)
    val BorderStrong = Color(0xFF303039)

    val TextPrimary = Color(0xFFF6F6F8)
    val TextSecondary = Color(0xFFD0D0D7)
    val TextMuted = Color(0xFFB4B4C0)
    val TextDimmed = Color(0xFFBCBCC8)
    val TextDisabled = Color(0xFF8B8B98)

    val Brand = Color(0xFFFF710B)

    val GetText = Color(0xFF60A5FA)
    val GetBg = Color(0xFF112142)
    val PostText = Color(0xFF42DF89)
    val PostBg = Color(0xFF052012)
    val PutText = Color(0xFFFFBB25)
    val PutBg = Color(0xFF2B1A00)
    val DeleteText = Color(0xFFFF686E)
    val DeleteBg = Color(0xFF2E0809)
    val PatchText = Color(0xFFC084FC)
    val PatchBg = Color(0xFF200A38)
    val HeadText = Color(0xFFA1A1AA)
    val HeadBg = Color(0xFF1C1C22)

    val Status2xx = Color(0xFF3FE58A)
    val Status3xx = Color(0xFFFBBF24)
    val Status4xx = Color(0xFFFF686E)
    val Status5xx = Color(0xFFFF2854)

    val SwitchOn = Color(0xFF3A8A4C)
    val SwitchOff = Color(0xFF47474E)
}

fun methodColors(method: String): Pair<Color, Color> = when (method.uppercase()) {
    "GET" -> BugsyDesktopColors.GetText to BugsyDesktopColors.GetBg
    "POST" -> BugsyDesktopColors.PostText to BugsyDesktopColors.PostBg
    "PUT" -> BugsyDesktopColors.PutText to BugsyDesktopColors.PutBg
    "DELETE" -> BugsyDesktopColors.DeleteText to BugsyDesktopColors.DeleteBg
    "PATCH" -> BugsyDesktopColors.PatchText to BugsyDesktopColors.PatchBg
    else -> BugsyDesktopColors.HeadText to BugsyDesktopColors.HeadBg
}

fun statusColor(code: Int?): Color = when {
    code == null -> BugsyDesktopColors.TextDisabled
    code >= 500 -> BugsyDesktopColors.Status5xx
    code >= 400 -> BugsyDesktopColors.Status4xx
    code >= 300 -> BugsyDesktopColors.Status3xx
    else -> BugsyDesktopColors.Status2xx
}

fun formatDuration(ms: Long): String {
    if (ms < 1_000) return "${ms}ms"
    val wholeSeconds = ms / 1_000
    val tenths = (ms % 1_000) / 100
    return "$wholeSeconds.${tenths}s"
}
