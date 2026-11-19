package com.bugsy.core.ws

import java.io.Closeable
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket

/**
 * Puts TLS in front of the BugsyWsServer (DEB-67). Ktor's CIO server can't serve HTTPS, so this accepts TLS
 * connections on the public [port] and relays the decrypted bytes to the CIO server, which listens on loopback only
 * ([backendPort] on 127.0.0.1) and so is never reachable from the Wi-Fi. Pure JDK (`javax.net.ssl`), no extra library.
 */
internal class TlsTerminator(
    keyStore: KeyStore,
    keyPassword: CharArray,
    private val port: Int,
    private val backendPort: Int,
) : Closeable {
    private val serverSocket: SSLServerSocket
    private val open = java.util.Collections.synchronizedSet(mutableSetOf<Socket>())

    @Volatile
    private var closed = false

    init {
        val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            .apply { init(keyStore, keyPassword) }.keyManagers
        val context = SSLContext.getInstance("TLS").apply { init(keyManagers, null, null) }
        serverSocket = (context.serverSocketFactory.createServerSocket(port, 50) as SSLServerSocket).apply {
            reuseAddress = true
            // TLS 1.3 where the OS has it (Android 10+), 1.2 otherwise; never the old ones.
            enabledProtocols = supportedProtocols.filter { it == "TLSv1.3" || it == "TLSv1.2" }.toTypedArray()
        }
        Thread(::acceptLoop, "bugsy-tls-accept").apply { isDaemon = true }.start()
    }

    private fun acceptLoop() {
        while (!closed) {
            val client = try {
                serverSocket.accept()
            } catch (_: IOException) {
                break
            }
            open += client
            Thread({ relay(client) }, "bugsy-tls-conn").apply { isDaemon = true }.start()
        }
    }

    private fun relay(client: Socket) {
        val backend = try {
            Socket(InetAddress.getByName("127.0.0.1"), backendPort)
        } catch (_: IOException) {
            closeQuietly(client)
            return
        }
        open += backend
        // One direction on a helper thread, the other here; either side closing tears both down.
        val toBackend = Thread({ pump(client, backend) }, "bugsy-tls-up").apply { isDaemon = true; start() }
        pump(backend, client)
        toBackend.join(1_000)
    }

    private fun pump(from: Socket, to: Socket) {
        try {
            val buffer = ByteArray(8192)
            val input = from.getInputStream()
            val output = to.getOutputStream()
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                output.flush()
            }
        } catch (_: IOException) {
            // The peer went away mid-stream: nothing to report, just close both sides.
        } finally {
            closeQuietly(from)
            closeQuietly(to)
            open -= from
            open -= to
        }
    }

    override fun close() {
        closed = true
        closeQuietly(serverSocket)
        synchronized(open) { open.toList() }.forEach(::closeQuietly)
        open.clear()
    }

    private fun closeQuietly(closeable: Closeable) {
        try {
            closeable.close()
        } catch (_: IOException) {
        }
    }

    private fun closeQuietly(socket: ServerSocket) {
        try {
            socket.close()
        } catch (_: IOException) {
        }
    }
}
