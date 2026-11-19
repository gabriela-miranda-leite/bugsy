package com.bugsy.desktop

import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.extensions.HashAlgorithm
import io.ktor.network.tls.extensions.SignatureAlgorithm
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import java.net.ServerSocket
import java.net.Socket
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import kotlin.concurrent.thread
import java.security.KeyStore
import java.security.cert.X509Certificate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** The phone serves `wss://` with a self-signed certificate and the desktop pins its fingerprint (DEB-67). */
class BugsyWsClientTlsTest {
    private var engine: EmbeddedServer<*, *>? = null

    @AfterTest
    fun tearDown() {
        relays.forEach { runCatching { it.close() } }
        engine?.stop(gracePeriodMillis = 0, timeoutMillis = 500)
    }

    private class Tls(val keyStore: KeyStore, val fingerprint: String)

    /** [domains] and [ips] are what the certificate names: a phone's certificate does not name the LAN address it is reached at. */
    private fun newTls(domains: List<String>? = listOf(BUGSY_TLS_SERVER_NAME), ips: List<java.net.InetAddress>? = emptyList()): Tls {
        val keyStore = buildKeyStore {
            certificate("bugsy-ws") {
                hash = HashAlgorithm.SHA256
                sign = SignatureAlgorithm.ECDSA
                password = "secret"
                keySizeInBits = 256
                if (domains != null) this.domains = domains
                if (ips != null) this.ipAddresses = ips
            }
        }
        return Tls(keyStore, certificateFingerprint(keyStore.getCertificate("bugsy-ws") as X509Certificate))
    }

    private val relays = mutableListOf<AutoCloseable>()

    /** The phone's shape: a plain Ktor server on loopback with a TLS socket in front of it (Ktor's CIO can't serve HTTPS). */
    private fun startTlsServer(tls: Tls, port: Int) {
        val backendPort = ServerSocket(0).use { it.localPort }
        engine = embeddedServer(CIO, host = "127.0.0.1", port = backendPort) {
            install(WebSockets)
            routing {
                webSocket("/bugsy") {
                    incoming.receive()
                    send(Frame.Text("""{"type":"snapshot","requests":[{"id":"1","url":"https://api.bugsy.dev/v1/users","method":"GET"}],"mockRules":[],"flags":[]}"""))
                    incoming.receive()
                }
            }
        }.start(wait = false)

        val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            .apply { init(tls.keyStore, "secret".toCharArray()) }.keyManagers
        val context = SSLContext.getInstance("TLS").apply { init(keyManagers, null, null) }
        val server = context.serverSocketFactory.createServerSocket(port)
        relays += server
        thread(isDaemon = true) {
            while (!server.isClosed) {
                val client = runCatching { server.accept() }.getOrNull() ?: break
                thread(isDaemon = true) {
                    runCatching {
                        val backend = Socket("127.0.0.1", backendPort)
                        thread(isDaemon = true) { runCatching { client.getInputStream().copyTo(backend.getOutputStream()) }; runCatching { backend.close() } }
                        runCatching { backend.getInputStream().copyTo(client.getOutputStream()) }
                        client.close()
                    }
                }
            }
        }
    }

