package com.bugsy.desktop

import com.bugsy.desktop.export.MockRuleFile
import com.bugsy.desktop.export.buildHar
import com.bugsy.desktop.export.saveHarToFile
import com.bugsy.desktop.ui.DesktopToastAction
import com.bugsy.desktop.ui.DesktopToastData
import com.bugsy.desktop.ui.DesktopToastHost
import com.bugsy.desktop.ui.DesktopToastType
import com.bugsy.desktop.wire.WireDebugFlag
import com.bugsy.desktop.wire.WireDebugRequest
import com.bugsy.desktop.wire.WireDeviceInfo
import com.bugsy.desktop.wire.WireMockRule
import com.bugsy.desktop.wire.displayName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Matches the reference design's quick filter chips (All/Errors/Slow/Mocked) — a different axis from the
 * search field, which filters by URL/method text (DEB-60's own ticket text asked for that one). */
enum class RequestQuickFilter { ALL, ERRORS, SLOW, MOCKED }

/** Mirrors `BugsyConfig.DEFAULT_SLOW_REQUEST_THRESHOLD_MS` on mobile — not sent over the wire, so this is a
 * local approximation, not necessarily what the connected app is actually configured with. */
const val SLOW_REQUEST_THRESHOLD_MS = 2_000L

/**
 * Plain class, not `androidx.lifecycle.ViewModel` — this is a desktop JVM app, no Android lifecycle to hook
 * into. Owns the coroutine scope for the whole window's lifetime; [onCloseWindow] tears it down.
 */
class DesktopViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client = BugsyWsClient(scope)

    val connectionState: StateFlow<ConnectionState> = client.state

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _quickFilter = MutableStateFlow(RequestQuickFilter.ALL)
    val quickFilter: StateFlow<RequestQuickFilter> = _quickFilter

    private val _selectedRequestId = MutableStateFlow<String?>(null)

    private val _selectedTab = MutableStateFlow(DesktopTab.REQUESTS)
    val selectedTab: StateFlow<DesktopTab> = _selectedTab

    fun selectTab(tab: DesktopTab) {
        _selectedTab.value = tab
    }

    val mockRules: StateFlow<List<WireMockRule>> = client.mockRules

    private val _mockPrefill = MutableStateFlow<WireMockRule?>(null)
    private val _mockPrefillSourceLabel = MutableStateFlow<String?>(null)
    val mockPrefillSourceLabel: StateFlow<String?> = _mockPrefillSourceLabel

    private val _selectedMockRuleId = MutableStateFlow<String?>(null)

    private val _isCreatingMockRule = MutableStateFlow(false)

    /** The rule being edited in the form panel: the prefill (if any), else a persisted rule by id, else `null`
     * (list-only, no panel) — unless [_isCreatingMockRule] is set, in which case `null` itself means "blank new
     * rule form", distinguished by [isMockFormOpen]. */
    val editingMockRule: StateFlow<WireMockRule?> =
        combine(mockRules, _selectedMockRuleId, _mockPrefill) { rules, id, prefill ->
            prefill ?: rules.firstOrNull { it.id == id }
        }.stateIn(scope, SharingStarted.Eagerly, null)

    val isMockFormOpen: StateFlow<Boolean> =
        combine(_selectedMockRuleId, _mockPrefill, _isCreatingMockRule) { id, prefill, creating ->
            id != null || prefill != null || creating
        }.stateIn(scope, SharingStarted.Eagerly, false)

    fun selectMockRule(id: String) {
        _isCreatingMockRule.value = false
        _selectedMockRuleId.value = id
    }

    fun startNewMockRule() {
        _selectedMockRuleId.value = null
        _isCreatingMockRule.value = true
    }

    fun closeMockForm() {
        _selectedMockRuleId.value = null
        _isCreatingMockRule.value = false
        consumeMockPrefill()
    }

    /** Mirrors mobile's `addMockRuleWithToast` in `DebugOverlay.kt`: same toast title/subtitle shape, whether
     * this is a new rule or an edit of an existing one. */
    fun addOrUpdateMockRule(rule: WireMockRule) {
        client.sendAddOrUpdateMockRule(rule)
        closeMockForm()
        DesktopToastHost.show(
            DesktopToastData(type = DesktopToastType.Success, title = "Mock rule saved", subtitle = "${rule.method ?: "ANY"} ${rule.displayName} → ${rule.statusCode}"),
        )
    }

    /** Mirrors mobile's `setMockActiveWithToast`: only toasts when disabling, not when (re-)enabling. */
    fun toggleMockRule(id: String, enabled: Boolean) {
        val rule = mockRules.value.firstOrNull { it.id == id }
        client.sendToggleMockRule(id, enabled)
        if (!enabled && rule != null) {
            DesktopToastHost.show(DesktopToastData(type = DesktopToastType.Info, title = "Mock disabled", subtitle = rule.displayName))
        }
    }

    /** Mirrors mobile's `removeMockRuleWithToast`: a Destructive toast with a 5s Undo window that re-sends the
     * exact same rule (same id) as a fresh `add_mock_rule` if clicked. */
    fun deleteMockRule(id: String) {
        val removed = mockRules.value.firstOrNull { it.id == id }
        client.sendDeleteMockRule(id)
        if (_selectedMockRuleId.value == id) closeMockForm()
        if (removed != null) {
            DesktopToastHost.show(
                DesktopToastData(
                    type = DesktopToastType.Destructive,
                    title = "Mock rule deleted",
                    subtitle = "${removed.method ?: "ANY"} ${removed.displayName}",
                    action = DesktopToastAction("Undo") { client.sendAddOrUpdateMockRule(removed) },
                    durationMs = 5_000L,
                ),
            )
        }
    }

    fun exportMockRules() {
        val file = MockRuleFile.exportToFile(mockRules.value) ?: return
        DesktopToastHost.show(DesktopToastData(type = DesktopToastType.Success, title = "Mock rules exported", subtitle = file.name))
    }

    /** Always merges — upserts each imported rule as its own `add_mock_rule` command. Unlike mobile, there's no
     * merge/replace choice dialog here. Toasts either way, so a failed/empty import says so instead of silently
     * doing nothing. */
    fun importMockRules() {
        when (val result = MockRuleFile.importFromFile()) {
            is MockRuleFile.ImportResult.Success -> {
                result.rules.forEach { client.sendAddOrUpdateMockRule(it) }
                val subtitle = when (result.rules.size) {
                    0 -> "The file had no rules to import"
                    1 -> "Imported 1 rule"
                    else -> "Imported ${result.rules.size} rules"
                }
                DesktopToastHost.show(DesktopToastData(type = DesktopToastType.Success, title = "Mock rules imported", subtitle = subtitle))
            }
            is MockRuleFile.ImportResult.Invalid -> DesktopToastHost.show(
                DesktopToastData(type = DesktopToastType.Error, title = "Import failed", subtitle = result.reason),
            )
            MockRuleFile.ImportResult.Cancelled -> Unit
        }
    }

    /** Mirrors mobile's `DebugRequest.toMockRulePrefill()`: an exact-match pattern for the request's path, its
     * method and status, pre-filled as a one-shot rule — then switches to the Mocks tab to show it. */
    fun createMockFromRequest(request: WireDebugRequest) {
        _mockPrefill.value = WireMockRule(
            id = "mock-${Random.nextLong()}",
            urlPattern = "^${request.url.pathOnly().escapeRegexLiterally()}$",
            method = request.method,
            statusCode = request.statusCode ?: 200,
            responseHeaders = request.responseHeaders.ifEmpty { mapOf("Content-Type" to "application/json") },
            responseBody = request.responseBody.orEmpty(),
            isOneShot = true,
        )
        _mockPrefillSourceLabel.value = "Pre-filled from ${request.method} ${request.url.pathOnly()} · ${request.statusCode ?: "—"}"
        _selectedTab.value = DesktopTab.MOCKS
    }

    private fun consumeMockPrefill() {
        _mockPrefill.value = null
        _mockPrefillSourceLabel.value = null
    }

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

    val deviceInfo: StateFlow<WireDeviceInfo?> = client.deviceInfo

    /** Set the moment [connectionState] first becomes [ConnectionState.CONNECTED] after a [connect] call, `null`
     * once disconnected — a purely local fact (mobile doesn't report a connection time), shown in the Device tab. */
    private val _connectedAtEpochMs = MutableStateFlow<Long?>(null)
    val connectedAtEpochMs: StateFlow<Long?> = _connectedAtEpochMs

    init {
        scope.launch {
            var wasConnected = false
            connectionState.collect { state ->
                if (state == ConnectionState.CONNECTED && !wasConnected) _connectedAtEpochMs.value = System.currentTimeMillis()
                if (state == ConnectionState.DISCONNECTED) _connectedAtEpochMs.value = null
                wasConnected = state == ConnectionState.CONNECTED
            }
        }
        // Mirrors mobile's own toast trigger off `MockEngine.oneShotConsumedEvents` (DebugOverlay.kt).
        scope.launch {
            client.mockRuleConsumed.collect { rule ->
                DesktopToastHost.show(
                    DesktopToastData(type = DesktopToastType.Warning, title = "Mock used up (ONCE)", subtitle = "${rule.displayName} · ${rule.statusCode}"),
                )
            }
        }
    }

    /** Unfiltered — HAR export uses this, same as mobile's `onExportHar(requests)` (the raw list, not what's
     * currently filtered on screen). */
    val allRequests: StateFlow<List<WireDebugRequest>> = client.requests

    val requests: StateFlow<List<WireDebugRequest>> = combine(client.requests, _searchQuery, _quickFilter) { all, query, filter ->
        all.filter { request ->
            matchesQuickFilter(request, filter) &&
                (query.isBlank() || request.url.contains(query, ignoreCase = true) || request.method.contains(query, ignoreCase = true))
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val selectedRequest: StateFlow<WireDebugRequest?> =
        combine(client.requests, _selectedRequestId) { all, id -> all.firstOrNull { it.id == id } }
            .stateIn(scope, SharingStarted.Eagerly, null)

    private fun matchesQuickFilter(request: WireDebugRequest, filter: RequestQuickFilter): Boolean = when (filter) {
        RequestQuickFilter.ALL -> true
        RequestQuickFilter.ERRORS -> (request.statusCode ?: 0) >= 400
        RequestQuickFilter.SLOW -> request.durationMs >= SLOW_REQUEST_THRESHOLD_MS
        RequestQuickFilter.MOCKED -> request.isMocked
    }

    fun connect(host: String, port: Int, token: String) = client.connect(host, port, token)

    fun disconnect() {
        client.disconnect()
        _selectedRequestId.value = null
    }

    fun selectRequest(id: String?) {
        _selectedRequestId.value = id
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setQuickFilter(filter: RequestQuickFilter) {
        _quickFilter.value = filter
    }

    fun clearRequests() = client.clearLocal()

    fun exportHar() {
        val file = saveHarToFile(buildHar(allRequests.value)) ?: return
        DesktopToastHost.show(DesktopToastData(type = DesktopToastType.Success, title = "HAR exported", subtitle = "${file.name} · ${allRequests.value.size} requests"))
    }

    fun onCloseWindow() {
        client.close()
        scope.cancel()
    }
}

/** Mirrors `:ui`'s `String.pathOnly()`: "https://api.bugsy.dev/v1/users/42?x=1" -> "/v1/users/42". */
private fun String.pathOnly(): String {
    val afterScheme = substringAfter("://", missingDelimiterValue = this)
    val withoutHost = afterScheme.substringAfter('/', missingDelimiterValue = "").let { if (it.isEmpty()) "/" else "/$it" }
    return withoutHost.substringBefore('?')
}

/** Mirrors `:ui`'s `String.escapeRegexLiterally()`: backslash-escapes regex metacharacters one by one, so the
 * generated pattern reads as the plain route with light escaping, not `Regex.escape`'s unreadable `\Q...\E`. */
private fun String.escapeRegexLiterally(): String = buildString {
    for (c in this@escapeRegexLiterally) {
        if (c in "\\^$.|?*+()[]{}") append('\\')
        append(c)
    }
}
