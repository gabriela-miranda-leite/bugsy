package com.bugsy.core.ws

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** The only way into the BugsyWsServer from the network is TLS, with a certificate whose fingerprint the desktop pins (DEB-67). */
class TlsTerminatorTest {
    private val closeables = mutableListOf<AutoCloseable>()

    @AfterTest
    fun tearDown() {
        closeables.forEach { runCatching { it.close() } }
    }

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    /** A loopback backend that echoes every byte, standing in for the Ktor server. */
    private fun startEchoBackend(): Int {
        val server = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1")).also { closeables += it }
        thread(isDaemon = true) {
            while (!server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: break
                thread(isDaemon = true) { runCatching { socket.getInputStream().copyTo(socket.getOutputStream()) }; runCatching { socket.close() } }
            }
        }
        return server.localPort
    }

    /** Trusts anything and hands back the certificate it saw: the test checks the fingerprint itself, as the desktop does. */
    private fun connectTls(port: Int, seen: MutableList<X509Certificate>): SSLSocket {
        val trust = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                seen += chain!!.first()
            }
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(trust), null) }
        return (context.socketFactory.createSocket("127.0.0.1", port) as SSLSocket).also { closeables += it }
    }

    /** Reads what the echo backend sends back until [marker] shows up. */
    private fun readUntil(socket: SSLSocket, marker: String): String {
        socket.soTimeout = 5_000
        val out = StringBuilder()
        val buffer = ByteArray(1024)
        while (marker !in out) {
            val read = socket.getInputStream().read(buffer)
            if (read < 0) break
            out.append(buffer.decodeToString(0, read))
        }
        return out.toString()
    }

    @Test
    fun `serves the generated certificate and relays bytes to the backend`() {
        val identity = generateWsTlsIdentity()
        val port = freePort()
        closeables += TlsTerminator(identity.keyStore, identity.password, port, startEchoBackend())

        val seen = mutableListOf<X509Certificate>()
        val socket = connectTls(port, seen)
        socket.startHandshake()

        assertEquals(identity.fingerprint, sha256Hex(seen.single().encoded))
        socket.getOutputStream().apply { write("GET /bugsy HTTP/1.1\r\nHost: x\r\n\r\nhello bugsy".encodeToByteArray()); flush() }
        val echoed = readUntil(socket, "hello bugsy")
        assertTrue(echoed.endsWith("\r\n\r\nhello bugsy"), echoed)
    }

    @Test
    fun `uses TLS 1_2 or newer only`() {
        val identity = generateWsTlsIdentity()
        val port = freePort()
        closeables += TlsTerminator(identity.keyStore, identity.password, port, startEchoBackend())

        val socket = connectTls(port, mutableListOf())
        socket.startHandshake()

        assertTrue(socket.session.protocol in setOf("TLSv1.2", "TLSv1.3"), socket.session.protocol)
    }

    @Test
    fun `a plain client cannot reach the backend through the public port`() {
        val identity = generateWsTlsIdentity()
        val port = freePort()
        closeables += TlsTerminator(identity.keyStore, identity.password, port, startEchoBackend())

        Socket("127.0.0.1", port).use { plain ->
            plain.soTimeout = 2_000
            plain.getOutputStream().apply { write("GET /bugsy HTTP/1.1\r\n\r\n".encodeToByteArray()); flush() }
            // A TLS server answers a non-TLS client with an alert or a close, never with the backend's echo.
            val echoed = runCatching { plain.getInputStream().readNBytes(24).decodeToString() }.getOrDefault("")
            assertTrue(!echoed.startsWith("GET /bugsy"), "the plain request reached the backend: $echoed")
        }
    }

    @Test
    fun `each start generates a different certificate`() {
        assertTrue(generateWsTlsIdentity().fingerprint != generateWsTlsIdentity().fingerprint)
    }

    @Test
    fun `stops accepting connections when closed`() {
        val identity = generateWsTlsIdentity()
        val port = freePort()
        val terminator = TlsTerminator(identity.keyStore, identity.password, port, startEchoBackend())
        terminator.close()

        assertFailsWith<java.io.IOException> {
            connectTls(port, mutableListOf()).startHandshake()
        }
    }

    @Test
    fun `the real server is reachable over TLS and exposes the fingerprint it reports`() {
        val port = freePort()
        val server = AndroidBugsyWsServer(port).also { it.start() }
        closeables += AutoCloseable { server.stop() }

        val seen = mutableListOf<X509Certificate>()
        connectTls(port, seen).startHandshake()

        assertEquals(server.certFingerprint, sha256Hex(seen.single().encoded))
    }

    @Test
    fun `adds the internal secret and the peer address, and drops any the client sent`() {
        val identity = generateWsTlsIdentity()
        val port = freePort()
        closeables += TlsTerminator(identity.keyStore, identity.password, port, startEchoBackend(), internalSecret = "real-secret")

        val socket = connectTls(port, mutableListOf())
        socket.startHandshake()
        val request = "GET /bugsy HTTP/1.1\r\nHost: x\r\nx-bugsy-internal: forged\r\nX-Bugsy-Peer: 6.6.6.6\r\n\r\nEND"
        socket.getOutputStream().apply { write(request.encodeToByteArray()); flush() }
        val seen = readUntil(socket, "END")

        assertTrue("X-Bugsy-Internal: real-secret" in seen, seen)
        assertTrue("X-Bugsy-Peer: 127.0.0.1" in seen, seen)
        assertTrue("forged" !in seen && "6.6.6.6" !in seen, seen)
    }

    @Test
    fun `a connection that never sends a request head is closed`() {
        val identity = generateWsTlsIdentity()
        val port = freePort()
        closeables += TlsTerminator(identity.keyStore, identity.password, port, startEchoBackend())

        val socket = connectTls(port, mutableListOf())
        socket.startHandshake()
        socket.getOutputStream().apply { write("x".repeat(20_000).encodeToByteArray()); flush() }
        socket.soTimeout = 5_000

        assertTrue(runCatching { socket.getInputStream().read() }.getOrDefault(-1) == -1)
    }
}

class WsTlsIdentityTest {
    @Test
    fun `the Android certificate names bugsy_local so the desktop's TLS client accepts it at any address`() {
        val certificate = generateWsTlsIdentity().keyStore.getCertificate(WS_TLS_KEY_ALIAS) as X509Certificate

        val names = certificate.subjectAlternativeNames.map { it[1] as String }
        assertEquals(listOf(BUGSY_TLS_SERVER_NAME), names)
    }
}
