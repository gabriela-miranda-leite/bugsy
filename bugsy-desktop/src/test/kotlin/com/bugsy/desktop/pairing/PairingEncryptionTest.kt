package com.bugsy.desktop.pairing

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The QR flow's sealed `pair` (DEB-67): the token and certificate fingerprint never travel in clear. */
class PairingEncryptionTest {
    private val paired = mutableListOf<PairedDevice>()
    private val secret = randomSecret()
    private var codeCount = 0
    private val codes = PairingCodeManager(nextCode = { if (codeCount++ == 0) "482913" else "99991$codeCount" }, nextSecret = { secret })
    private val handler = PairingHandler(codes, desktopName = { "MacBook" }, onPaired = { paired += it })

    /** What `:core` does on the phone, written independently here so a mismatch between the two shows up. */
    private fun seal(secretBase64Url: String, code: String, json: String): Pair<String, String> {
        val digest = MessageDigest.getInstance("SHA-256").apply { update("bugsy-pair-v1".encodeToByteArray()) }
        val key = SecretKeySpec(digest.digest(Base64.getUrlDecoder().decode(secretBase64Url)), "AES")
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            updateAAD(code.encodeToByteArray())
        }
        val enc = cipher.doFinal(json.encodeToByteArray())
        return Base64.getEncoder().encodeToString(iv) to Base64.getEncoder().encodeToString(enc)
    }

    private fun sealedPair(secretToUse: String = secret, aadCode: String = "482913", code: String = "482913", json: String = """{"token":"tok3n","fp":"ab12"}"""): PairingMessage? {
        val (iv, enc) = seal(secretToUse, aadCode, json)
        return handler.handle(PairingMessage(PairingMessage.PAIR, code = code, port = 7788, name = "Pixel", os = "Android 15", iv = iv, enc = enc), "192.168.0.24")
    }

    @Test
    fun `a sealed pair is opened and reports the token and the fingerprint`() {
        val reply = sealedPair()

        assertEquals(PairingMessage(PairingMessage.PAIR_RESULT, ok = true), reply)
        assertEquals(listOf(PairedDevice("192.168.0.24", 7788, "tok3n", "Pixel", "Android 15", fingerprint = "ab12")), paired)
    }

    @Test
    fun `the qr text carries the secret`() {
        assertTrue(pairingQrText("192.168.0.10", 7790, "482913", secret).endsWith("&key=$secret"))
    }

    @Test
    fun `a pair sealed with another secret is refused`() {
        val reply = sealedPair(secretToUse = randomSecret())

        assertEquals(false, reply?.ok)
        assertTrue(paired.isEmpty())
    }

    @Test
    fun `a sealed pair bound to another code is refused`() {
        val reply = sealedPair(aadCode = "000000")

        assertEquals(false, reply?.ok)
        assertTrue(paired.isEmpty())
    }

    @Test
    fun `a flipped ciphertext byte is refused`() {
        val (iv, enc) = seal(secret, "482913", """{"token":"tok3n"}""")
        val bytes = Base64.getDecoder().decode(enc).also { it[0] = (it[0].toInt() xor 1).toByte() }
        val reply = handler.handle(
            PairingMessage(PairingMessage.PAIR, code = "482913", port = 7788, iv = iv, enc = Base64.getEncoder().encodeToString(bytes)),
            "192.168.0.24",
        )

        assertEquals(false, reply?.ok)
        assertTrue(paired.isEmpty())
    }

    @Test
    fun `a failed open does not burn the code`() {
        sealedPair(secretToUse = randomSecret())
        val reply = sealedPair()

        assertEquals(true, reply?.ok)
    }

    @Test
    fun `a replay of a sealed pair after the code was used is refused`() {
        val (iv, enc) = seal(secret, "482913", """{"token":"tok3n"}""")
        val message = PairingMessage(PairingMessage.PAIR, code = "482913", port = 7788, iv = iv, enc = enc)
        assertEquals(true, handler.handle(message, "192.168.0.24")?.ok)

        // The code rotated (single use), so the same datagram a sniffer captured is now worthless.
        assertEquals(false, handler.handle(message, "192.168.0.99")?.ok)
        assertEquals(1, paired.size)
    }

    @Test
    fun `a plain pair still works for the code and list flows and keeps the fingerprint`() {
        val reply = handler.handle(
            PairingMessage(PairingMessage.PAIR, code = "482913", port = 7788, token = "tok3n", fp = "cd34"),
            "192.168.0.24",
        )

        assertEquals(true, reply?.ok)
        assertEquals("cd34", paired.single().fingerprint)
    }

    @Test
    fun `secretFor only answers for the code on screen`() {
        assertNotNull(codes.secretFor("482913"))
        assertNull(codes.secretFor("000000"))
    }
}
