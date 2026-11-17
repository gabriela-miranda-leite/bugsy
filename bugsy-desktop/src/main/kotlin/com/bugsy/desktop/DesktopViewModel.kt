package com.bugsy.desktop

import com.bugsy.desktop.export.buildHar
import com.bugsy.desktop.export.saveHarToFile
import com.bugsy.desktop.wire.WireDebugRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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
        saveHarToFile(buildHar(allRequests.value))
    }

    fun onCloseWindow() {
        client.close()
        scope.cancel()
    }
}
