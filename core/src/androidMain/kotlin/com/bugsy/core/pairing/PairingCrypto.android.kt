package com.bugsy.core.pairing

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
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

/** Must match the desktop's key derivation for the ECDH flows: SHA-256 over a fixed label and the shared secret. */
private fun deriveEcdhAesKey(shared: ByteArray): SecretKeySpec {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update("bugsy-pair-v2".encodeToByteArray())
    return SecretKeySpec(digest.digest(shared), "AES")
}

internal actual fun pairingSha256(input: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(input)

internal actual fun openPeerSession(peerPublicKey: ByteArray): PeerSession? = runCatching {
    val peer = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(peerPublicKey))
    val own = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    val shared = KeyAgreement.getInstance("ECDH").apply {
        init(own.private)
        doPhase(peer, true)
    }.generateSecret()
    val key = deriveEcdhAesKey(shared)
    object : PeerSession {
        override val publicKey: ByteArray = own.public.encoded

        override fun seal(aad: ByteArray, plaintext: ByteArray): PeerSealed? = runCatching {
            val iv = ByteArray(GCM_IV_BYTES).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(aad)
            PeerSealed(publicKey, iv, cipher.doFinal(plaintext))
        }.getOrNull()
    }
}.getOrNull()
