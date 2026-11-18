@file:OptIn(ExperimentalForeignApi::class)

package com.bugsy.core.pairing

import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.UIKit.UIDevice
import platform.posix.AF_INET
import platform.posix.INADDR_ANY
import platform.posix.SOCK_DGRAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_BROADCAST
import platform.posix.SO_RCVTIMEO
import platform.posix.SO_REUSEADDR
import platform.posix.bind
import platform.posix.close
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.posix.IFF_BROADCAST
import platform.posix.IFF_LOOPBACK
import platform.posix.IFF_UP
import platform.darwin.inet_addr
import platform.darwin.inet_ntoa
import platform.posix.recvfrom
import platform.posix.sendto
import platform.posix.setsockopt
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.posix.timeval
import kotlin.concurrent.Volatile

/** iOS (ARM) is little-endian; network byte order is big-endian — swap the two bytes of the port. */
private fun Int.toNetworkShort(): UShort = (((this and 0xFF) shl 8) or ((this shr 8) and 0xFF)).toUShort()

private fun UShort.fromNetworkShort(): Int = toInt().let { ((it and 0xFF) shl 8) or ((it shr 8) and 0xFF) }

private class PosixUdpTransport(private val fd: Int) : UdpTransport {
    @Volatile private var closed = false

    override fun send(host: String, port: Int, payload: ByteArray): Boolean {
        if (closed) return false
        return memScoped {
            val address = alloc<sockaddr_in>()
            address.sin_family = AF_INET.convert()
            address.sin_port = port.toNetworkShort()
            address.sin_addr.s_addr = inet_addr(host)
            val sent = payload.usePinned { pinned ->
                sendto(fd, pinned.addressOf(0), payload.size.convert(), 0, address.ptr.reinterpret<sockaddr>(), sizeOf<sockaddr_in>().convert())
            }
            sent >= 0
        }
    }

    override fun receive(timeoutMs: Int): UdpPacket? {
        if (closed) return null
        return memScoped {
            val timeout = alloc<timeval>()
            timeout.tv_sec = (timeoutMs / 1000).convert()
            timeout.tv_usec = ((timeoutMs % 1000) * 1000).convert()
            setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, timeout.ptr, sizeOf<timeval>().convert())

            val buffer = ByteArray(2048)
            val sender = alloc<sockaddr_in>()
            val senderLength = alloc<UIntVar>()
            senderLength.value = sizeOf<sockaddr_in>().convert()
            val received = buffer.usePinned { pinned ->
                recvfrom(fd, pinned.addressOf(0), buffer.size.convert(), 0, sender.ptr.reinterpret<sockaddr>(), senderLength.ptr)
            }
            if (received <= 0) {
                null
            } else {
                UdpPacket(
                    buffer.copyOf(received.toInt()),
                    inet_ntoa(sender.sin_addr.readValue())?.toKString().orEmpty(),
                    sender.sin_port.fromNetworkShort(),
                )
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        close(fd)
    }
}

internal actual fun openUdpTransport(listenPort: Int?): UdpTransport? {
    val fd = socket(AF_INET, SOCK_DGRAM, 0)
    if (fd < 0) return null
    val bound = memScoped {
        val enable = alloc<IntVar>()
        enable.value = 1
        setsockopt(fd, SOL_SOCKET, SO_BROADCAST, enable.ptr, sizeOf<IntVar>().convert())
        setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, enable.ptr, sizeOf<IntVar>().convert())

        val address = alloc<sockaddr_in>()
        address.sin_family = AF_INET.convert()
        address.sin_port = (listenPort ?: 0).toNetworkShort()
        address.sin_addr.s_addr = INADDR_ANY
        bind(fd, address.ptr.reinterpret<sockaddr>(), sizeOf<sockaddr_in>().convert()) == 0
    }
    if (!bound) {
        close(fd)
        return null
    }
    return PosixUdpTransport(fd)
}

/** One IPv4 interface that's up and not loopback. */
private class LanInterface(val name: String, val ip: String, val broadcast: String?)

private fun lanInterfaces(): List<LanInterface> = memScoped {
    val head = alloc<CPointerVar<ifaddrs>>()
    if (getifaddrs(head.ptr) != 0) return@memScoped emptyList()
    val result = mutableListOf<LanInterface>()
    var current = head.value
    while (current != null) {
        val entry = current.pointed
        val address = entry.ifa_addr
        val flags = entry.ifa_flags.toInt()
        if (address != null &&
            address.pointed.sa_family.toInt() == AF_INET &&
            flags and IFF_UP != 0 &&
            flags and IFF_LOOPBACK == 0
        ) {
            val ip = inet_ntoa(address.reinterpret<sockaddr_in>().pointed.sin_addr.readValue())?.toKString()
            val broadcast = if (flags and IFF_BROADCAST != 0) {
                entry.ifa_dstaddr?.reinterpret<sockaddr_in>()?.pointed?.sin_addr?.readValue()
                    ?.let { inet_ntoa(it)?.toKString() }
            } else {
                null
            }
            if (ip != null) result += LanInterface(entry.ifa_name?.toKString().orEmpty(), ip, broadcast)
        }
        current = entry.ifa_next
    }
    freeifaddrs(head.value)
    result
}

private fun String.isPrivateIpv4(): Boolean {
    val parts = split('.').mapNotNull { it.toIntOrNull() }
    if (parts.size != 4) return false
    return parts[0] == 10 || (parts[0] == 192 && parts[1] == 168) || (parts[0] == 172 && parts[1] in 16..31)
}

internal actual fun localIpv4Address(): String? =
    // en0 is the Wi-Fi interface on iPhones and Macs.
    lanInterfaces().filter { it.ip.isPrivateIpv4() }.sortedBy { if (it.name == "en0") 0 else 1 }.firstOrNull()?.ip

internal actual fun broadcastAddresses(): List<String> =
    (listOf("255.255.255.255") + lanInterfaces().mapNotNull { it.broadcast }).distinct()

internal actual fun deviceDisplayName(): String = UIDevice.currentDevice.name

internal actual fun osDisplayName(): String = "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}"
