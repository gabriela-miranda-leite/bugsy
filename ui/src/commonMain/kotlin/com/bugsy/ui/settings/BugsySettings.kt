package com.bugsy.ui.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Preferences for whoever uses the overlay. They stay in memory only, live as long as the process does, and
 * never go to disk (same rule as the captured data).
 */
object BugsySettings {
    private val _revealSensitiveByDefault = MutableStateFlow(false)

    /** Whether requests open showing sensitive data (`true`) or masked (`false`, the default). */
    val revealSensitiveByDefault: StateFlow<Boolean> = _revealSensitiveByDefault

    fun setRevealSensitiveByDefault(value: Boolean) {
        _revealSensitiveByDefault.value = value
    }

    internal fun reset() {
        _revealSensitiveByDefault.value = false
    }
}
