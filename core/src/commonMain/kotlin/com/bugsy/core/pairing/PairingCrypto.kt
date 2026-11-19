package com.bugsy.core.pairing

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** AES-GCM output: a fresh 12-byte [iv] and the [ciphertext] with its authentication tag appended. */
internal class SealedPayload(val iv: ByteArray, val ciphertext: ByteArray)

/**
 * Encrypts [plaintext] with a key derived from [secret] (the random value in the desktop's QR code), binding
 * [aad] (the pairing code) to it. Returns `null` on a platform without an implementation yet (iOS), where the
 * caller falls back to the unencrypted `pair`. See DEB-67.
 */
internal expect fun sealPairingPayload(secret: ByteArray, aad: ByteArray, plaintext: ByteArray): SealedPayload?

/** [publicKey] is the phone's one-time ECDH key the desktop needs to derive the same AES key. */
internal class PeerSealed(val publicKey: ByteArray, val iv: ByteArray, val ciphertext: ByteArray)

/**
 * Encrypts [plaintext] to the desktop's one-time ECDH [peerPublicKey] (P-256, X.509 encoded) with a fresh key pair
 * of our own, binding [aad]. Protects the token against someone who only listens to the network, in the flows that
 * have no QR secret (DEB-67). `null` where unsupported (iOS) or when the key is not a valid P-256 key.
 */
internal expect fun sealForPeer(peerPublicKey: ByteArray, aad: ByteArray, plaintext: ByteArray): PeerSealed?

@OptIn(ExperimentalEncodingApi::class)
internal fun decodeBase64(text: String): ByteArray? = runCatching { Base64.decode(text) }.getOrNull()

@OptIn(ExperimentalEncodingApi::class)
internal fun ByteArray.toBase64(): String = Base64.encode(this)

@OptIn(ExperimentalEncodingApi::class)
internal fun decodeUrlSafeBase64(text: String): ByteArray? =
    runCatching { Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(text) }.getOrNull()
