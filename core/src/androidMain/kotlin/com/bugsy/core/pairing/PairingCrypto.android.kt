package com.bugsy.core.pairing

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val GCM_TAG_BITS = 128
private const val GCM_IV_BYTES = 12

/** Must match the desktop's `deriveKey`: SHA-256 over a fixed label and the QR secret, so the label can change with the protocol. */
internal fun deriveAesKey(secret: ByteArray): SecretKeySpec {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update("bugsy-pair-v1".encodeToByteArray())
    return SecretKeySpec(digest.digest(secret), "AES")
}

internal actual fun sealPairingPayload(secret: ByteArray, aad: ByteArray, plaintext: ByteArray): SealedPayload? = runCatching {
    val iv = ByteArray(GCM_IV_BYTES).also { SecureRandom().nextBytes(it) }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, deriveAesKey(secret), GCMParameterSpec(GCM_TAG_BITS, iv))
    cipher.updateAAD(aad)
    SealedPayload(iv, cipher.doFinal(plaintext))
}.getOrNull()
