package com.bugsy.core.pairing

import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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

    /** The person said the verification codes differ, or never confirmed them: nothing secret was sent. */
    data object NotVerified : PairingResult()
}

/** The short code to compare with the one on the desktop before the token is sent (DEB-67). */
data class PairingVerification(val code: String, val desktopName: String?)

/** A desktop that picked this device from its "Devices on your network" list and is waiting for approval. */
data class ConnectRequest(
    val desktopName: String,
    val desktopHost: String,
    val desktopPort: Int,
    val nonce: String,
    /** The desktop's one-time ECDH public key, so the token is sent sealed (DEB-67). */
    val desktopPk: String? = null,
    /** The short code to compare with the desktop's screen before allowing; `null` for an older desktop with no key. */
    val verificationCode: String? = null,
)

private const val CONNECT_REQUEST_TTL_MS = 60_000L
private const val DISCOVER_ATTEMPTS = 3
private const val RECEIVE_SLICE_MS = 200L
private const val VERIFICATION_TIMEOUT_MS = 45_000L

/**
 * Both halves of the pairing protocol (see [PairingMessage]) for the mobile side:
 *  - the **responder**, always listening on [PAIRING_DEVICE_UDP_PORT] so the desktop can list this device and ask
 *    to connect to it;
 *  - the **pairing client**, which hands the desktop this device's address and session token.
 */
