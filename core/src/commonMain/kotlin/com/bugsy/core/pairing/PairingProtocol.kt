package com.bugsy.core.pairing

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** UDP port Bugsy Desktop listens on for pairing messages (DEB-64). */
const val PAIRING_DESKTOP_UDP_PORT: Int = 7790

/** UDP port every mobile device running Bugsy listens on, so the desktop can discover it (DEB-64). */
const val PAIRING_DEVICE_UDP_PORT: Int = 7791

const val PAIRING_CODE_LENGTH: Int = 6

/**
 * One datagram of the pairing protocol (DEB-64), discriminated by [type]. A single flat class instead of a sealed
 * hierarchy keeps the wire format trivial to mirror in the desktop module, which doesn't depend on `:core`.
 *
 * The mobile device is the WebSocket server and the desktop is the client, so pairing is only about handing the
 * desktop the device's address and session token:
 *
 * ```
 * code flow   mobile --discover{code}--> (broadcast)      desktop --desktop_here{name}--> mobile
 *             mobile --pair{code,port,token,name,os}-->   desktop --pair_result{ok}-->    mobile
 * QR flow     the same, minus `discover` (the QR already carries the desktop's address)
 * list flow   desktop --who--> (broadcast)                mobile --device{name,os,port}--> desktop
 *             desktop --connect_request{nonce,name}-->    mobile (user approves) --pair{nonce,...}--> desktop
 * ```
 *
 * The token only ever travels in a unicast `pair`, never in a broadcast.
 */
@Serializable
data class PairingMessage(
    val type: String,
    val code: String? = null,
    val nonce: String? = null,
    val port: Int? = null,
    val token: String? = null,
    val name: String? = null,
    val os: String? = null,
    val ok: Boolean? = null,
    val reason: String? = null,
    /** SHA-256 hex of the device's TLS certificate, which the desktop pins. Absent when the device has no TLS. */
    val fp: String? = null,
    /** With [enc]: the AES-GCM nonce (base64). In the QR flow `enc` replaces [token] and [fp], so neither is in clear. */
    val iv: String? = null,
    /** AES-GCM of `{"token","fp"}` (base64), keyed by the secret in the QR code and bound to [code]. */
    val enc: String? = null,
) {
    companion object {
        const val DISCOVER = "discover"
        const val DESKTOP_HERE = "desktop_here"
        const val PAIR = "pair"
        const val PAIR_RESULT = "pair_result"
        const val WHO = "who"
        const val DEVICE = "device"
        const val CONNECT_REQUEST = "connect_request"

        const val REASON_INVALID = "invalid"
        const val REASON_EXPIRED = "expired"
        const val REASON_LOCKED = "locked"
    }
}

private val pairingJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

fun PairingMessage.encode(): ByteArray = pairingJson.encodeToString(PairingMessage.serializer(), this).encodeToByteArray()

fun decodePairingMessage(bytes: ByteArray): PairingMessage? =
    runCatching { pairingJson.decodeFromString(PairingMessage.serializer(), bytes.decodeToString()) }.getOrNull()

/**
 * What the desktop's QR code carries: `bugsy://pair?host=192.168.0.10&port=7790&code=482913&key=<base64url>`.
 * `key` is a random 128-bit secret only the screen shows; with it the phone encrypts the token (DEB-67), so
 * someone sniffing the Wi-Fi sees neither the token nor the certificate fingerprint. Optional for older desktops.
 */
data class PairingQr(val host: String, val port: Int, val code: String, val secret: String? = null)

private const val QR_PREFIX = "bugsy://pair?"

fun PairingQr.toQrText(): String =
    "${QR_PREFIX}host=$host&port=$port&code=$code" + (secret?.let { "&key=$it" } ?: "")

fun parsePairingQr(text: String): PairingQr? {
    if (!text.startsWith(QR_PREFIX)) return null
    val params = text.removePrefix(QR_PREFIX).split('&').mapNotNull {
        val index = it.indexOf('=')
        if (index <= 0) null else it.substring(0, index) to it.substring(index + 1)
    }.toMap()
    val host = params["host"]?.takeIf { it.isNotBlank() } ?: return null
    val port = params["port"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
    val code = params["code"]?.takeIf { it.length == PAIRING_CODE_LENGTH && it.all(Char::isDigit) } ?: return null
    val secret = params["key"]?.takeIf { it.isNotBlank() }
    return PairingQr(host, port, code, secret)
}

/** A received datagram plus who sent it, so replies can go straight back. */
class UdpPacket(val data: ByteArray, val fromHost: String, val fromPort: Int)
