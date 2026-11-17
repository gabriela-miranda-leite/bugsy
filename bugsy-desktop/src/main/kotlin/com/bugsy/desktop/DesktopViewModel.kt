package com.bugsy.desktop

import com.bugsy.desktop.wire.WireDebugRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow

/**
 * Plain class, not `androidx.lifecycle.ViewModel` — this is a desktop JVM app, no Android lifecycle to hook
 * into. Owns the coroutine scope for the whole window's lifetime; [onCloseWindow] tears it down.
 */
class DesktopViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client = BugsyWsClient(scope)

    val connectionState: StateFlow<ConnectionState> = client.state
    val requests: StateFlow<List<WireDebugRequest>> = client.requests

    fun connect(host: String, port: Int, token: String) = client.connect(host, port, token)

    fun disconnect() = client.disconnect()

    fun onCloseWindow() {
        client.close()
        scope.cancel()
    }
}