    private fun withClient(block: suspend (BugsyWsClient) -> Unit) = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val client = BugsyWsClient(scope)
        try {
            block(client)
        } finally {
            client.close()
            scope.cancel()
        }
    }

    @Test
    fun `connects over wss when the certificate matches the pinned fingerprint`() {
        val tls = newTls()
        val port = ServerSocket(0).use { it.localPort }
        startTlsServer(tls, port)

        withClient { client ->
            client.connect("127.0.0.1", port, "tok3n", fingerprint = tls.fingerprint)
            withTimeout(10_000) { client.requests.first { it.isNotEmpty() } }
            assertEquals("1", client.requests.value.single().id)
        }
    }

    @Test
    fun `never connects when the certificate is not the pinned one`() {
        val tls = newTls()
        val port = ServerSocket(0).use { it.localPort }
        startTlsServer(tls, port)

        withClient { client ->
            client.connect("127.0.0.1", port, "tok3n", fingerprint = newTls().fingerprint)
            delay(3_000)
            assertNotEquals(ConnectionState.CONNECTED, client.state.value)
            assertEquals(emptyList(), client.requests.value)
        }
    }

    @Test
    fun `without a pairing it learns the certificate and still connects over wss`() {
        val tls = newTls()
        val port = ServerSocket(0).use { it.localPort }
        startTlsServer(tls, port)

        withClient { client ->
            client.connect("127.0.0.1", port, "tok3n")
            withTimeout(10_000) { client.requests.first { it.isNotEmpty() } }
            assertEquals(ConnectionState.CONNECTED, client.state.value)
        }
    }

    @Test
    fun `the pinning trust manager compares fingerprints ignoring case`() {
        val tls = newTls()
        val cert = tls.keyStore.getCertificate("bugsy-ws") as X509Certificate

        PinnedTrustManager(tls.fingerprint.uppercase()).checkServerTrusted(arrayOf(cert), "ECDHE_ECDSA")
    }

    @Test
    fun `connects over wss when the certificate does not name the address it is reached at`() {
        // The phone is reached at its LAN address (192.168.x.y), which no certificate it makes can name.
        val tls = newTls()
        val port = ServerSocket(0).use { it.localPort }
        startTlsServer(tls, port)

        withClient { client ->
            client.connect("127.0.0.1", port, "tok3n", fingerprint = tls.fingerprint)
            withTimeout(10_000) { client.requests.first { it.isNotEmpty() } }
            assertEquals(ConnectionState.CONNECTED, client.state.value)
        }
    }
}

class ConnectionFailureTextTest {
    @Test
    fun `a pinned certificate that does not match says so and is not treated as a network problem`() {
        val text = describeConnectionFailure(java.security.cert.CertificateException("server certificate does not match the pinned fingerprint"))

        assertTrue("not the one it reported" in text, text)
    }

    @Test
    fun `a refused connection says the app on the phone is closed and names the address`() {
        val text = describeConnectionFailure(java.net.ConnectException("Connection refused"), "192.168.0.24:7788")

        assertTrue("192.168.0.24:7788 refused" in text && "app on the phone is closed" in text, text)
    }

    @Test
    fun `a refused connection to localhost suggests forwarding the emulator's port`() {
        val text = describeConnectionFailure(java.net.ConnectException("Connection refused"), "localhost:7788")

        assertTrue("adb forward tcp:7788 tcp:7788" in text, text)
        assertTrue("adb forward" !in describeConnectionFailure(java.net.ConnectException("Connection refused"), "192.168.0.24:7788"))
    }

    @Test
    fun `no route to host points at the Mac's Local Network permission first and the network second`() {
        val text = describeConnectionFailure(
            java.net.ConnectException("Failed to connect to /192.168.0.24:7788").apply { initCause(java.net.NoRouteToHostException("No route to host")) },
            "192.168.0.24:7788",
        )

        assertTrue("No route to 192.168.0.24:7788" in text, text)
        assertTrue("Local Network" in text && "guest or office" in text, text)
    }

    @Test
    fun `a silence names the address and suggests another network or a hotspot`() {
        val text = describeConnectionFailure(java.net.SocketTimeoutException("connect timed out"), "192.168.0.24:7788")

        assertTrue("192.168.0.24:7788 did not answer" in text && "hotspot" in text, text)
    }

    @Test
    fun `without an address it still reads well`() {
        assertTrue(describeConnectionFailure(java.net.ConnectException("Connection refused")).startsWith("the phone refused"))
    }

    @Test
    fun `a failure wrapped in another is still told apart`() {
        val text = describeConnectionFailure(RuntimeException("outer", java.security.cert.CertificateException("does not match the pinned fingerprint")))

        assertTrue("not the one it reported" in text, text)
    }
}
