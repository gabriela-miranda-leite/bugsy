package com.bugsy.core.ws

import com.bugsy.core.DebugRepository
import com.bugsy.core.MockEngine
import com.bugsy.core.Bugsy
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

    override val certFingerprint: String = tls.fingerprint

    /**
     * Ktor serves plain HTTP/WebSocket on loopback only, and a [TlsTerminator] on the public [port] is the only way
     * in from the network: the token and the captured requests never cross the Wi-Fi in clear.
     */
    fun start() {
        val backendPort = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1")).use { it.localPort }
        engine = embeddedServer(CIO, host = "127.0.0.1", port = backendPort) {
            bugsyModule(sessionToken, _connectedClients)
        }.start(wait = false)
        terminator = TlsTerminator(tls.keyStore, tls.password, port, backendPort)
    }

    override fun stop() {
        terminator?.close()
        terminator = null
        engine?.stop(gracePeriodMillis = 1000, timeoutMillis = 2000)
        engine = null
    }
}

/** Shared between the real server and its tests ([AndroidBugsyWsServerTest]) via Ktor's `testApplication`. */
internal fun Application.bugsyModule(sessionToken: String, connectedClients: MutableStateFlow<Int> = MutableStateFlow(0)) {
    install(WebSockets)
    routing {
        webSocket("/bugsy") {
            val first = incoming.receiveCatching().getOrNull() as? Frame.Text ?: return@webSocket
            val handshake = runCatching { json.decodeFromString<HandshakeMessage>(first.readText()) }.getOrNull()
            if (handshake?.value != sessionToken) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "invalid token"))
                return@webSocket
            }

            connectedClients.update { it + 1 }
            try {
                // UNDISPATCHED, and started before the snapshot is sent: must subscribe to the merged flows before
                // the client can possibly react to anything (even the snapshot arriving), or a change processed
                // right after could emit its event before this collector exists to see it — SharedFlow's default
                // replay = 0 means that emission would just be lost (DEB-58).
                val job = launch(start = CoroutineStart.UNDISPATCHED) {
                    merge(
                        DebugRepository.added.map { BugsyEvent.RequestRecorded(it) },
                        MockEngine.ruleChanges.map { BugsyEvent.MockRuleChanged(it.rule, it.action.name.lowercase()) },
                        MockEngine.oneShotConsumedEvents.map { BugsyEvent.MockRuleConsumed(it) },
                        Bugsy.flagManager.flagChanges.map { BugsyEvent.FlagChanged(it) },
                    ).collect { event -> send(Frame.Text(json.encodeToString(BugsyEvent.serializer(), event))) }
                }

                send(Frame.Text(json.encodeToString(BugsyEvent.serializer(), currentBugsySnapshot())))

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val command = runCatching {
                            json.decodeFromString(BugsyCommand.serializer(), frame.readText())
                        }.getOrNull()
                        if (command != null) applyBugsyCommand(command)
                    }
                }

                job.cancel()
            } finally {
                connectedClients.update { it - 1 }
            }
        }
    }
}
