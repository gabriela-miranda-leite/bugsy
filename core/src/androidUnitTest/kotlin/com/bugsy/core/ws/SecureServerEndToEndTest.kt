package com.bugsy.core.ws

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import java.net.ServerSocket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The real server, reached the way the desktop reaches it: wss through the TLS terminator, with the token as first frame. */
class SecureServerEndToEndTest {
    private val json = Json { ignoreUnknownKeys = true }
    private var server: AndroidBugsyWsServer? = null

    @AfterTest
    fun tearDown() {
        server?.stop()
    }

    private fun start(): Int {
        val port = ServerSocket(0).use { it.localPort }
        server = AndroidBugsyWsServer(port).also { it.start() }
        return port
    }

    private fun pinned(fingerprint: String) = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            check(sha256Hex(chain!!.first().encoded) == fingerprint) { "certificate is not the pinned one" }
        }
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    /** The same shape as the desktop's client: OkHttp, trusting only the pinned fingerprint, no hostname check. */
    private fun client(fingerprint: String): HttpClient {
        val trust = pinned(fingerprint)
        val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(trust), null) }
        return HttpClient(OkHttp) {
            install(WebSockets)
            engine {
                config {
                    sslSocketFactory(context.socketFactory, trust)
                    hostnameVerifier { _, _ -> true }
                }
            }
        }
    }

    @Test
    fun `a pinned client with the right token gets the snapshot over wss`() = runBlocking {
        val port = start()
        val http = client(server!!.certFingerprint)
        try {
            http.webSocket("wss://127.0.0.1:$port/bugsy") {
                send(Frame.Text(json.encodeToString(HandshakeMessage.serializer(), HandshakeMessage(server!!.sessionToken))))
                val frame = withTimeout(10_000) { incoming.receive() } as Frame.Text
                assertIs<BugsyEvent.Snapshot>(json.decodeFromString(BugsyEvent.serializer(), frame.readText()))
            }
        } finally {
            http.close()
        }
        Unit
    }

    /** Connects, sends [token] and returns the reason the server closed with. */
    private suspend fun closeReasonFor(http: HttpClient, port: Int, token: String): String? {
        var reason: String? = null
        http.webSocket("wss://127.0.0.1:$port/bugsy") {
            runCatching { send(Frame.Text(json.encodeToString(HandshakeMessage.serializer(), HandshakeMessage(token)))) }
            reason = runCatching { withTimeout(10_000) { closeReason.await() } }.getOrNull()?.message
        }
        return reason
    }

    @Test
    fun `a wrong token is refused and too many wrong ones lock the address`() = runBlocking {
        val port = start()
        val http = client(server!!.certFingerprint)
        try {
            val reasons = (1..6).map { closeReasonFor(http, port, "WRONGTOKEN12") }
            assertEquals("invalid token", reasons.first())
            assertEquals("too many attempts", reasons.last())
            // Locked means locked even for the right token, until the lock runs out.
            assertEquals("too many attempts", closeReasonFor(http, port, server!!.sessionToken))
        } finally {
            http.close()
        }
        Unit
    }

    @Test
    fun `a client that pins another certificate never gets a connection`() = runBlocking {
        val port = start()
        val http = client("00".repeat(32))
        try {
            val result = runCatching {
                http.webSocket("wss://127.0.0.1:$port/bugsy") { send(Frame.Text("x")) }
            }
            assertTrue(result.isFailure)
        } finally {
            http.close()
        }
        Unit
    }
}
