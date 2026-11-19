package com.bugsy.desktop.pairing

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val GCM_TAG_BITS = 128

/** A fresh 128-bit secret for the QR code, base64url without padding. Only the desktop screen ever shows it. */
internal fun randomSecret(): String {
    val bytes = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

/** Must match `:core`'s `deriveAesKey`: SHA-256 over a fixed label and the QR secret. */
private fun deriveKey(secret: ByteArray): SecretKeySpec {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update("bugsy-pair-v1".encodeToByteArray())
    return SecretKeySpec(digest.digest(secret), "AES")
}

/** What a sealed `pair` carries once opened. */
internal class PairSecret(val token: String, val fingerprint: String?)

/**
 * Opens the phone's `enc` with the QR [secret] and [code] as authenticated data. `null` for anything that is not
 * exactly what that secret produced: a wrong key, a different code or a tampered byte all fail the GCM tag, so
 * a forged `pair` cannot get a token accepted.
 */
internal fun openPairSecret(secretBase64Url: String, code: String, ivBase64: String, encBase64: String): PairSecret? = runCatching {
    val secret = Base64.getUrlDecoder().decode(secretBase64Url)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, deriveKey(secret), GCMParameterSpec(GCM_TAG_BITS, Base64.getDecoder().decode(ivBase64)))
    cipher.updateAAD(code.encodeToByteArray())
    parsePairSecret(cipher.doFinal(Base64.getDecoder().decode(encBase64)).decodeToString())
}.getOrNull()

private fun parsePairSecret(json: String): PairSecret? {
    val parsed = kotlinx.serialization.json.Json.parseToJsonElement(json) as? kotlinx.serialization.json.JsonObject ?: return null
    val token = (parsed["token"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.takeIf { it.isNotBlank() } ?: return null
    val fp = (parsed["fp"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
    return PairSecret(token, fp)
}

/** A one-time P-256 key pair: the public half goes to the phone, the private half never leaves this process. */
internal fun newEcdhKeyPair(): KeyPair =
    KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

internal fun KeyPair.publicKeyBase64(): String = Base64.getEncoder().encodeToString(public.encoded)

/** Must match `:core`'s `deriveEcdhAesKey`. */
private fun deriveEcdhKey(shared: ByteArray): SecretKeySpec {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update("bugsy-pair-v2".encodeToByteArray())
    return SecretKeySpec(digest.digest(shared), "AES")
}

/**
 * Opens a `pair` the phone sealed to our one-time key: ECDH with the phone's [phonePublicKey], then AES-GCM with
 * [aad] (the code or the nonce) as authenticated data. `null` for anything that does not open cleanly.
 */
internal fun openPeerSealed(own: KeyPair, phonePublicKey: String, aad: String, ivBase64: String, encBase64: String): PairSecret? = runCatching {
    val phone = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(phonePublicKey)))
    val shared = KeyAgreement.getInstance("ECDH").apply {
        init(own.private)
        doPhase(phone, true)
    }.generateSecret()
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, deriveEcdhKey(shared), GCMParameterSpec(GCM_TAG_BITS, Base64.getDecoder().decode(ivBase64)))
    cipher.updateAAD(aad.encodeToByteArray())
    parsePairSecret(cipher.doFinal(Base64.getDecoder().decode(encBase64)).decodeToString())
}.getOrNull()
