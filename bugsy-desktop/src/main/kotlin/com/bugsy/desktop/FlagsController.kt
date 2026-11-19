package com.bugsy.desktop

import com.bugsy.desktop.wire.WireDebugFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Feature-flag search/selection and the toggle/reset commands. Split out of `DesktopViewModel` (DEB-74). */
internal class FlagsController(scope: CoroutineScope, private val client: BugsyWsClient) {
    val flags: StateFlow<List<WireDebugFlag>> = client.flags

    private val _flagSearchQuery = MutableStateFlow("")
    val flagSearchQuery: StateFlow<String> = _flagSearchQuery

    fun setFlagSearchQuery(query: String) {
        _flagSearchQuery.value = query
    }

    private val _selectedFlagKey = MutableStateFlow<String?>(null)

    val selectedFlag: StateFlow<WireDebugFlag?> =
        combine(flags, _selectedFlagKey) { all, key -> all.firstOrNull { it.key == key } }
            .stateIn(scope, SharingStarted.Eagerly, null)

    fun selectFlag(key: String?) {
        _selectedFlagKey.value = key
    }

    /** Boolean-only, same as mobile's own `FlagsScreen` — other types are read-only in the UI even though
     * `FlagManager.override` itself accepts `Any`. */
    fun toggleFlag(key: String, enabled: Boolean) = client.sendToggleFlag(key, enabled)

    fun resetFlag(key: String) = client.sendResetFlag(key)

    fun resetAllFlags() = client.sendResetAllFlags()
}
