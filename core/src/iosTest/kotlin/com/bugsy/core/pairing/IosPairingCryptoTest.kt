package com.bugsy.core.pairing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** The iOS sealing, run on the real iOS crypto stack. The desktop side opens what it prints (see the desktop tests). */
class IosPairingCryptoTest {
    private val desktopPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEFYixex2i4D1nAn5SxqeTQeScvF1NOny7lhJyXSYy0FN0RFbaG2mN5rlGQijOgDmDqR9wtgJ4c2YxwTqHcX6OIA=="
    private val plaintext = """{"token":"stbbaXHDfnzC","fp":"ab12cd"}""".encodeToByteArray()

    @Test
    fun printInteropVectors() {
        val peer = sealForPeer(decodeBase64(desktopPublicKey)!!, "482913".encodeToByteArray(), plaintext)
        assertNotNull(peer)
        println("VECTOR_PEER cpk=${peer.publicKey.toBase64()} iv=${peer.iv.toBase64()} enc=${peer.ciphertext.toBase64()} alg=${peer.alg}")

        val qr = sealPairingPayload(decodeUrlSafeBase64("AAECAwQFBgcICQoLDA0ODw")!!, "482913".encodeToByteArray(), plaintext)
        assertNotNull(qr)
        println("VECTOR_QR iv=${qr.iv.toBase64()} enc=${qr.ciphertext.toBase64()} alg=${qr.alg}")
    }

    @Test
    fun aSealIsDifferentEveryTime() {
        val a = sealPairingPayload(ByteArray(16) { it.toByte() }, "1".encodeToByteArray(), plaintext)!!
        val b = sealPairingPayload(ByteArray(16) { it.toByte() }, "1".encodeToByteArray(), plaintext)!!
        assertEquals(ALG_CBC_HMAC, a.alg)
        assertEquals(false, a.iv.contentEquals(b.iv))
        assertEquals(false, a.ciphertext.contentEquals(b.ciphertext))
    }

    @Test
    fun aKeyThatIsNotAP256PublicKeyIsRefused() {
        assertNull(sealForPeer(ByteArray(10), "1".encodeToByteArray(), plaintext))
        assertNull(sealForPeer(ByteArray(91), "1".encodeToByteArray(), plaintext))
    }
}
