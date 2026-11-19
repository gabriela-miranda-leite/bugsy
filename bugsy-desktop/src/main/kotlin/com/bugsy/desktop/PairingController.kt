package com.bugsy.desktop

import com.bugsy.desktop.pairing.DesktopPairingServer
import com.bugsy.desktop.pairing.DesktopVerification
import com.bugsy.desktop.pairing.DeviceDiscovery
import com.bugsy.desktop.pairing.DiscoveredDevice
import com.bugsy.desktop.pairing.PAIRING_DESKTOP_UDP_PORT
import com.bugsy.desktop.pairing.PairedDevice
import com.bugsy.desktop.pairing.PairingCode
import com.bugsy.desktop.pairing.PairingCodeManager
import com.bugsy.desktop.pairing.PairingHandler
import com.bugsy.desktop.pairing.desktopDisplayName
import com.bugsy.desktop.pairing.localIpv4Address
import com.bugsy.desktop.pairing.pairingQrText
import com.bugsy.desktop.ui.DesktopToastData
import com.bugsy.desktop.ui.DesktopToastHost
import com.bugsy.desktop.ui.DesktopToastType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val PENDING_REQUEST_TIMEOUT_MS = 60_000L

/**
 * Everything about getting a phone connected (DEB-64): the QR/6-digit-code pairing server, the "Devices on your
 * network" discovery/approve flow, and handing a successful pairing's address/token/fingerprint to [onPaired].
 * Split out of `DesktopViewModel` (DEB-74) so pairing concerns don't live alongside mocks/flags/requests.
 */
internal class PairingController(private val scope: CoroutineScope, private val onPaired: (PairedDevice) -> Unit) {
    private val codeManager = PairingCodeManager()
    private val pairingHandler = PairingHandler(codeManager, desktopName = ::desktopDisplayName, onPaired = onPaired)
    private val pairingServer = DesktopPairingServer(scope, pairingHandler)
    private val discovery = DeviceDiscovery(scope)

    /** `false` when UDP port [PAIRING_DESKTOP_UDP_PORT] is taken (another Bugsy Desktop is open): the QR code and
     * 6-digit code then can't work, but "Manual connection" still does. */
    val isPairingAvailable: Boolean = pairingServer.start()

    val pairingCode: StateFlow<PairingCode> = codeManager.current

    /** The short code to compare with the phone's screen while a pairing waits for the person (DEB-67). */
    val verification: StateFlow<DesktopVerification?> = pairingHandler.verification

    private val _localIp = MutableStateFlow(localIpv4Address())

    /** What the QR code encodes, or `null` when this machine isn't on a network. */
    val pairingQrText: StateFlow<String?> = combine(codeManager.current, _localIp) { code, ip ->
        ip?.let { pairingQrText(it, PAIRING_DESKTOP_UDP_PORT, code.code, code.secret) }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    val devices: StateFlow<List<DiscoveredDevice>> = discovery.devices
    val isScanningDevices: StateFlow<Boolean> = discovery.isScanning

    private val _pendingDeviceRequest = MutableStateFlow<DiscoveredDevice?>(null)

    /** The device that was asked to approve this desktop and hasn't answered yet. */
    val pendingDeviceRequest: StateFlow<DiscoveredDevice?> = _pendingDeviceRequest

    init {
        scope.launch {
            while (true) {
                delay(1_000)
                codeManager.rotateIfExpired()
                pairingHandler.clearExpiredVerification()
            }
        }
    }

    fun refreshDevices() {
        _localIp.value = localIpv4Address()
        discovery.refresh()
    }

    /** "Connect" on a discovered device: the phone can't just hand out its token to anyone on the network who
     * asks, so the user has to approve this desktop in the phone's overlay first. */
    fun connectToDevice(device: DiscoveredDevice) {
        val nonce = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        val publicKey = pairingHandler.expectNonce(nonce, device.host)
        _pendingDeviceRequest.value = device
        scope.launch {
            if (!discovery.requestConnect(device, nonce, desktopDisplayName(), publicKey)) {
                _pendingDeviceRequest.value = null
                DesktopToastHost.show(DesktopToastData(type = DesktopToastType.Error, title = "Could not reach ${device.name}", subtitle = "Check that it is on the same network"))
                return@launch
            }
            delay(PENDING_REQUEST_TIMEOUT_MS)
            if (_pendingDeviceRequest.value == device) _pendingDeviceRequest.value = null
        }
    }

    /** Called once [onPaired] has handled a successful pairing, so the "waiting for approval" row clears. */
    fun clearPendingDeviceRequest() {
        _pendingDeviceRequest.value = null
    }

    fun stop() = pairingServer.stop()
}
