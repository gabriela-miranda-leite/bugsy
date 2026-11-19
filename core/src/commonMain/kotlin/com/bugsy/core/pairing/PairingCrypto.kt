package com.bugsy.core.pairing

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** The value of `PairingMessage.alg` for AES-CBC with HMAC-SHA256 (encrypt-then-MAC), used where AES-GCM is not available (iOS). */
internal const val ALG_CBC_HMAC = "cbc-hmac"

/** AES-GCM output: a fresh 12-byte [iv] and the [ciphertext] with its authentication tag appended. */
internal class SealedPayload(val iv: ByteArray, val ciphertext: ByteArray, val alg: String? = null)

/**
 * Encrypts [plaintext] with a key derived from [secret] (the random value in the desktop's QR code), binding
 * [aad] (the pairing code) to it. Returns `null` on a platform without an implementation yet (iOS), where the
 * caller falls back to the unencrypted `pair`. See DEB-67.
 */
internal expect fun sealPairingPayload(secret: ByteArray, aad: ByteArray, plaintext: ByteArray): SealedPayload?

/** [publicKey] is the phone's one-time ECDH key the desktop needs to derive the same AES key. */
internal class PeerSealed(val publicKey: ByteArray, val iv: ByteArray, val ciphertext: ByteArray, val alg: String? = null)

/**
 * A one-time ECDH key pair of ours, already combined with the desktop's one-time public key (P-256, X.509 encoded).
 * [publicKey] is what the desktop needs to derive the same key. Separate from [seal] so the short verification code
 * can be shown before anything secret is sent (DEB-67).
 */
internal interface PeerSession {
    val publicKey: ByteArray

    /** Encrypts [plaintext] with the shared key, binding [aad]. */
    fun seal(aad: ByteArray, plaintext: ByteArray): PeerSealed?
}

/** `null` where unsupported or when the key is not a valid P-256 key. */
internal expect fun openPeerSession(peerPublicKey: ByteArray): PeerSession?

internal fun sealForPeer(peerPublicKey: ByteArray, aad: ByteArray, plaintext: ByteArray): PeerSealed? =
    openPeerSession(peerPublicKey)?.seal(aad, plaintext)

internal expect fun pairingSha256(input: ByteArray): ByteArray

/**
 * The short code both screens show, so a person can tell the phone and the desktop are really talking to each other
 * and not to someone in between (DEB-67). It is a hash of both one-time public keys and of [context] (the pairing code
 * or nonce): whoever answers in the desktop's place has to use a key of their own on each side, and then the two
 * screens show different codes. 4 digits is enough because every wrong guess is a mismatch the person sees.
 */
internal fun verificationCode(desktopPublicKey: ByteArray, phonePublicKey: ByteArray, context: String): String {
    fun field(bytes: ByteArray) = byteArrayOf((bytes.size shr 8).toByte(), bytes.size.toByte()) + bytes
    val digest = pairingSha256("bugsy-sas-v1".encodeToByteArray() + field(desktopPublicKey) + field(phonePublicKey) + field(context.encodeToByteArray()))
    val value = ((digest[0].toInt() and 0xFF) shl 24) or ((digest[1].toInt() and 0xFF) shl 16) or ((digest[2].toInt() and 0xFF) shl 8) or (digest[3].toInt() and 0xFF)
    return (value.toUInt() % 10_000u).toString().padStart(4, '0')
}

@OptIn(ExperimentalEncodingApi::class)
internal fun decodeBase64(text: String): ByteArray? = runCatching { Base64.decode(text) }.getOrNull()

@OptIn(ExperimentalEncodingApi::class)
internal fun ByteArray.toBase64(): String = Base64.encode(this)

@OptIn(ExperimentalEncodingApi::class)
internal fun decodeUrlSafeBase64(text: String): ByteArray? =
    runCatching { Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(text) }.getOrNull()
