package com.bugsy.desktop.pairing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException

/** A phone that completed pairing and is waiting for the desktop to connect to its BugsyWsServer. */
data class PairedDevice(val host: String, val port: Int, val token: String, val name: String?, val os: String?)

/**
 * Decides what to answer each datagram. Pure (no sockets), so the whole protocol is unit-testable; see
 * [DesktopPairingServer] for the UDP loop around it.
 */
class PairingHandler(
    private val codes: PairingCodeManager,
    private val desktopName: () -> String,
    private val onPaired: (PairedDevice) -> Unit,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private val lock = Any()
    private val pendingNonces = mutableMapOf<String, Pair<String, Long>>()

    /** The user picked [host] from the device list: its next `pair` may authenticate with [nonce] instead of the code. */
    fun expectNonce(nonce: String, host: String, ttlMs: Long = NONCE_TTL_MS) = synchronized(lock) {
        pendingNonces.entries.removeAll { it.value.second <= nowMs() }
        pendingNonces[nonce] = host to nowMs() + ttlMs
    }

    private fun consumeNonce(nonce: String, host: String): Boolean = synchronized(lock) {
        val (expectedHost, expiresAt) = pendingNonces.remove(nonce) ?: return false
        expectedHost == host && nowMs() < expiresAt
    }

    /** The reply to send back to [fromHost], or `null` for datagrams that deserve silence. */
    fun handle(message: PairingMessage, fromHost: String): PairingMessage? = when (message.type) {
        PairingMessage.DISCOVER -> when (val check = codes.check(message.code)) {
            CodeCheck.VALID -> PairingMessage(PairingMessage.DESKTOP_HERE, name = desktopName())
            else -> failure(check)
        }
        PairingMessage.PAIR -> handlePair(message, fromHost)
        else -> null
    }

    private fun handlePair(message: PairingMessage, fromHost: String): PairingMessage {
        val port = message.port?.takeIf { it in 1..65535 }
        val token = message.token?.takeIf { it.isNotBlank() }
        if (port == null || token == null) return failure(CodeCheck.INVALID)

        val authorized = when {
            message.nonce != null -> if (consumeNonce(message.nonce, fromHost)) CodeCheck.VALID else CodeCheck.INVALID
            else -> codes.check(message.code)
        }
        if (authorized != CodeCheck.VALID) return failure(authorized)

        if (message.nonce == null) codes.consume()
        onPaired(PairedDevice(fromHost, port, token, message.name, message.os))
        return PairingMessage(PairingMessage.PAIR_RESULT, ok = true)
    }

    private fun failure(check: CodeCheck) = PairingMessage(
        PairingMessage.PAIR_RESULT,
        ok = false,
        reason = when (check) {
            CodeCheck.EXPIRED -> PairingMessage.REASON_EXPIRED
            CodeCheck.LOCKED -> PairingMessage.REASON_LOCKED
            else -> PairingMessage.REASON_INVALID
        },
    )

    private companion object {
        const val NONCE_TTL_MS = 60_000L
    }
}

/** Listens on [PAIRING_DESKTOP_UDP_PORT] and feeds every datagram to [handler]. */
class DesktopPairingServer(
    private val scope: CoroutineScope,
    private val handler: PairingHandler,
    private val port: Int = PAIRING_DESKTOP_UDP_PORT,
) {
    private var job: Job? = null
    private var socket: DatagramSocket? = null

    /** `false` when the port couldn't be bound (another Bugsy Desktop is already running). */
    fun start(): Boolean {
        if (job != null) return true
        val listenPort = port // not `port` inside `apply`: DatagramSocket has its own `port` (the *remote* one, -1)
        val bound = runCatching {
            DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(listenPort))
                soTimeout = 500
            }
        }.getOrNull() ?: return false
        socket = bound
        job = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(2048)
            while (isActive && !bound.isClosed) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    bound.receive(packet)
                } catch (_: SocketTimeoutException) {
                    continue
                } catch (_: Exception) {
                    break
                }
                val message = decodePairingMessage(packet.data.copyOf(packet.length)) ?: continue
                val reply = handler.handle(message, packet.address.hostAddress.orEmpty()) ?: continue
                val bytes = reply.encode()
                runCatching { bound.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName(packet.address.hostAddress), packet.port)) }
            }
        }
        return true
    }

    fun stop() {
        job?.cancel()
        job = null
        socket?.close()
        socket = null
    }
}
