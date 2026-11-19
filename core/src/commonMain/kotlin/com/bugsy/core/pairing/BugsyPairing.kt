package com.bugsy.core.pairing

import com.bugsy.core.Bugsy
import com.bugsy.core.ws.BUGSY_WS_DEFAULT_PORT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Public entry point of the desktop pairing flow (DEB-64), used by the overlay's Settings screen. Lives for as long
 * as the BugsyWsServer does: [Bugsy.initialize] starts it and [Bugsy.destroy] stops it.
 */
object BugsyPairing {
    private var service: PairingService? = null

    private val _pendingConnectRequest = MutableStateFlow<ConnectRequest?>(null)

    /** A desktop asked to connect from its "Devices on your network" list; the overlay must ask the user. */
    val pendingConnectRequest: StateFlow<ConnectRequest?> get() = service?.pendingConnectRequest ?: _pendingConnectRequest

    private val _pendingVerification = MutableStateFlow<PairingVerification?>(null)

    /**
     * Set while pairing by code waits for the person to compare a short code with the desktop's screen; the overlay
     * must show it and call [confirmVerification] (DEB-67). Nothing secret is sent before that.
     */
    val pendingVerification: StateFlow<PairingVerification?> get() = service?.pendingVerification ?: _pendingVerification

    /** The person's answer to [pendingVerification]: `true` if the codes on the phone and the desktop are the same. */
    fun confirmVerification(match: Boolean) {
        service?.confirmVerification(match)
    }

    /** `192.168.0.24:7788`, or `null` when the server isn't running or the device isn't on a network. */
    fun deviceAddress(): String? {
        if (Bugsy.wsServerToken == null) return null
        val ip = localIpv4Address() ?: return null
        return "$ip:$BUGSY_WS_DEFAULT_PORT"
    }

    suspend fun pairWithCode(code: String): PairingResult =
        service?.pairWithCode(code) ?: PairingResult.ServerUnavailable

    suspend fun pairWithQr(qrText: String): PairingResult =
        service?.pairWithQr(qrText) ?: PairingResult.ServerUnavailable

    suspend fun respondToConnectRequest(allow: Boolean) {
        service?.respondToConnectRequest(allow)
    }

    internal fun start() {
        stop()
        service = PairingService(wsPort = BUGSY_WS_DEFAULT_PORT, tokenProvider = { Bugsy.wsServerToken }, fingerprintProvider = { Bugsy.wsServerFingerprint }).also { it.startResponder() }
    }

    internal fun stop() {
        service?.stop()
        service = null
    }
}
