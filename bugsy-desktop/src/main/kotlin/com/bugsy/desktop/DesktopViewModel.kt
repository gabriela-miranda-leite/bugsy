package com.bugsy.desktop

import com.bugsy.desktop.pairing.DesktopVerification
import com.bugsy.desktop.pairing.DiscoveredDevice
import com.bugsy.desktop.pairing.PairedDevice
import com.bugsy.desktop.pairing.PairingCode
import com.bugsy.desktop.ui.DesktopToastData
import com.bugsy.desktop.ui.DesktopToastHost
import com.bugsy.desktop.ui.DesktopToastType
import com.bugsy.desktop.wire.WireCustomInfo
import com.bugsy.desktop.wire.WireDebugFlag
import com.bugsy.desktop.wire.WireDebugRequest
import com.bugsy.desktop.wire.WireDeviceInfo
import com.bugsy.desktop.wire.WireMockRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Matches the reference design's quick filter chips (All/Errors/Slow/Mocked) — a different axis from the
 * search field, which filters by URL/method text (DEB-60's own ticket text asked for that one). */
enum class RequestQuickFilter { ALL, ERRORS, SLOW, MOCKED }

/** Mirrors `BugsyConfig.DEFAULT_SLOW_REQUEST_THRESHOLD_MS` on mobile — not sent over the wire, so this is a
 * local approximation, not necessarily what the connected app is actually configured with. */
const val SLOW_REQUEST_THRESHOLD_MS = 2_000L

/** A TLS certificate seen for the first time on a manual connection (DEB-74): [fingerprint] is its SHA-256 hex. */
class PendingFingerprint(val fingerprint: String, internal val decision: CompletableDeferred<Boolean>)

/**
 * Plain class, not `androidx.lifecycle.ViewModel` — this is a desktop JVM app, no Android lifecycle to hook
 * into. Owns the coroutine scope for the whole window's lifetime; [onCloseWindow] tears it down.
 *
 * Delegates each concern to its own controller ([PairingController], [MockRulesController], [FlagsController],
 * [RequestsController]) and re-exposes their state/actions under the same names as before the split (DEB-74),
 * so this is the one place the UI layer talks to; connection lifecycle (connect/disconnect/fingerprint trust)
 * is small enough to stay here directly, next to [client].
 */
class DesktopViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client = BugsyWsClient(scope)

    val connectionState: StateFlow<ConnectionState> = client.state

    /** Why the connection to the phone is failing, while it is. */
    val connectionError: StateFlow<String?> = client.lastError

    // --- Pairing with a phone (DEB-64) -------------------------------------------------------------------

    private val pairingController = PairingController(scope, onPaired = ::onDevicePaired)

    val isPairingAvailable: Boolean get() = pairingController.isPairingAvailable
    val pairingCode: StateFlow<PairingCode> get() = pairingController.pairingCode
    val verification: StateFlow<DesktopVerification?> get() = pairingController.verification
    val pairingQrText: StateFlow<String?> get() = pairingController.pairingQrText
    val devices: StateFlow<List<DiscoveredDevice>> get() = pairingController.devices
    val isScanningDevices: StateFlow<Boolean> get() = pairingController.isScanningDevices
    val pendingDeviceRequest: StateFlow<DiscoveredDevice?> get() = pairingController.pendingDeviceRequest

    fun refreshDevices() = pairingController.refreshDevices()

    fun connectToDevice(device: DiscoveredDevice) = pairingController.connectToDevice(device)

    private fun onDevicePaired(device: PairedDevice) {
        pairingController.clearPendingDeviceRequest()
        client.connect(device.host, device.port, device.token, device.fingerprint)
        DesktopToastHost.show(DesktopToastData(type = DesktopToastType.Success, title = "Paired with ${device.name ?: device.host}", subtitle = listOfNotNull(device.os, device.host).joinToString(" · ")))
    }

    // --- Tabs ---------------------------------------------------------------------------------------------

    private val _selectedTab = MutableStateFlow(DesktopTab.REQUESTS)
    val selectedTab: StateFlow<DesktopTab> = _selectedTab

    fun selectTab(tab: DesktopTab) {
        _selectedTab.value = tab
    }

    // --- Mock rules (DEB-61) -------------------------------------------------------------------------------

    private val mockRulesController = MockRulesController(scope, client, onSwitchToMocksTab = { _selectedTab.value = DesktopTab.MOCKS })

    val mockRules: StateFlow<List<WireMockRule>> get() = mockRulesController.mockRules
    val mockPrefillSourceLabel: StateFlow<String?> get() = mockRulesController.mockPrefillSourceLabel
    val editingMockRule: StateFlow<WireMockRule?> get() = mockRulesController.editingMockRule
    val isMockFormOpen: StateFlow<Boolean> get() = mockRulesController.isMockFormOpen

    fun selectMockRule(id: String) = mockRulesController.selectMockRule(id)
    fun startNewMockRule() = mockRulesController.startNewMockRule()
    fun closeMockForm() = mockRulesController.closeMockForm()
    fun addOrUpdateMockRule(rule: WireMockRule) = mockRulesController.addOrUpdateMockRule(rule)
    fun toggleMockRule(id: String, enabled: Boolean) = mockRulesController.toggleMockRule(id, enabled)
    fun deleteMockRule(id: String) = mockRulesController.deleteMockRule(id)
    fun exportMockRules() = mockRulesController.exportMockRules()
    fun importMockRules() = mockRulesController.importMockRules()
    fun createMockFromRequest(request: WireDebugRequest) = mockRulesController.createMockFromRequest(request)

    // --- Feature flags --------------------------------------------------------------------------------------

    private val flagsController = FlagsController(scope, client)

    val flags: StateFlow<List<WireDebugFlag>> get() = flagsController.flags
    val flagSearchQuery: StateFlow<String> get() = flagsController.flagSearchQuery
    val selectedFlag: StateFlow<WireDebugFlag?> get() = flagsController.selectedFlag

    fun setFlagSearchQuery(query: String) = flagsController.setFlagSearchQuery(query)
    fun selectFlag(key: String?) = flagsController.selectFlag(key)
    fun toggleFlag(key: String, enabled: Boolean) = flagsController.toggleFlag(key, enabled)
    fun resetFlag(key: String) = flagsController.resetFlag(key)
    fun resetAllFlags() = flagsController.resetAllFlags()

    // --- Requests --------------------------------------------------------------------------------------------

    private val requestsController = RequestsController(scope, client)

    /** Unfiltered — HAR export uses this, same as mobile's `onExportHar(requests)` (the raw list, not what's
     * currently filtered on screen). */
    val allRequests: StateFlow<List<WireDebugRequest>> get() = requestsController.allRequests
    val requests: StateFlow<List<WireDebugRequest>> get() = requestsController.requests
    val selectedRequest: StateFlow<WireDebugRequest?> get() = requestsController.selectedRequest
    val searchQuery: StateFlow<String> get() = requestsController.searchQuery
    val quickFilter: StateFlow<RequestQuickFilter> get() = requestsController.quickFilter

    fun selectRequest(id: String?) = requestsController.selectRequest(id)
    fun setSearchQuery(query: String) = requestsController.setSearchQuery(query)
    fun setQuickFilter(filter: RequestQuickFilter) = requestsController.setQuickFilter(filter)
    fun clearRequests() = requestsController.clearRequests()
    fun exportHar() = requestsController.exportHar()

    // --- Connection lifecycle --------------------------------------------------------------------------------

    val deviceInfo: StateFlow<WireDeviceInfo?> = client.deviceInfo
    val customInfo: StateFlow<List<WireCustomInfo>> = client.customInfo

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
    }

    private val _pendingFingerprint = MutableStateFlow<PendingFingerprint?>(null)

    /** A certificate learned trust-on-first-use from a manual connection, awaiting the person's approval. */
    val pendingFingerprint: StateFlow<PendingFingerprint?> = _pendingFingerprint

    fun connect(host: String, port: Int, token: String) {
        client.connect(host, port, token, confirmFingerprint = { fingerprint ->
            val decision = CompletableDeferred<Boolean>()
            _pendingFingerprint.value = PendingFingerprint(fingerprint, decision)
            decision.await()
        })
    }

    /** Answers the [pendingFingerprint] prompt: `true` trusts the certificate and lets the handshake continue. */
    fun resolvePendingFingerprint(trust: Boolean) {
        _pendingFingerprint.value?.decision?.complete(trust)
        _pendingFingerprint.value = null
    }

    fun disconnect() {
        client.disconnect()
        requestsController.clearSelection()
    }

    fun onCloseWindow() {
        pairingController.stop()
        client.close()
        scope.cancel()
    }
}

/** Mirrors `:ui`'s `String.pathOnly()`: "https://api.bugsy.dev/v1/users/42?x=1" -> "/v1/users/42". Internal, not
 * private, so [com.bugsy.desktop.DesktopViewModelParityTest] can check it stays in sync with the `:ui` copy. */
internal fun String.pathOnly(): String {
    val afterScheme = substringAfter("://", missingDelimiterValue = this)
    val withoutHost = afterScheme.substringAfter('/', missingDelimiterValue = "").let { if (it.isEmpty()) "/" else "/$it" }
    return withoutHost.substringBefore('?')
}

/** Mirrors `:ui`'s `String.escapeRegexLiterally()`: backslash-escapes regex metacharacters one by one, so the
 * generated pattern reads as the plain route with light escaping, not `Regex.escape`'s unreadable `\Q...\E`. */
internal fun String.escapeRegexLiterally(): String = buildString {
    for (c in this@escapeRegexLiterally) {
        if (c in "\\^$.|?*+()[]{}") append('\\')
        append(c)
    }
}
