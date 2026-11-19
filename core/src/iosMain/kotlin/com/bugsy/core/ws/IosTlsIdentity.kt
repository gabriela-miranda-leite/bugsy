@file:OptIn(ExperimentalForeignApi::class)

package com.bugsy.core.ws

import com.bugsy.core.secureRandomBytes
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.CoreCrypto.CC_SHA256
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.Security.SecCertificateCreateWithData
import platform.Security.SecItemCopyMatching
import platform.Security.kSecClassIdentity
import platform.Security.kSecReturnRef
import platform.Security.SecItemAdd
import platform.Security.SecItemDelete
import platform.Security.SecKeyCopyExternalRepresentation
import platform.Security.SecKeyCopyPublicKey
import platform.Security.SecKeyCreateRandomKey
import platform.Security.SecKeyCreateSignature
import platform.Security.errSecSuccess
import platform.Security.kSecAttrApplicationTag
import platform.Security.kSecAttrIsPermanent
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecAttrLabel
import platform.Security.kSecClass
import platform.Security.kSecClassCertificate
import platform.Security.kSecClassKey
import platform.Security.kSecKeyAlgorithmECDSASignatureMessageX962SHA256
import platform.Security.kSecPrivateKeyAttrs
import platform.Security.kSecValueRef

/** The TLS identity of one run of the iOS BugsyWsServer: the key and certificate (in the Keychain) and what to pin. */
internal class IosTlsIdentity(val identity: COpaquePointer, val fingerprint: String, private val remove: () -> Unit) {
    /** Takes the key and the certificate back out of the Keychain: nothing of a previous run is left behind. */
    fun destroy() = remove()
}

private const val KEY_TAG = "dev.bugsy.ws.tls"
private const val CERT_LABEL = "dev.bugsy.ws.tls"
private const val ONE_DAY_SECONDS = 86_400L

/**
 * Makes a throwaway P-256 key and a self-signed certificate for it, and the `SecIdentity` the TLS server needs.
 * The key has to live in the Keychain for `SecIdentityCreateWithCertificate` to pair it with the certificate, so it
 * is removed again by [IosTlsIdentity.destroy] and also cleared at the start of the next run, in case the app was
 * killed before it could. `null` when the Keychain is not available (for example a test process with no app).
 */
internal fun createIosTlsIdentity(): IosTlsIdentity? = memScoped {
    purgeKeychainItems()
    val tag = KEY_TAG.encodeToByteArray().toCFData() ?: return@memScoped null
    val error = alloc<CFErrorRefVar>()

    val keySize = alloc<IntVar>().also { it.value = 256 }
    val sizeNumber = CFNumberCreate(null, kCFNumberIntType, keySize.ptr)
    val privateAttributes = cfDictionary(kSecAttrIsPermanent to kCFBooleanTrue, kSecAttrApplicationTag to tag)
    val parameters = cfDictionary(
        kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
        kSecAttrKeySizeInBits to sizeNumber,
        kSecPrivateKeyAttrs to privateAttributes,
    )
    val privateKey = SecKeyCreateRandomKey(parameters, error.ptr) ?: return@memScoped null
    val publicKey = SecKeyCopyPublicKey(privateKey) ?: return@memScoped null
    val point = SecKeyCopyExternalRepresentation(publicKey, error.ptr)?.toByteArray() ?: return@memScoped null

    var signFailed = false
    val serial = secureRandomBytes(8).also { it[0] = (it[0].toInt() and 0x7F).toByte() }
    val now = NSDate().timeIntervalSince1970.toLong()
    val der = buildSelfSignedCertificate(point, serial, now - ONE_DAY_SECONDS, now + 365 * ONE_DAY_SECONDS) { tbs ->
        val data = tbs.toCFData()
        val signature = SecKeyCreateSignature(privateKey, kSecKeyAlgorithmECDSASignatureMessageX962SHA256, data, error.ptr)?.toByteArray()
        if (signature == null) signFailed = true
        signature ?: ByteArray(0)
    }
    if (signFailed) return@memScoped null

    val certificate = SecCertificateCreateWithData(null, der.toCFData()) ?: return@memScoped null
    val add = cfDictionary(kSecClass to kSecClassCertificate, kSecValueRef to certificate, kSecAttrLabel to CERT_LABEL.toCFString())
    if (SecItemAdd(add, null) != errSecSuccess) return@memScoped null

    // On iOS the identity is looked up in the Keychain: it is the certificate matched with the key made above.
    val found = alloc<COpaquePointerVar>()
    val query = cfDictionary(kSecClass to kSecClassIdentity, kSecAttrLabel to CERT_LABEL.toCFString(), kSecReturnRef to kCFBooleanTrue)
    if (SecItemCopyMatching(query, found.ptr) != errSecSuccess) {
        purgeKeychainItems()
        return@memScoped null
    }
    val identityRef = found.value ?: return@memScoped null
    IosTlsIdentity(identityRef, sha256Hex(der)) { purgeKeychainItems() }
}

private fun purgeKeychainItems() {
    val tag = KEY_TAG.encodeToByteArray().toCFData()
    SecItemDelete(cfDictionary(kSecClass to kSecClassKey, kSecAttrApplicationTag to tag))
    SecItemDelete(cfDictionary(kSecClass to kSecClassCertificate, kSecAttrLabel to CERT_LABEL.toCFString()))
}

private fun cfDictionary(vararg pairs: Pair<CFTypeRef?, CFTypeRef?>) =
    CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr).also { dictionary ->
        pairs.forEach { (key, value) -> CFDictionaryAddValue(dictionary, key, value) }
    }

private fun String.toCFString(): CFTypeRef? = platform.CoreFoundation.CFStringCreateWithCString(null, this, platform.CoreFoundation.kCFStringEncodingUTF8)

internal fun ByteArray.toCFData(): CFDataRef? =
    if (isEmpty()) CFDataCreate(null, null, 0) else usePinned { CFDataCreate(null, it.addressOf(0).reinterpret(), size.convert()) }

internal fun CFDataRef.toByteArray(): ByteArray {
    val length = CFDataGetLength(this).toInt()
    val bytes = CFDataGetBytePtr(this) ?: return ByteArray(0)
    return ByteArray(length) { bytes[it].toByte() }.also { CFRelease(this) }
}

private fun sha256Hex(bytes: ByteArray): String {
    val digest = UByteArray(32)
    bytes.usePinned { input -> digest.usePinned { output -> CC_SHA256(input.addressOf(0), bytes.size.convert(), output.addressOf(0)) } }
    return digest.joinToString("") { it.toString(16).padStart(2, '0') }
}
