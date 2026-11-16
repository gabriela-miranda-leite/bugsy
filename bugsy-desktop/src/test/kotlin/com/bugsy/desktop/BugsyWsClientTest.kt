package com.bugsy.desktop

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import java.net.ServerSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A real loopback socket, same spirit as [com.bugsy.core.ws.AndroidBugsyWsServerTest] (DEB-58) — this exercises
 * the actual Ktor CIO client against a real server, not a mock.
 *
 * The fake server is started *outside* any `runBlocking`/coroutine scope, deliberately: `embeddedServer(...)`'s
 * internal per-connection coroutines (including this route's handler, parked on the second `incoming.receive()`
 * below) get parented to whatever coroutine context is active at the call site. Starting it inside a test's
 * `runBlocking` block made that block's own root job wait on this route handler to finish — which only
 * [tearDown] ever does, after the test method returns. Structured-concurrency deadlock.
 */
class BugsyWsClientTest {
    private var engine: EmbeddedServer<*, *>? = null

    @AfterTest
    fun tearDown() {
        engine?.stop(gracePeriodMillis = 0, timeoutMillis = 500)
    }

    @Test
    fun `connects, receives the snapshot and then a live request`() {
        val port = freePort()
        engine = embeddedServer(CIO, port = port) {
            install(WebSockets)
            routing {
                webSocket("/bugsy") {
                    incoming.receive() // handshake frame; this fake server doesn't validate it
                    send(Frame.Text("""{"type":"snapshot","requests":[]}"""))
                    send(
                        Frame.Text(
                            """{"type":"request_recorded","request":{"id":"1","url":"https://api.bugsy.dev/v1/users","method":"GET","statusCode":200,"durationMs":12}}""",
                        ),
                    )
                    incoming.receive() // keep the connection open until the test tears it down
                }
            }
        }.start(wait = false)

        runBlocking {
            val scope = CoroutineScope(Dispatchers.Default)
            val client = BugsyWsClient(scope)
            try {
                client.connect("127.0.0.1", port, "any-token")

                withTimeout(5_000) {
                    client.requests.first { it.isNotEmpty() }
                }

                assertEquals(ConnectionState.CONNECTED, client.state.value)
                assertEquals(1, client.requests.value.size)
                assertEquals("1", client.requests.value.first().id)
            } finally {
                client.close()
                scope.cancel()
            }
        }
    }

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }
}
