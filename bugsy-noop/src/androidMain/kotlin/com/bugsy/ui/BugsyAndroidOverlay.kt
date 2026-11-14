package com.bugsy.ui

/**
 * No-op: composes nothing. It's a plain function (not @Composable) so this artifact doesn't depend on Compose,
 * but it can still be called from inside any Composable in the app, just like the debug version.
 */
fun BugsyAndroidOverlay() = Unit
