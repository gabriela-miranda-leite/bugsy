package com.bugsy.desktop

/** The sections in the sidebar nav (DEB-60) — only [REQUESTS] has real content today; the others are
 * placeholders until their own wire-protocol work (mock management, flags, device info) lands. */
enum class DesktopTab(val label: String) {
    REQUESTS("Requests"),
    MOCKS("Mocks"),
    TIMELINE("Timeline"),
    FLAGS("Flags"),
    DEVICE("Device"),
}
