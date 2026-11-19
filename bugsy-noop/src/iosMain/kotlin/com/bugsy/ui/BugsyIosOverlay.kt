package com.bugsy.ui

/** Mirrors the debug artifact's constant so host apps can compile against it in both configurations; never posted here, since there's no overlay to dismiss. */
const val BUGSY_OVERLAY_DID_DISMISS_NOTIFICATION = "BugsyOverlayDidDismiss"

/** Mirrors the debug artifact's constant so host apps can compile against it in both configurations; never posted here, since no flag is ever changed from an overlay or a desktop. */
const val BUGSY_FLAGS_DID_CHANGE_NOTIFICATION = "BugsyFlagsDidChange"

/** No-op: doesn't observe or present any overlay. */
fun startBugsyIosOverlay() = Unit
