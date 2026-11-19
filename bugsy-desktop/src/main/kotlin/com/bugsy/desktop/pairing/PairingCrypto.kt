package com.bugsy.desktop.pairing

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
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
    val json = cipher.doFinal(Base64.getDecoder().decode(encBase64)).decodeToString()
    val parsed = kotlinx.serialization.json.Json.parseToJsonElement(json) as kotlinx.serialization.json.JsonObject
    val token = (parsed["token"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.takeIf { it.isNotBlank() } ?: return@runCatching null
    val fp = (parsed["fp"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
    PairSecret(token, fp)
}.getOrNull()
