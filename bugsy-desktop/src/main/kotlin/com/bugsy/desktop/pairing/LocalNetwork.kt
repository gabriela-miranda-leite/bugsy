package com.bugsy.desktop.pairing

import java.net.Inet4Address
import java.net.NetworkInterface

private fun lanInterfaces(): List<NetworkInterface> =
    runCatching { NetworkInterface.getNetworkInterfaces().toList() }.getOrDefault(emptyList())
        .filter { runCatching { it.isUp && !it.isLoopback && !it.isVirtual && !it.isPointToPoint }.getOrDefault(false) }

/** This machine's LAN IPv4 address as a phone on the same Wi-Fi would reach it, or `null` when offline. */
fun localIpv4Address(): String? =
    lanInterfaces()
        .sortedBy { if (it.name.startsWith("en") || it.name.startsWith("wlan") || it.name.startsWith("eth")) 0 else 1 }
        .firstNotNullOfOrNull { iface ->
            iface.inetAddresses.toList().filterIsInstance<Inet4Address>().firstOrNull { it.isSiteLocalAddress }?.hostAddress
        }

/** The limited broadcast plus each interface's directed broadcast — some networks only forward one of them. */
fun broadcastAddresses(): List<String> {
    val directed = lanInterfaces().flatMap { iface -> iface.interfaceAddresses.mapNotNull { it.broadcast?.hostAddress } }
    return (listOf("255.255.255.255") + directed).distinct()
}

/** Shown to the phone user when approving a connection request from this machine. */
fun desktopDisplayName(): String =
    runCatching { java.net.InetAddress.getLocalHost().hostName }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Bugsy Desktop"
