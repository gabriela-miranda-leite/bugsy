package com.bugsy.desktop

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

    private val _methodFilter = MutableStateFlow<String?>(null)
    val methodFilter: StateFlow<String?> = _methodFilter

    private val _selectedRequestId = MutableStateFlow<String?>(null)

    private val _selectedTab = MutableStateFlow(DesktopTab.REQUESTS)
    val selectedTab: StateFlow<DesktopTab> = _selectedTab

    fun selectTab(tab: DesktopTab) {
        _selectedTab.value = tab
    }

    val requests: StateFlow<List<WireDebugRequest>> = combine(client.requests, _searchQuery, _methodFilter) { all, query, method ->
        all.filter { request ->
            (method == null || request.method.equals(method, ignoreCase = true)) &&
                (query.isBlank() || request.url.contains(query, ignoreCase = true) || request.method.contains(query, ignoreCase = true))
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val selectedRequest: StateFlow<WireDebugRequest?> =
        combine(client.requests, _selectedRequestId) { all, id -> all.firstOrNull { it.id == id } }
            .stateIn(scope, SharingStarted.Eagerly, null)

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

    fun setMethodFilter(method: String?) {
        _methodFilter.value = method
    }

    fun onCloseWindow() {
        client.close()
        scope.cancel()
    }
}
