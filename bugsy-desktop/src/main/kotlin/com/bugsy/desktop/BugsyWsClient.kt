package com.bugsy.desktop

import com.bugsy.desktop.wire.WireDebugRequest
import com.bugsy.desktop.wire.WireEvent
import com.bugsy.desktop.wire.encodeHandshake
import com.bugsy.desktop.wire.parseWireEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING }

private const val INITIAL_BACKOFF_MS = 1_000L
private const val MAX_BACKOFF_MS = 30_000L

/** Connects to a mobile app's BugsyWsServer (DEB-58), reconnecting with exponential backoff until [disconnect]. */
class BugsyWsClient(private val scope: CoroutineScope) {
    private val client = HttpClient(CIO) { install(WebSockets) }

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state

    private val _requests = MutableStateFlow<List<WireDebugRequest>>(emptyList())
    val requests: StateFlow<List<WireDebugRequest>> = _requests

    private var job: Job? = null

    fun connect(host: String, port: Int, token: String) {
        disconnect()
        _requests.value = emptyList()
        job = scope.launch {
            var backoffMs = INITIAL_BACKOFF_MS
            var firstAttempt = true
            while (isActive) {
                _state.value = if (firstAttempt) ConnectionState.CONNECTING else ConnectionState.RECONNECTING
                firstAttempt = false
                try {
                    client.webSocket(host = host, port = port, path = "/bugsy") {
                        send(Frame.Text(encodeHandshake(token)))
                        _state.value = ConnectionState.CONNECTED
                        backoffMs = INITIAL_BACKOFF_MS
                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            when (val event = parseWireEvent(frame.readText())) {
                                is WireEvent.Snapshot -> _requests.value = event.requests
                                is WireEvent.RequestRecorded -> _requests.update { listOf(event.request) + it }
                                WireEvent.Unhandled -> Unit
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Connection refused, dropped mid-session, or handshake rejected — fall through to retry.
                }
                if (!isActive) break
                _state.value = ConnectionState.RECONNECTING
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            }
        }
    }

    /** Clears the local view only — same "display vs. source" distinction as [setPaused]. There's no
     * `BugsyCommand.ClearRequests` sent to mobile (the client never sends commands today); mobile's own history
     * keeps whatever it already had, and would resend it in a `snapshot` on the next reconnect. */
    fun clearLocal() {
        _requests.value = emptyList()
    }

    fun disconnect() {
        job?.cancel()
        job = null
        _state.value = ConnectionState.DISCONNECTED
    }

    /**
     * Ktor's CIO `HttpClient.close()` can block waiting for an in-flight connection to actually finish tearing
     * down — [disconnect] only requests cancellation, it doesn't wait for it. Closing on a daemon thread means
     * whoever calls this (the UI thread, on window close) never hangs on it either way.
     */
    fun close() {
        disconnect()
        Thread({ runCatching { client.close() } }, "BugsyWsClient-close").apply { isDaemon = true }.start()
    }
}
