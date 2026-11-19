package com.bugsy.core.ws

import io.ktor.network.tls.certificates.buildKeyStore
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom

internal const val WS_TLS_KEY_ALIAS = "bugsy-ws"

/** A throwaway identity for one run of the BugsyWsServer: nothing is written to disk, a new one is made per start. */
internal class WsTlsIdentity(val keyStore: KeyStore, val password: CharArray, val fingerprint: String)

/**
 * Generates a self-signed certificate (ECDSA P-256) held only in memory. It is not trusted by anything: the desktop
 * accepts exactly this certificate because it learns its [WsTlsIdentity.fingerprint] over the pairing channel
 * (see DEB-67), which is the pin.
 */
internal fun generateWsTlsIdentity(): WsTlsIdentity {
    val password = ByteArray(24).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }.toCharArray()
    val keyStore = buildKeyStore {
        certificate(WS_TLS_KEY_ALIAS) {
            hash = io.ktor.network.tls.extensions.HashAlgorithm.SHA256
            sign = io.ktor.network.tls.extensions.SignatureAlgorithm.ECDSA
            keySizeInBits = 256
            this.password = String(password)
            // Only the name the desktop connects as; no addresses (a phone's LAN address cannot be known here).
            domains = listOf(BUGSY_TLS_SERVER_NAME)
            ipAddresses = emptyList()
        }
    }
    val encoded = keyStore.getCertificate(WS_TLS_KEY_ALIAS).encoded
    return WsTlsIdentity(keyStore, password, sha256Hex(encoded))
}

internal fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
