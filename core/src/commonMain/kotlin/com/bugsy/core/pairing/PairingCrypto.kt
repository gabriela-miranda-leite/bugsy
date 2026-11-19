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

@OptIn(ExperimentalEncodingApi::class)
internal fun ByteArray.toBase64(): String = Base64.encode(this)

@OptIn(ExperimentalEncodingApi::class)
internal fun decodeUrlSafeBase64(text: String): ByteArray? =
    runCatching { Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(text) }.getOrNull()
