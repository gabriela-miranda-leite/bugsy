package com.bugsy.ui.theme

import androidx.compose.ui.graphics.Color

/** All values below are taken 1:1 from the Figma file (G8aLo2h9267KWTcJMSxQrA), not approximated. */
object BugsyColors {
    // Surfaces
    val Background = Color(0xFF09090D) // cURL / JSON code block background
    val Surface = Color(0xFF0C0C10) // Bugsy sheet background (header + content)
    val SurfaceCard = Color(0xFF121216) // metric cards, search bar input
    val SurfaceElevated = Color(0xFF17171D) // dismiss ("X") button circle

    // Borders
    val Border = Color(0xFF27272E) // cards, inputs, code block borders
    val BorderSubtle = Color(0xFF202027) // row / key-value dividers
    val BorderHeader = Color(0xFF25252D) // header-level dividers (tab bar, back-button row)
    val BorderStrong = Color(0xFF303039) // "Clear" button border
    val BorderButton = Color(0xFF34343F) // "Copy" / "Copy all" button border
    val DragHandle = Color(0xFF404047)

    // Text
    val TextPrimary = Color(0xFFF6F6F8)
    val TextSecondary = Color(0xFFD0D0D7) // emphasized values: URLs, header/kv values
    val TextMuted = Color(0xFFB4B4C0) // code block body text (cURL, JSON)
    val TextDimmed = Color(0xFFBCBCC8) // list row URL text
    val TextDisabled = Color(0xFF8B8B98) // labels, section titles, duration, placeholders, inactive tab

    // Brand
    val Brand = Color(0xFFFF710B)

    // Flags
    val SwitchOn       = Color(0xFF3A8A4C)
    val SwitchOff      = Color(0xFF47474E)
    val OverrideBadge  = Color(0xFFA27CF0)

    // HTTP method badges (HeadText/HeadBg unverified — no HEAD/OPTIONS example in Figma)
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

    // Status codes (Status3xx unverified — no 3xx example in Figma)
    val Status2xx = Color(0xFF3FE58A)
    val Status3xx = Color(0xFFFBBF24)
    val Status4xx = Color(0xFFFF686E)
    val Status5xx = Color(0xFFFF2854)
}
