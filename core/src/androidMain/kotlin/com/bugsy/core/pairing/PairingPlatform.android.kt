package com.bugsy.core.pairing

import android.os.Build
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface

private class JvmUdpTransport(private val socket: DatagramSocket) : UdpTransport {
    override fun send(host: String, port: Int, payload: ByteArray): Boolean = runCatching {
        socket.send(DatagramPacket(payload, payload.size, InetAddress.getByName(host), port))
    }.isSuccess

    override fun receive(timeoutMs: Int): UdpPacket? = runCatching {
        socket.soTimeout = timeoutMs.coerceAtLeast(1)
        val buffer = ByteArray(2048)
        val packet = DatagramPacket(buffer, buffer.size)
        socket.receive(packet)
        UdpPacket(packet.data.copyOf(packet.length), packet.address.hostAddress.orEmpty(), packet.port)
    }.getOrNull()

    override fun close() = socket.close()
}

internal actual fun openUdpTransport(listenPort: Int?): UdpTransport? = runCatching {
    val socket = DatagramSocket(null)
    socket.reuseAddress = true
    socket.broadcast = true
    socket.bind(if (listenPort != null) InetSocketAddress(listenPort) else InetSocketAddress(0))
    JvmUdpTransport(socket)
}.getOrNull()

private fun lanInterfaces(): List<NetworkInterface> =
    runCatching { NetworkInterface.getNetworkInterfaces().toList() }.getOrDefault(emptyList())
        .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }

internal actual fun localIpv4Address(): String? =
    lanInterfaces()
        // Wi-Fi first: on a phone, "wlan0" is the interface the desktop can actually reach.
        .sortedBy { if (it.name.startsWith("wlan")) 0 else 1 }
        .firstNotNullOfOrNull { iface ->
            iface.inetAddresses.toList().filterIsInstance<Inet4Address>().firstOrNull { it.isSiteLocalAddress }?.hostAddress
        }

internal actual fun broadcastAddresses(): List<String> {
    val directed = lanInterfaces().flatMap { iface -> iface.interfaceAddresses.mapNotNull { it.broadcast?.hostAddress } }
    return (listOf("255.255.255.255") + directed).distinct()
}

internal actual fun deviceDisplayName(): String = Build.MODEL ?: "Android"

internal actual fun osDisplayName(): String = "Android ${Build.VERSION.RELEASE}"
