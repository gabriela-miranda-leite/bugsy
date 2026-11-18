package com.bugsy.core.pairing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.TimeSource

/** Outcome of an attempt to pair this device with Bugsy Desktop (DEB-64). */
sealed class PairingResult {
    data class Success(val desktopName: String?) : PairingResult()

    /** Nobody on the network answered: wrong network, desktop closed, or a firewall blocking UDP. */
    data object DesktopNotFound : PairingResult()
    data object InvalidCode : PairingResult()
    data object ExpiredCode : PairingResult()

    /** Too many wrong attempts against the current code; the desktop has rotated it. */
    data object TooManyAttempts : PairingResult()
    data object InvalidQr : PairingResult()
    data object NoNetwork : PairingResult()

    /** The BugsyWsServer isn't running (not initialized, or no server on this platform). */
    data object ServerUnavailable : PairingResult()
}

/** A desktop that picked this device from its "Devices on your network" list and is waiting for approval. */
data class ConnectRequest(val desktopName: String, val desktopHost: String, val desktopPort: Int, val nonce: String)

private const val CONNECT_REQUEST_TTL_MS = 60_000L
private const val DISCOVER_ATTEMPTS = 3
private const val RECEIVE_SLICE_MS = 200L

/**
 * Both halves of the pairing protocol (see [PairingMessage]) for the mobile side:
 *  - the **responder**, always listening on [PAIRING_DEVICE_UDP_PORT] so the desktop can list this device and ask
 *    to connect to it;
 *  - the **pairing client**, which hands the desktop this device's address and session token.
 */
