package com.bugsy.desktop.pairing

import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * What an iPhone sends, produced by the real iOS crypto stack (`IosPairingCryptoTest` in :core, run on the
 * simulator) and sealed to the fixed test key below. The desktop has to open it: this is what keeps the two sides
 * speaking the same `cbc-hmac` format. The key exists only for this test.
 */
class IosInteropTest {
    private val keys = KeyFactory.getInstance("EC").let { factory ->
        KeyPair(
            factory.generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEFYixex2i4D1nAn5SxqeTQeScvF1NOny7lhJyXSYy0FN0RFbaG2mN5rlGQijOgDmDqR9wtgJ4c2YxwTqHcX6OIA=="))),
            factory.generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode("MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgU32tgHOiuTqxz+dy4nRN2sbSOGwmXGMYQ07sk1iZEo+hRANCAAQViLF7HaLgPWcCflLGp5NB5Jy8XU06fLuWEnJdJjLQU3REVtobaY3muUZCKM6AOYOpH3C2AnhzZjHBOodxfo4g"))),
        )
    }

    private val peerCpk = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEKmU52iCkdmOtwzvjcii/nqxMd0ZSQ/abPSmvUo1XDdxHT5qYInrxPlLILflpJtQ+lJXbsp08XUvThqAeWXZWDQ=="
    private val peerIv = "1XPDUTBN+5HEfcojpD+4RA=="
    private val peerEnc = "GyEExNs8SXV0ijyRRZCv0nr0rtNWGfqsysxkNQZylfQR6HTZrQTE7jz9tzUnkfF5mQVK4uVWgXEVkBs9HkqlfK8JmNXE8OP4nEE0GdKYs/s="

    private val qrIv = "EnmvQ5Sf4QLGwpyWc9116w=="
    private val qrEnc = "gOi6CzsZ1eUIe4E/oJi9M7To5VYawiEXL2nCQzMRRPHAqDdIGaD6BlRc2RMin7B6x/CgECr9aJMvQ2tgppFCyqnGHqgsi1WIaIFnn5cRe+o="

    @Test
    fun `opens an iOS code or list flow pair sealed to the desktop key`() {
        val opened = openPeerSealed(keys, peerCpk, aad = "482913", ivBase64 = peerIv, encBase64 = peerEnc, alg = "cbc-hmac")

        assertNotNull(opened)
        assertEquals("stbbaXHDfnzC", opened.token)
        assertEquals("ab12cd", opened.fingerprint)
    }

    @Test
    fun `refuses the iOS pair with another code as authenticated data`() {
        assertNull(openPeerSealed(keys, peerCpk, aad = "000000", ivBase64 = peerIv, encBase64 = peerEnc, alg = "cbc-hmac"))
    }

    @Test
    fun `refuses the iOS pair when one byte of the ciphertext changed`() {
        val bytes = Base64.getDecoder().decode(peerEnc).also { it[3] = (it[3].toInt() xor 1).toByte() }

        assertNull(openPeerSealed(keys, peerCpk, aad = "482913", ivBase64 = peerIv, encBase64 = Base64.getEncoder().encodeToString(bytes), alg = "cbc-hmac"))
    }

    @Test
    fun `refuses the iOS pair when read as AES-GCM`() {
        assertNull(openPeerSealed(keys, peerCpk, aad = "482913", ivBase64 = peerIv, encBase64 = peerEnc, alg = null))
    }

    @Test
    fun `opens an iOS QR flow pair sealed with the qr secret`() {
        val opened = openPairSecret("AAECAwQFBgcICQoLDA0ODw", code = "482913", ivBase64 = qrIv, encBase64 = qrEnc, alg = "cbc-hmac")

        assertNotNull(opened)
        assertEquals("stbbaXHDfnzC", opened.token)
        assertEquals("ab12cd", opened.fingerprint)
    }

    @Test
    fun `refuses the iOS QR pair with another secret`() {
        assertNull(openPairSecret("AQIDBAUGBwgJCgsMDQ4PEA", code = "482913", ivBase64 = qrIv, encBase64 = qrEnc, alg = "cbc-hmac"))
    }
}
