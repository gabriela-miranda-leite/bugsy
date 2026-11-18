package com.bugsy.desktop

/** The sections in the sidebar nav (DEB-60). No Timeline tab — removed on request, not part of DEB-60's own
 * deliverables list and never had any wire-protocol backing. */
enum class DesktopTab(val label: String) {
    REQUESTS("Requests"),
    MOCKS("Mocks"),
    FLAGS("Flags"),
    DEVICE("Device"),
}
