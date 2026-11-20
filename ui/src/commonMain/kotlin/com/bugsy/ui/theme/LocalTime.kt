package com.bugsy.ui.theme

/** [epochMillis] (UTC) as `HH:mm:ss` in the device's own time zone, not UTC. */
expect fun formatLocalHms(epochMillis: Long): String
