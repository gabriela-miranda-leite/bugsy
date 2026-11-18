package com.bugsy.desktop

import com.bugsy.desktop.wire.WireDebugFlag
import com.bugsy.desktop.wire.WireDebugRequest
import com.bugsy.desktop.wire.WireDeviceInfo
import com.bugsy.desktop.wire.WireEvent
import com.bugsy.desktop.wire.WireMockRule
import com.bugsy.desktop.wire.encodeAddMockRule
import com.bugsy.desktop.wire.encodeDeleteMockRule
import com.bugsy.desktop.wire.encodeHandshake
import com.bugsy.desktop.wire.encodeResetAllFlags
import com.bugsy.desktop.wire.encodeResetFlag
import com.bugsy.desktop.wire.encodeToggleFlag
import com.bugsy.desktop.wire.encodeToggleMockRule
import com.bugsy.desktop.wire.parseWireEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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

    private val _mockRules = MutableStateFlow<List<WireMockRule>>(emptyList())
    val mockRules: StateFlow<List<WireMockRule>> = _mockRules

    private val _flags = MutableStateFlow<List<WireDebugFlag>>(emptyList())
    val flags: StateFlow<List<WireDebugFlag>> = _flags

    private val _deviceInfo = MutableStateFlow<WireDeviceInfo?>(null)
    val deviceInfo: StateFlow<WireDeviceInfo?> = _deviceInfo

    /** A one-shot event stream (not state) — a toast fires once per consumption, it isn't "the current
     * consumed rule" the way [mockRules] is "the current rule list". `replay = 0` so a late subscriber doesn't
     * get a stale one. */
    private val _mockRuleConsumed = MutableSharedFlow<WireMockRule>(extraBufferCapacity = 8)
    val mockRuleConsumed: SharedFlow<WireMockRule> = _mockRuleConsumed

    private var job: Job? = null

    /** Set while the handshake has completed, so [sendAddOrUpdateMockRule]/etc. have somewhere to write; `null`
     * whenever [state] isn't [ConnectionState.CONNECTED]. */
    private var session: DefaultClientWebSocketSession? = null

    fun connect(host: String, port: Int, token: String) {
        disconnect()
        _requests.value = emptyList()
        _mockRules.value = emptyList()
        _flags.value = emptyList()
        _deviceInfo.value = null
        job = scope.launch {
            var backoffMs = INITIAL_BACKOFF_MS
            var firstAttempt = true
            while (isActive) {
                _state.value = if (firstAttempt) ConnectionState.CONNECTING else ConnectionState.RECONNECTING
                firstAttempt = false
                try {
                    client.webSocket(host = host, port = port, path = "/bugsy") {
                        send(Frame.Text(encodeHandshake(token)))
                        session = this
                        _state.value = ConnectionState.CONNECTED
                        backoffMs = INITIAL_BACKOFF_MS
                        try {
                            for (frame in incoming) {
                                if (frame !is Frame.Text) continue
                                when (val event = parseWireEvent(frame.readText())) {
                                    is WireEvent.Snapshot -> {
                                        _requests.value = event.requests
                                        _mockRules.value = event.mockRules
                                        _flags.value = event.flags
                                        _deviceInfo.value = event.deviceInfo
                                    }
                                    is WireEvent.RequestRecorded -> _requests.update { listOf(event.request) + it }
                                    is WireEvent.MockRuleChanged -> _mockRules.update { rules ->
                                        when {
                                            event.action == "deleted" -> rules.filterNot { it.id == event.rule.id }
                                            rules.any { it.id == event.rule.id } -> rules.map { if (it.id == event.rule.id) event.rule else it }
                                            else -> rules + event.rule
                                        }
                                    }
                                    is WireEvent.MockRuleConsumed -> _mockRuleConsumed.tryEmit(event.rule)
                                    is WireEvent.FlagChanged -> _flags.update { flags ->
                                        if (flags.any { it.key == event.flag.key }) {
                                            flags.map { if (it.key == event.flag.key) event.flag else it }
                                        } else {
                                            flags + event.flag
                                        }
                                    }
                                    WireEvent.Unhandled -> Unit
                                }
                            }
                        } finally {
                            session = null
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

    /** Clears the local view only — same "display vs. source" distinction as [clearLocal] below. There's no
     * `BugsyCommand.ClearRequests` sent to mobile here either; mobile's own history keeps whatever it already
     * had, and would resend it in a `snapshot` on the next reconnect. */
    fun clearLocal() {
        _requests.value = emptyList()
    }

    /** No-op (not an error) if not currently connected — the caller's UI already reflects the real rule list
     * from the last snapshot/broadcast either way. */
    fun sendAddOrUpdateMockRule(rule: WireMockRule) {
        scope.launch { runCatching { session?.send(Frame.Text(encodeAddMockRule(rule))) } }
    }

    fun sendToggleMockRule(id: String, enabled: Boolean) {
        scope.launch { runCatching { session?.send(Frame.Text(encodeToggleMockRule(id, enabled))) } }
    }

    fun sendDeleteMockRule(id: String) {
        scope.launch { runCatching { session?.send(Frame.Text(encodeDeleteMockRule(id))) } }
    }

    fun sendToggleFlag(key: String, enabled: Boolean) {
        scope.launch { runCatching { session?.send(Frame.Text(encodeToggleFlag(key, enabled))) } }
    }

    fun sendResetFlag(key: String) {
        scope.launch { runCatching { session?.send(Frame.Text(encodeResetFlag(key))) } }
    }

    fun sendResetAllFlags() {
        scope.launch { runCatching { session?.send(Frame.Text(encodeResetAllFlags())) } }
    }

    fun disconnect() {
        job?.cancel()
        job = null
        session = null
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
