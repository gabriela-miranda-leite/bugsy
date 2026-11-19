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
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val GCM_TAG_BITS = 128

/** A fresh 128-bit secret for the QR code, base64url without padding. Only the desktop screen ever shows it. */
internal fun randomSecret(): String {
    val bytes = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

/** SHA-256 over a fixed label and a secret: the 32-byte key every sealing variant starts from. `:core` does the same. */
private fun labelledKey(label: String, secret: ByteArray): ByteArray =
    MessageDigest.getInstance("SHA-256").apply { update(label.encodeToByteArray()) }.digest(secret)

/**
 * Opens [encBase64] with [key]: AES-256-GCM by default, or (alg `cbc-hmac`, what an iOS phone sends) AES-256-CBC
 * with the HMAC-SHA256 checked first, in constant time. Throws on anything that does not authenticate.
 */
private fun open(key: ByteArray, aad: ByteArray, ivBase64: String, encBase64: String, alg: String?): ByteArray {
    val iv = Base64.getDecoder().decode(ivBase64)
    val sealed = Base64.getDecoder().decode(encBase64)
    return when (alg) {
        null -> {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(aad)
            cipher.doFinal(sealed)
        }
        "cbc-hmac" -> {
            require(sealed.size > 32 && iv.size == 16) { "malformed cbc-hmac payload" }
            val ciphertext = sealed.copyOfRange(0, sealed.size - 32)
            val mac = sealed.copyOfRange(sealed.size - 32, sealed.size)
            val encryptionKey = labelledKey("bugsy-enc", key)
            val macKey = labelledKey("bugsy-mac", key)
            val hmac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(macKey, "HmacSHA256")) }
            val aadLength = byteArrayOf((aad.size shr 24).toByte(), (aad.size shr 16).toByte(), (aad.size shr 8).toByte(), aad.size.toByte())
            val expected = hmac.doFinal(aadLength + aad + iv + ciphertext)
            require(MessageDigest.isEqual(expected, mac)) { "bad MAC" }
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(encryptionKey, "AES"), IvParameterSpec(iv))
            cipher.doFinal(ciphertext)
        }
        else -> throw IllegalArgumentException("unknown alg $alg")
    }
}

/** What a sealed `pair` carries once opened. */
internal class PairSecret(val token: String, val fingerprint: String?)

/**
 * Opens the phone's `enc` with the QR [secretBase64Url] and [code] as authenticated data. `null` for anything that is
 * not exactly what that secret produced: a wrong key, a different code or a tampered byte all fail authentication,
 * so a forged `pair` cannot get a token accepted.
 */
internal fun openPairSecret(secretBase64Url: String, code: String, ivBase64: String, encBase64: String, alg: String? = null): PairSecret? = runCatching {
    val secret = Base64.getUrlDecoder().decode(secretBase64Url)
    parsePairSecret(open(labelledKey("bugsy-pair-v1", secret), code.encodeToByteArray(), ivBase64, encBase64, alg).decodeToString())
}.getOrNull()

/** A one-time P-256 key pair: the public half goes to the phone, the private half never leaves this process. */
internal fun newEcdhKeyPair(): KeyPair =
    KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

internal fun KeyPair.publicKeyBase64(): String = Base64.getEncoder().encodeToString(public.encoded)

/**
 * Opens a `pair` the phone sealed to our one-time key: ECDH with the phone's [phonePublicKey], then the shared
 * secret becomes the key (SHA-256 over a label and the secret) for AES-GCM, or AES-CBC with HMAC for an iOS phone
 * ([alg]), with [aad] (the code or the nonce) as authenticated data. `null` for anything that does not open cleanly.
 */
internal fun openPeerSealed(own: KeyPair, phonePublicKey: String, aad: String, ivBase64: String, encBase64: String, alg: String? = null): PairSecret? = runCatching {
    val phone = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(phonePublicKey)))
    val shared = KeyAgreement.getInstance("ECDH").apply {
        init(own.private)
        doPhase(phone, true)
    }.generateSecret()
    parsePairSecret(open(labelledKey("bugsy-pair-v2", shared), aad.encodeToByteArray(), ivBase64, encBase64, alg).decodeToString())
}.getOrNull()

private fun parsePairSecret(json: String): PairSecret? {
    val parsed = kotlinx.serialization.json.Json.parseToJsonElement(json) as? kotlinx.serialization.json.JsonObject ?: return null
    val token = (parsed["token"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.takeIf { it.isNotBlank() } ?: return null
    val fp = (parsed["fp"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
    return PairSecret(token, fp)
}
