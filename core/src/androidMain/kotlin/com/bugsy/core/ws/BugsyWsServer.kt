package com.bugsy.core.ws

import com.bugsy.core.DebugRepository
import com.bugsy.core.MockEngine
import com.bugsy.core.Bugsy
import com.bugsy.core.secureRandomBytes
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.InetAddress
import java.net.ServerSocket

private val json = Json { ignoreUnknownKeys = true }

actual fun startBugsyWsServer(port: Int): BugsyWsServerHandle? = AndroidBugsyWsServer(port).also { it.start() }

internal class AndroidBugsyWsServer(private val port: Int) : BugsyWsServerHandle {
    override val sessionToken: String = generateBugsySessionToken()

    private val _connectedClients = MutableStateFlow(0)
    override val connectedClients: StateFlow<Int> = _connectedClients

    private var engine: EmbeddedServer<*, *>? = null
    private var terminator: TlsTerminator? = null
    private val tls = generateWsTlsIdentity()
    private val internalSecret = secureRandomBytes(24).joinToString("") { "%02x".format(it) }
    private val guard = HandshakeGuard()

    override val certFingerprint: String = tls.fingerprint

    /**
     * Ktor serves plain HTTP/WebSocket on loopback only, and a [TlsTerminator] on the public [port] is the only way
     * in from the network: the token and the captured requests never cross the Wi-Fi in clear.
     */
    fun start() {
        val backendPort = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1")).use { it.localPort }
        engine = embeddedServer(CIO, host = "127.0.0.1", port = backendPort) {
            bugsyModule(sessionToken, _connectedClients, internalSecret = internalSecret, guard = guard)
        }.start(wait = false)
        terminator = TlsTerminator(tls.keyStore, tls.password, port, backendPort, internalSecret)
    }

    override fun stop() {
        terminator?.close()
        terminator = null
        engine?.stop(gracePeriodMillis = 1000, timeoutMillis = 2000)
        engine = null
    }
}

/** Shared between the real server and its tests ([AndroidBugsyWsServerTest]) via Ktor's `testApplication`. */
internal fun Application.bugsyModule(
    sessionToken: String,
    connectedClients: MutableStateFlow<Int> = MutableStateFlow(0),
    /** When set, only a request that carries it is served: the TLS terminator is the one way in (see [TlsTerminator]). */
    internalSecret: String? = null,
    guard: HandshakeGuard = HandshakeGuard(),
) {
    install(WebSockets)
    routing {
        webSocket("/bugsy") {
            val headers = call.request.headers
            if (internalSecret != null && !constantTimeEquals(headers[INTERNAL_HEADER].orEmpty(), internalSecret)) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "not allowed"))
                return@webSocket
            }
            val peer = headers[PEER_HEADER] ?: call.request.local.remoteHost
            if (guard.isLocked(peer)) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "too many attempts"))
                return@webSocket
            }
            val first = incoming.receiveCatching().getOrNull() as? Frame.Text ?: return@webSocket
            val handshake = runCatching { json.decodeFromString<HandshakeMessage>(first.readText()) }.getOrNull()
            if (handshake?.value == null || !constantTimeEquals(handshake.value, sessionToken)) {
                guard.recordFailure(peer)
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "invalid token"))
                return@webSocket
            }
            guard.recordSuccess(peer)

            connectedClients.update { it + 1 }
            try {
                // One collector per flow, each UNDISPATCHED and all started before the snapshot is sent: collecting a
                // SharedFlow registers the subscriber before the first suspension, so by the time the client can react
                // to anything (even the snapshot arriving) none of these can miss an event, and SharedFlow's default
                // replay = 0 would otherwise drop it. `merge(...)` is not enough: it starts its collectors in child
                // coroutines that are dispatched, so an event in that gap was lost (DEB-58).
                suspend fun forward(event: BugsyEvent) = send(Frame.Text(json.encodeToString(BugsyEvent.serializer(), event)))
                val forwarders = listOf(
                    launch(start = CoroutineStart.UNDISPATCHED) { DebugRepository.added.collect { forward(BugsyEvent.RequestRecorded(it)) } },
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        MockEngine.ruleChanges.collect { forward(BugsyEvent.MockRuleChanged(it.rule, it.action.name.lowercase())) }
                    },
                    launch(start = CoroutineStart.UNDISPATCHED) { MockEngine.oneShotConsumedEvents.collect { forward(BugsyEvent.MockRuleConsumed(it)) } },
                    launch(start = CoroutineStart.UNDISPATCHED) { Bugsy.flagManager.flagChanges.collect { forward(BugsyEvent.FlagChanged(it)) } },
                )

                send(Frame.Text(json.encodeToString(BugsyEvent.serializer(), currentBugsySnapshot())))

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val command = runCatching {
                            json.decodeFromString(BugsyCommand.serializer(), frame.readText())
                        }.getOrNull()
                        if (command != null) applyBugsyCommand(command)
                    }
                }

                forwarders.forEach { it.cancel() }
            } finally {
                connectedClients.update { it - 1 }
            }
        }
    }
}
