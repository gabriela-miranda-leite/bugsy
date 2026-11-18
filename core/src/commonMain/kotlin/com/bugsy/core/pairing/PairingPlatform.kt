package com.bugsy.core.pairing

/** A bound (or ephemeral) UDP socket. Blocking by design: callers run it off the main thread. */
internal interface UdpTransport {
    /** Returns `false` if the datagram couldn't be sent (no network, unreachable...). */
    fun send(host: String, port: Int, payload: ByteArray): Boolean

    /** Blocks up to [timeoutMs] for one datagram; `null` on timeout or once closed. */
    fun receive(timeoutMs: Int): UdpPacket?

    fun close()
}

/**
 * Opens a UDP socket with SO_BROADCAST set. Bound to [listenPort] when given (with address reuse), otherwise to an
 * ephemeral port. `null` when the socket can't be opened — e.g. the port is already taken.
 */
internal expect fun openUdpTransport(listenPort: Int? = null): UdpTransport?

/** The device's LAN IPv4 address (Wi-Fi first), or `null` when it isn't on a network. */
internal expect fun localIpv4Address(): String?

/** Addresses to broadcast to: the limited broadcast plus each interface's directed broadcast. */
internal expect fun broadcastAddresses(): List<String>

internal expect fun deviceDisplayName(): String

internal expect fun osDisplayName(): String