internal class PairingService(
    private val wsPort: Int,
    private val tokenProvider: () -> String?,
    private val fingerprintProvider: () -> String? = { null },
    private val openTransport: (Int?) -> UdpTransport? = ::openUdpTransport,
    private val broadcasts: () -> List<String> = ::broadcastAddresses,
    private val deviceName: () -> String = ::deviceDisplayName,
    private val osName: () -> String = ::osDisplayName,
    private val nowMs: () -> Long = ::monotonicMillis,
    private val discoverWaitMs: Long = 1_000L,
    private val pairWaitMs: Long = 3_000L,
    private val verificationWaitMs: Long = VERIFICATION_TIMEOUT_MS,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var responderJob: Job? = null
    private var expiryJob: Job? = null

    private val _pendingConnectRequest = MutableStateFlow<ConnectRequest?>(null)
    val pendingConnectRequest: StateFlow<ConnectRequest?> = _pendingConnectRequest

    private val _pendingVerification = MutableStateFlow<PairingVerification?>(null)

    /** Set while pairing by code is waiting for the person to compare the short code with the desktop's. */
    val pendingVerification: StateFlow<PairingVerification?> = _pendingVerification

    private var verificationAnswer: CompletableDeferred<Boolean>? = null

    /** The one-time keys of connect requests waiting for the person's answer, by nonce. */
    private val requestSessions = mutableMapOf<String, PeerSession>()

    /** The person compared the codes: [match] is whether they were the same. A `false` sends nothing secret. */
    fun confirmVerification(match: Boolean) {
        verificationAnswer?.complete(match)
    }

    private suspend fun awaitVerification(code: String, desktopName: String?): Boolean {
        val answer = CompletableDeferred<Boolean>()
        verificationAnswer = answer
        _pendingVerification.value = PairingVerification(code, desktopName)
        val matched = withTimeoutOrNull(verificationWaitMs) { answer.await() } ?: false
        _pendingVerification.value = null
        verificationAnswer = null
        return matched
    }

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
        _pendingVerification.value = null
        verificationAnswer?.complete(false)
        requestSessions.clear()
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
                val desktopPkBytes = message.pk?.let(::decodeBase64)
                val session = desktopPkBytes?.let(::openPeerSession)
                val desktopPort = message.port ?: PAIRING_DESKTOP_UDP_PORT
                var verification: String? = null
                if (session != null && desktopPkBytes != null) {
                    // The desktop shows the same code as soon as it gets this, so the person can compare before allowing.
                    verification = verificationCode(desktopPkBytes, session.publicKey, nonce)
                    requestSessions[nonce] = session
                    transport.send(packet.fromHost, desktopPort, PairingMessage(PairingMessage.KEX, nonce = nonce, cpk = session.publicKey.toBase64()).encode())
                }
                val request = ConnectRequest(
                    desktopName = message.name ?: packet.fromHost,
                    desktopHost = packet.fromHost,
                    desktopPort = desktopPort,
                    nonce = nonce,
                    desktopPk = message.pk,
                    verificationCode = verification,
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
        val session = requestSessions.remove(request.nonce)
        if (!allow) return
        val token = tokenProvider() ?: return
        withContext(Dispatchers.Default) {
            val transport = openTransport(null) ?: return@withContext
            try {
                transport.send(request.desktopHost, request.desktopPort, pairMessage(token, nonce = request.nonce, session = session).encode())
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
                        PairingMessage.DESKTOP_HERE -> pairWithCodeAt(transport, packet, code, token, message)
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
            pairAt(transport, qr.host, qr.port, qr.code, token, desktopName = null, secret = qr.secret, session = null)
        } finally {
            transport.close()
        }
    }

    /**
     * The desktop answered the code. When it gave a one-time key, the phone sends its own (`kex`), both screens show
     * the same short code, and only once the person confirms they match does the sealed token go out. An older
     * desktop with no key gets the token the way it always did.
     */
    private suspend fun pairWithCodeAt(transport: UdpTransport, packet: UdpPacket, code: String, token: String, hello: PairingMessage): PairingResult {
        val desktopPkBytes = hello.pk?.let(::decodeBase64)
        val session = desktopPkBytes?.let(::openPeerSession)
        if (session != null && desktopPkBytes != null) {
            transport.send(packet.fromHost, packet.fromPort, PairingMessage(PairingMessage.KEX, code = code, cpk = session.publicKey.toBase64()).encode())
            if (!awaitVerification(verificationCode(desktopPkBytes, session.publicKey, code), hello.name)) return PairingResult.NotVerified
        }
        return pairAt(transport, packet.fromHost, packet.fromPort, code, token, hello.name, secret = null, session = session)
    }

    private fun pairAt(
        transport: UdpTransport,
        host: String,
        port: Int,
        code: String,
        token: String,
        desktopName: String?,
        secret: String?,
        session: PeerSession?,
    ): PairingResult {
        val payload = pairMessage(token, code = code, secret = secret, session = session).encode()
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

    /**
     * What goes in the `pair`. The token and the certificate fingerprint are sealed whenever the platform can:
     *  1. with the QR [secret], the strongest: only the desktop's screen has it;
     *  2. otherwise to the desktop's one-time ECDH key, through [session] (code and list flows), which keeps them
     *     from anyone who only listens. The short verification code the person confirmed before this is what
     *     exposes someone who answers in the desktop's place;
     *  3. otherwise (an iOS phone, or an older desktop) they go in clear, as before.
     */
    private fun pairMessage(token: String, code: String? = null, nonce: String? = null, secret: String? = null, session: PeerSession? = null): PairingMessage {
        val fingerprint = fingerprintProvider()
        val plaintext = pairingJson.encodeToString(PairSecret.serializer(), PairSecret(token, fingerprint)).encodeToByteArray()
        val aad = (code ?: nonce)?.encodeToByteArray()

        val qrKey = secret?.let(::decodeUrlSafeBase64)
        if (qrKey != null && code != null) {
            sealPairingPayload(qrKey, code.encodeToByteArray(), plaintext)?.let { sealed ->
                return PairingMessage(PairingMessage.PAIR, code = code, port = wsPort, name = deviceName(), os = osName(), iv = sealed.iv.toBase64(), enc = sealed.ciphertext.toBase64(), alg = sealed.alg)
            }
        }
        if (session != null && aad != null) {
            session.seal(aad, plaintext)?.let { sealed ->
                return PairingMessage(
                    PairingMessage.PAIR, code = code, nonce = nonce, port = wsPort, name = deviceName(), os = osName(),
                    cpk = sealed.publicKey.toBase64(), iv = sealed.iv.toBase64(), enc = sealed.ciphertext.toBase64(), alg = sealed.alg,
                )
            }
        }
        return PairingMessage(PairingMessage.PAIR, code = code, nonce = nonce, port = wsPort, token = token, name = deviceName(), os = osName(), fp = fingerprint)
    }

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

/** What the sealed `pair` carries. */
@Serializable
private class PairSecret(val token: String, val fp: String? = null)

private val pairingJson = Json { explicitNulls = false }
