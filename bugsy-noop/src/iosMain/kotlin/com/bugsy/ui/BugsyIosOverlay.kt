package com.bugsy.ui

/** Mirrors the debug artifact's constant so host apps can compile against it in both configurations; never posted here, since there's no overlay to dismiss. */
const val BUGSY_OVERLAY_DID_DISMISS_NOTIFICATION = "BugsyOverlayDidDismiss"

/** No-op: doesn't observe or present any overlay. */
fun startBugsyIosOverlay() = Unit