internal class PairingService(
    private val wsPort: Int,
    private val tokenProvider: () -> String?,
    private val openTransport: (Int?) -> UdpTransport? = ::openUdpTransport,
    private val broadcasts: () -> List<String> = ::broadcastAddresses,
    private val deviceName: () -> String = ::deviceDisplayName,
    private val osName: () -> String = ::osDisplayName,
    private val nowMs: () -> Long = ::monotonicMillis,
    private val discoverWaitMs: Long = 1_000L,
    private val pairWaitMs: Long = 3_000L,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var responderJob: Job? = null
    private var expiryJob: Job? = null

    private val _pendingConnectRequest = MutableStateFlow<ConnectRequest?>(null)
    val pendingConnectRequest: StateFlow<ConnectRequest?> = _pendingConnectRequest

    /** Starts answering the desktop's discovery. Idempotent; silently does nothing if the port is taken. */
    fun startResponder() {
        if (responderJob != null) return
        val transport = openTransport(PAIRING_DEVICE_UDP_PORT) ?: return
        responderJob = scope.launch {
            try {
                while (isActive) {
                    val packet = transport.receive((RECEIVE_SLICE_MS * 2).toInt()) ?: continue
                    val message = decodePairingMessage(packet.data) ?: continue
                    handleResponderMessage(transport, packet, message)
                }
            } finally {
                transport.close()
            }
        }
    }

    fun stop() {
        responderJob = null
        expiryJob = null
        _pendingConnectRequest.value = null
        scope.cancel()
    }

    private fun handleResponderMessage(transport: UdpTransport, packet: UdpPacket, message: PairingMessage) {
        when (message.type) {
            PairingMessage.WHO -> {
                val reply = PairingMessage(PairingMessage.DEVICE, name = deviceName(), os = osName(), port = wsPort)
                transport.send(packet.fromHost, packet.fromPort, reply.encode())
            }
            PairingMessage.CONNECT_REQUEST -> {
                val nonce = message.nonce ?: return
                val request = ConnectRequest(
                    desktopName = message.name ?: packet.fromHost,
                    desktopHost = packet.fromHost,
                    desktopPort = message.port ?: PAIRING_DESKTOP_UDP_PORT,
                    nonce = nonce,
                )
                _pendingConnectRequest.value = request
                expiryJob?.cancel()
                expiryJob = scope.launch {
                    delay(CONNECT_REQUEST_TTL_MS)
                    if (_pendingConnectRequest.value == request) _pendingConnectRequest.value = null
                }
            }
        }
    }

    /** Answers the desktop's request (see [pendingConnectRequest]). Only an approval ever sends the token. */
    suspend fun respondToConnectRequest(allow: Boolean) {
        val request = _pendingConnectRequest.value ?: return
        _pendingConnectRequest.value = null
        expiryJob?.cancel()
        if (!allow) return
        val token = tokenProvider() ?: return
        withContext(Dispatchers.Default) {
            val transport = openTransport(null) ?: return@withContext
            try {
                transport.send(request.desktopHost, request.desktopPort, pairMessage(token, nonce = request.nonce).encode())
            } finally {
                transport.close()
            }
        }
    }

    /** Pairs by the 6-digit code shown on the desktop: finds the desktop by broadcast, then hands it the token. */
    suspend fun pairWithCode(code: String): PairingResult = withContext(Dispatchers.Default) {
        if (code.length != PAIRING_CODE_LENGTH || !code.all(Char::isDigit)) return@withContext PairingResult.InvalidCode
        val token = tokenProvider() ?: return@withContext PairingResult.ServerUnavailable
        val transport = openTransport(null) ?: return@withContext PairingResult.NoNetwork
        try {
            val discover = PairingMessage(PairingMessage.DISCOVER, code = code).encode()
            var sentAnything = false
            repeat(DISCOVER_ATTEMPTS) {
                for (address in broadcasts()) sentAnything = transport.send(address, PAIRING_DESKTOP_UDP_PORT, discover) || sentAnything
                if (!sentAnything) return@withContext PairingResult.NoNetwork
                val reply = awaitMessage(transport, discoverWaitMs) {
                    it.second.type == PairingMessage.DESKTOP_HERE || it.second.type == PairingMessage.PAIR_RESULT
                }
                if (reply != null) {
                    val (packet, message) = reply
                    return@withContext when (message.type) {
                        PairingMessage.DESKTOP_HERE -> pairAt(transport, packet.fromHost, packet.fromPort, code, token, message.name)
                        else -> message.toFailure()
                    }
                }
            }
            PairingResult.DesktopNotFound
        } finally {
            transport.close()
        }
    }

    /** Pairs from the text of the desktop's QR code, which already carries the desktop's address. */
    suspend fun pairWithQr(qrText: String): PairingResult = withContext(Dispatchers.Default) {
        val qr = parsePairingQr(qrText) ?: return@withContext PairingResult.InvalidQr
        val token = tokenProvider() ?: return@withContext PairingResult.ServerUnavailable
        val transport = openTransport(null) ?: return@withContext PairingResult.NoNetwork
        try {
            pairAt(transport, qr.host, qr.port, qr.code, token, desktopName = null)
        } finally {
            transport.close()
        }
    }

    private fun pairAt(transport: UdpTransport, host: String, port: Int, code: String, token: String, desktopName: String?): PairingResult {
        val payload = pairMessage(token, code = code).encode()
        // UDP can drop a datagram: one retry halfway through the wait costs nothing, and the desktop treats a
        // repeated `pair` for an already-used code as invalid, which is why it only retries while still silent.
        if (!transport.send(host, port, payload)) return PairingResult.NoNetwork
        val deadline = nowMs() + pairWaitMs
        var resent = false
        while (nowMs() < deadline) {
            val reply = awaitMessage(transport, pairWaitMs / 2) { it.second.type == PairingMessage.PAIR_RESULT }
            if (reply != null) {
                val message = reply.second
                return if (message.ok == true) PairingResult.Success(desktopName ?: message.name) else message.toFailure()
            }
            if (!resent) {
                resent = true
                transport.send(host, port, payload)
            }
        }
        return PairingResult.DesktopNotFound
    }

    private fun pairMessage(token: String, code: String? = null, nonce: String? = null) =
        PairingMessage(PairingMessage.PAIR, code = code, nonce = nonce, port = wsPort, token = token, name = deviceName(), os = osName())

    private fun PairingMessage.toFailure(): PairingResult = when (reason) {
        PairingMessage.REASON_EXPIRED -> PairingResult.ExpiredCode
        PairingMessage.REASON_LOCKED -> PairingResult.TooManyAttempts
        else -> PairingResult.InvalidCode
    }

    private fun awaitMessage(
        transport: UdpTransport,
        timeoutMs: Long,
        accept: (Pair<UdpPacket, PairingMessage>) -> Boolean,
    ): Pair<UdpPacket, PairingMessage>? {
        val deadline = nowMs() + timeoutMs
        while (true) {
            val remaining = deadline - nowMs()
            if (remaining <= 0) return null
            val packet = transport.receive(minOf(remaining, RECEIVE_SLICE_MS).toInt()) ?: continue
            val message = decodePairingMessage(packet.data) ?: continue
            if (accept(packet to message)) return packet to message
        }
    }
}

private val monotonicOrigin = TimeSource.Monotonic.markNow()

private fun monotonicMillis(): Long = monotonicOrigin.elapsedNow().inWholeMilliseconds
