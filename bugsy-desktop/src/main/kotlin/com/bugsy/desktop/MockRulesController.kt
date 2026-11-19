package com.bugsy.desktop

import com.bugsy.desktop.export.MockRuleFile
import com.bugsy.desktop.ui.DesktopToastAction
import com.bugsy.desktop.ui.DesktopToastData
import com.bugsy.desktop.ui.DesktopToastHost
import com.bugsy.desktop.ui.DesktopToastType
import com.bugsy.desktop.wire.WireDebugRequest
import com.bugsy.desktop.wire.WireMockRule
import com.bugsy.desktop.wire.displayName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Mock rule CRUD, the create/edit form's open/prefill state, and the toasts that mirror mobile's own
 * `addMockRuleWithToast`/`setMockActiveWithToast`/`removeMockRuleWithToast` (DebugOverlay.kt). Split out of
 * `DesktopViewModel` (DEB-74).
 *
 * @param onSwitchToMocksTab [createMockFromRequest] pre-fills a rule from a request on another tab; this is how
 * it asks the caller to bring the Mocks tab to the front, without this controller knowing about tabs itself.
 */
internal class MockRulesController(
    scope: CoroutineScope,
    private val client: BugsyWsClient,
    private val onSwitchToMocksTab: () -> Unit,
) {
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

    init {
        // Mirrors mobile's own toast trigger off `MockEngine.oneShotConsumedEvents` (DebugOverlay.kt).
        scope.launch {
            client.mockRuleConsumed.collect { rule ->
                DesktopToastHost.show(
                    DesktopToastData(type = DesktopToastType.Warning, title = "Mock used up (ONCE)", subtitle = "${rule.displayName} · ${rule.statusCode}"),
                )
            }
        }
    }

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
        onSwitchToMocksTab()
    }

    private fun consumeMockPrefill() {
        _mockPrefill.value = null
        _mockPrefillSourceLabel.value = null
    }
}
