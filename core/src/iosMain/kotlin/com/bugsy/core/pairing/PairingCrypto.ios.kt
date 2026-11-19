@file:OptIn(ExperimentalForeignApi::class)

package com.bugsy.core.pairing

import com.bugsy.core.secureRandomBytes
import com.bugsy.core.ws.toByteArray
import com.bugsy.core.ws.toCFData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCHmacAlgSHA256
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreCrypto.kCCSuccess
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Security.SecKeyCopyExternalRepresentation
import platform.Security.SecKeyCopyKeyExchangeResult
import platform.Security.SecKeyCopyPublicKey
import platform.Security.SecKeyCreateRandomKey
import platform.Security.SecKeyCreateWithData
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPublic
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecKeyAlgorithmECDHKeyExchangeStandard

/*
 * iOS has no public AES-GCM, so the pairing is sealed with AES-256-CBC and then authenticated with HMAC-SHA256
 * (encrypt-then-MAC), marked `alg = "cbc-hmac"` on the wire. The keys come from the same secret as the GCM
 * variants: SHA-256 over a label and the secret (QR flow) or the ECDH shared secret (code and list flows), and the
 * cipher and MAC keys are split from it. The desktop's `PairingCrypto.kt` opens this format. DEB-67.
 */

/** DER prefix of an X.509 SubjectPublicKeyInfo for an uncompressed P-256 point: followed by the 65 point bytes. */
private val SPKI_P256_PREFIX = byteArrayOf(
    0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x02, 0x01,
    0x06, 0x08, 0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x03, 0x01, 0x07, 0x03, 0x42, 0x00,
)

internal actual fun sealPairingPayload(secret: ByteArray, aad: ByteArray, plaintext: ByteArray): SealedPayload? = runCatching {
    val key = sha256("bugsy-pair-v1".encodeToByteArray() + secret)
    val (iv, sealed) = sealCbcHmac(key, aad, plaintext)
    SealedPayload(iv, sealed, ALG_CBC_HMAC)
}.getOrNull()

internal actual fun pairingSha256(input: ByteArray): ByteArray = sha256(input)

internal actual fun openPeerSession(peerPublicKey: ByteArray): PeerSession? = runCatching {
    if (peerPublicKey.size != SPKI_P256_PREFIX.size + 65 || !peerPublicKey.copyOfRange(0, SPKI_P256_PREFIX.size).contentEquals(SPKI_P256_PREFIX)) {
        return@runCatching null
    }
    val point = peerPublicKey.copyOfRange(SPKI_P256_PREFIX.size, peerPublicKey.size)
    memScoped {
        val error = alloc<CFErrorRefVar>()
        val peer = SecKeyCreateWithData(
            point.toCFData(),
            dictionaryOf(kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom, kSecAttrKeyClass to kSecAttrKeyClassPublic),
            error.ptr,
        ) ?: return@memScoped null
        val ownPrivate = SecKeyCreateRandomKey(
            dictionaryOf(kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom, kSecAttrKeySizeInBits to 256.toNumber()),
            error.ptr,
        ) ?: return@memScoped null
        val ownPoint = SecKeyCopyExternalRepresentation(SecKeyCopyPublicKey(ownPrivate), error.ptr)?.toByteArray() ?: return@memScoped null
        val shared = SecKeyCopyKeyExchangeResult(ownPrivate, kSecKeyAlgorithmECDHKeyExchangeStandard, peer, dictionaryOf(), error.ptr)
            ?.toByteArray() ?: return@memScoped null
        val key = sha256("bugsy-pair-v2".encodeToByteArray() + shared)
        val ownPublicKey = SPKI_P256_PREFIX + ownPoint
        object : PeerSession {
            override val publicKey: ByteArray = ownPublicKey

            override fun seal(aad: ByteArray, plaintext: ByteArray): PeerSealed? = runCatching {
                val (iv, sealed) = sealCbcHmac(key, aad, plaintext)
                PeerSealed(ownPublicKey, iv, sealed, ALG_CBC_HMAC)
            }.getOrNull()
        }
    }
}.getOrNull()

/** Returns the IV and `ciphertext || mac`. The MAC covers the AAD (length-prefixed), the IV and the ciphertext. */
private fun sealCbcHmac(key: ByteArray, aad: ByteArray, plaintext: ByteArray): Pair<ByteArray, ByteArray> {
    val encryptionKey = sha256("bugsy-enc".encodeToByteArray() + key)
    val macKey = sha256("bugsy-mac".encodeToByteArray() + key)
    val iv = secureRandomBytes(16)
    val ciphertext = aesCbcEncrypt(encryptionKey, iv, plaintext)
    val mac = hmacSha256(macKey, intBytes(aad.size) + aad + iv + ciphertext)
    return iv to (ciphertext + mac)
}

private fun intBytes(value: Int) = byteArrayOf((value shr 24).toByte(), (value shr 16).toByte(), (value shr 8).toByte(), value.toByte())

private fun sha256(input: ByteArray): ByteArray {
    val digest = UByteArray(32)
    input.usePinned { source ->
        digest.usePinned { target -> CC_SHA256(if (input.isEmpty()) null else source.addressOf(0), input.size.convert(), target.addressOf(0)) }
    }
    return ByteArray(32) { digest[it].toByte() }
}

private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    val mac = UByteArray(32)
    key.usePinned { k -> data.usePinned { d -> mac.usePinned { m ->
        CCHmac(kCCHmacAlgSHA256, k.addressOf(0), key.size.convert(), d.addressOf(0), data.size.convert(), m.addressOf(0))
    } } }
    return ByteArray(32) { mac[it].toByte() }
}

private fun aesCbcEncrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray {
    val out = ByteArray(plaintext.size + 16)
    val written = memScoped {
        val moved = alloc<platform.posix.size_tVar>()
        val status = key.usePinned { k -> iv.usePinned { v -> plaintext.usePinned { p -> out.usePinned { o ->
            CCCrypt(
                kCCEncrypt, kCCAlgorithmAES, kCCOptionPKCS7Padding,
                k.addressOf(0), key.size.convert(), v.addressOf(0),
                if (plaintext.isEmpty()) null else p.addressOf(0), plaintext.size.convert(),
                o.addressOf(0), out.size.convert(), moved.ptr,
            )
        } } } }
        check(status == kCCSuccess) { "CCCrypt failed ($status)" }
        moved.value.toInt()
    }
    return out.copyOfRange(0, written)
}

private fun dictionaryOf(vararg pairs: Pair<platform.CoreFoundation.CFTypeRef?, platform.CoreFoundation.CFTypeRef?>) =
    CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr).also { dictionary ->
        pairs.forEach { (key, value) -> CFDictionaryAddValue(dictionary, key, value) }
    }

private fun Int.toNumber(): platform.CoreFoundation.CFTypeRef? = memScoped {
    val value = alloc<kotlinx.cinterop.IntVar>().also { it.value = this@toNumber }
    platform.CoreFoundation.CFNumberCreate(null, platform.CoreFoundation.kCFNumberIntType, value.ptr)
}
