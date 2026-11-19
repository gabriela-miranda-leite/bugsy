package com.bugsy.desktop.pairing

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** UDP port this desktop listens on for pairing messages. Mirrors `:core`'s `PAIRING_DESKTOP_UDP_PORT`. */
const val PAIRING_DESKTOP_UDP_PORT = 7790

/** UDP port every device running Bugsy listens on for discovery. Mirrors `:core`'s `PAIRING_DEVICE_UDP_PORT`. */
const val PAIRING_DEVICE_UDP_PORT = 7791

const val PAIRING_CODE_LENGTH = 6

/**
 * One datagram of the pairing protocol (DEB-64). Mirrors `:core`'s `PairingMessage`, which documents the flows:
 * this desktop module doesn't depend on `:core`, same as [com.bugsy.desktop.wire.WireProtocol] for the WS protocol.
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
    /** SHA-256 hex of the phone's TLS certificate. Mirrors `:core`'s `PairingMessage.fp`. */
    val fp: String? = null,
    /** AES-GCM nonce (base64) of [enc]. */
    val iv: String? = null,
    /** AES-GCM of `{"token","fp"}` (base64), keyed by the QR secret and bound to [code]. Replaces [token] and [fp]. */
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
 * What the QR code carries: `bugsy://pair?host=192.168.0.10&port=7790&code=482913&key=<secret>`. The secret never
 * leaves the screen, and the phone uses it to encrypt the token (DEB-67).
 */
fun pairingQrText(host: String, port: Int, code: String, secret: String? = null): String =
    "bugsy://pair?host=$host&port=$port&code=$code" + (secret?.let { "&key=$it" } ?: "")
