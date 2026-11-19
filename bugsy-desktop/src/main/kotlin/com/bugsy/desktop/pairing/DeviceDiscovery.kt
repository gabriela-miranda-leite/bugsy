package com.bugsy.desktop.pairing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException

/** A phone running Bugsy that answered the desktop's broadcast. [port] is its BugsyWsServer port. */
data class DiscoveredDevice(val host: String, val name: String, val os: String?, val port: Int)

private const val SCAN_DURATION_MS = 2_000L
private const val REBROADCAST_AFTER_MS = 700L

/** "Devices on your network": broadcasts `who` and collects the phones that answer. */
class DeviceDiscovery(
    private val scope: CoroutineScope,
    private val devicePort: Int = PAIRING_DEVICE_UDP_PORT,
    private val broadcasts: () -> List<String> = ::broadcastAddresses,
) {
    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredDevice>> = _devices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private var scanJob: Job? = null

    fun refresh() {
        if (scanJob?.isActive == true) return
        scanJob = scope.launch {
            _isScanning.value = true
            try {
                _devices.value = scan()
            } finally {
                _isScanning.value = false
            }
        }
    }

    private suspend fun scan(): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val found = linkedMapOf<String, DiscoveredDevice>()
        val socket = runCatching { DatagramSocket(null).apply { broadcast = true; bind(InetSocketAddress(0)); soTimeout = 100 } }.getOrNull()
            ?: return@withContext emptyList()
        socket.use {
            val who = PairingMessage(PairingMessage.WHO).encode()
            fun broadcastWho() = broadcasts().forEach { address ->
                runCatching { it.send(DatagramPacket(who, who.size, InetAddress.getByName(address), devicePort)) }
            }
            val start = System.currentTimeMillis()
            broadcastWho()
            var rebroadcast = false
            val buffer = ByteArray(2048)
            while (System.currentTimeMillis() - start < SCAN_DURATION_MS) {
                if (!rebroadcast && System.currentTimeMillis() - start > REBROADCAST_AFTER_MS) {
                    rebroadcast = true
                    broadcastWho()
                }
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    it.receive(packet)
                } catch (_: SocketTimeoutException) {
                    continue
                } catch (_: Exception) {
                    break
                }
                val message = decodePairingMessage(packet.data.copyOf(packet.length)) ?: continue
                if (message.type != PairingMessage.DEVICE) continue
                val host = packet.address.hostAddress ?: continue
                found[host] = DiscoveredDevice(host, message.name ?: host, message.os, message.port ?: 7788)
            }
        }
        found.values.toList()
    }

    /** Asks the phone to approve this desktop; it answers with a `pair` carrying [nonce] if the user allows it. */
    suspend fun requestConnect(device: DiscoveredDevice, nonce: String, desktopName: String, publicKey: String? = null): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            DatagramSocket().use { socket ->
                val bytes = PairingMessage(PairingMessage.CONNECT_REQUEST, nonce = nonce, name = desktopName, port = PAIRING_DESKTOP_UDP_PORT, pk = publicKey).encode()
                socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName(device.host), devicePort))
            }
        }.isSuccess
    }
}
