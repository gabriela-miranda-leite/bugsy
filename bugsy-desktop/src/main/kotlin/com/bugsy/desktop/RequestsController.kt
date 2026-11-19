package com.bugsy.desktop

import com.bugsy.desktop.export.buildHar
import com.bugsy.desktop.export.saveHarToFile
import com.bugsy.desktop.ui.DesktopToastData
import com.bugsy.desktop.ui.DesktopToastHost
import com.bugsy.desktop.ui.DesktopToastType
import com.bugsy.desktop.wire.WireDebugRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The request list's search/quick-filter/selection and HAR export. Split out of `DesktopViewModel` (DEB-74). */
internal class RequestsController(scope: CoroutineScope, private val client: BugsyWsClient) {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _quickFilter = MutableStateFlow(RequestQuickFilter.ALL)
    val quickFilter: StateFlow<RequestQuickFilter> = _quickFilter

    private val _selectedRequestId = MutableStateFlow<String?>(null)

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

    fun selectRequest(id: String?) {
        _selectedRequestId.value = id
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setQuickFilter(filter: RequestQuickFilter) {
        _quickFilter.value = filter
    }

    fun clearSelection() {
        _selectedRequestId.value = null
    }

    fun clearRequests() = client.clearLocal()

    fun exportHar() {
        val file = saveHarToFile(buildHar(allRequests.value)) ?: return
        DesktopToastHost.show(DesktopToastData(type = DesktopToastType.Success, title = "HAR exported", subtitle = "${file.name} · ${allRequests.value.size} requests"))
    }
}
