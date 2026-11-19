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

/** What the person compares with the phone's screen: [code] is shown until [expiresAtMs]. */
data class DesktopVerification(val code: String, val expiresAtMs: Long, val context: String)

/** A phone that completed pairing and is waiting for the desktop to connect to its BugsyWsServer. */
data class PairedDevice(
    val host: String,
    val port: Int,
    val token: String,
    val name: String?,
    val os: String?,
    /** SHA-256 hex of the phone's TLS certificate to pin, or `null` for a phone without TLS (DEB-67). */
    val fingerprint: String? = null,
)

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
    private class PendingNonce(val host: String, val expiresAtMs: Long, val keys: java.security.KeyPair)

    /** The phone's one-time public key from its `kex`, by pairing code or nonce: a sealed `pair` must use that same key. */
    private val phoneKeys = mutableMapOf<String, String>()

    private val _verification = kotlinx.coroutines.flow.MutableStateFlow<DesktopVerification?>(null)

    /** The short code to compare with the phone's screen while a pairing waits for the person (DEB-67). */
    val verification: kotlinx.coroutines.flow.StateFlow<DesktopVerification?> = _verification

    /** Drops a code nobody confirmed in time. */
    fun clearExpiredVerification() {
        val current = _verification.value ?: return
        if (nowMs() >= current.expiresAtMs) _verification.value = null
    }

    private fun peekNonce(nonce: String, host: String): PendingNonce? = synchronized(lock) {
        pendingNonces[nonce]?.takeIf { it.host == host && nowMs() < it.expiresAtMs }
    }

    private val pendingNonces = mutableMapOf<String, PendingNonce>()

    /**
     * The user picked [host] from the device list: its next `pair` may authenticate with [nonce] instead of the code.
     * Returns the one-time ECDH public key (base64) to put in the `connect_request`, so the phone can seal its token to it.
     */
    fun expectNonce(nonce: String, host: String, ttlMs: Long = NONCE_TTL_MS): String = synchronized(lock) {
        pendingNonces.entries.removeAll { it.value.expiresAtMs <= nowMs() }
        val keys = newEcdhKeyPair()
        pendingNonces[nonce] = PendingNonce(host, nowMs() + ttlMs, keys)
        keys.publicKeyBase64()
    }

    /** Single use: the nonce is gone after this, whether or not it matched. */
    private fun consumeNonce(nonce: String, host: String): PendingNonce? = synchronized(lock) {
        val pending = pendingNonces.remove(nonce) ?: return null
        pending.takeIf { it.host == host && nowMs() < it.expiresAtMs }
    }

    /** The reply to send back to [fromHost], or `null` for datagrams that deserve silence. */
    fun handle(message: PairingMessage, fromHost: String): PairingMessage? = when (message.type) {
        PairingMessage.DISCOVER -> when (val check = codes.check(message.code, fromHost)) {
            CodeCheck.VALID -> PairingMessage(PairingMessage.DESKTOP_HERE, name = desktopName(), pk = codes.current.value.publicKey.ifEmpty { null })
            else -> failure(check)
        }
        PairingMessage.PAIR -> handlePair(message, fromHost)
        PairingMessage.KEX -> handleKex(message, fromHost)
        else -> null
    }

    /**
     * The phone's one-time key, sent before its token. Shows the verification code both sides now derive, so the
     * person can check it matches the phone's screen. Never answers: the phone does not wait on the desktop here.
     */
    private fun handleKex(message: PairingMessage, fromHost: String): PairingMessage? {
        val phoneKey = message.cpk ?: return null
        val phoneKeyBytes = runCatching { java.util.Base64.getDecoder().decode(phoneKey) }.getOrNull() ?: return null
        val context: String
        val ownPublicKey: ByteArray
        if (message.nonce != null) {
            val pending = peekNonce(message.nonce, fromHost) ?: return null
            context = message.nonce
            ownPublicKey = pending.keys.public.encoded
        } else {
            val code = message.code ?: return null
            if (codes.check(code, fromHost) != CodeCheck.VALID) return null
            context = code
            ownPublicKey = codes.ecdhFor(code)?.public?.encoded ?: return null
        }
        synchronized(lock) { phoneKeys[context] = phoneKey }
        _verification.value = DesktopVerification(verificationCode(ownPublicKey, phoneKeyBytes, context), nowMs() + VERIFICATION_TTL_MS, context)
        return null
    }

    private fun handlePair(message: PairingMessage, fromHost: String): PairingMessage {
        val port = message.port?.takeIf { it in 1..65535 }
        if (port == null) return failure(CodeCheck.INVALID)

        val pendingNonce = message.nonce?.let { consumeNonce(it, fromHost) }
        val authorized = when {
            message.nonce != null -> if (pendingNonce != null) CodeCheck.VALID else CodeCheck.INVALID
            else -> codes.check(message.code, fromHost)
        }
        if (authorized != CodeCheck.VALID) return failure(authorized)

        // The secret is read before consume() rotates it. A sealed `pair` (QR flow) is only accepted when it opens
        // with the secret on screen, which a sniffer of the Wi-Fi does not have.
        val secret = message.code?.let(codes::secretFor)
        val token: String
        val fingerprint: String?
        if (message.enc != null) {
            // A sealed-to-key pair must come from the phone that sent the `kex` whose code the person compared.
            val bound = message.cpk == null || synchronized(lock) { phoneKeys[message.nonce ?: message.code] } == message.cpk
            val opened = if (!bound) null else when {
                // QR flow: sealed with the secret only the screen shows.
                message.cpk == null && secret != null && message.code != null && message.iv != null ->
                    openPairSecret(secret, message.code, message.iv, message.enc, message.alg)
                // List flow: sealed to the one-time key this request carried.
                message.cpk != null && pendingNonce != null && message.nonce != null && message.iv != null ->
                    openPeerSealed(pendingNonce.keys, message.cpk, message.nonce, message.iv, message.enc, message.alg)
                // Code flow: sealed to the one-time key `desktop_here` carried.
                message.cpk != null && message.code != null && message.iv != null ->
                    codes.ecdhFor(message.code)?.let { openPeerSealed(it, message.cpk, message.code, message.iv, message.enc, message.alg) }
                else -> null
            }
            if (opened == null) return failure(CodeCheck.INVALID)
            token = opened.token
            fingerprint = opened.fingerprint
        } else {
            token = message.token?.takeIf { it.isNotBlank() } ?: return failure(CodeCheck.INVALID)
            fingerprint = message.fp?.takeIf { it.isNotBlank() }
        }

        if (message.nonce == null) codes.consume()
        synchronized(lock) { phoneKeys.remove(message.nonce ?: message.code) }
        _verification.value = null
        onPaired(PairedDevice(fromHost, port, token, message.name, message.os, fingerprint))
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
        const val VERIFICATION_TTL_MS = 60_000L
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
