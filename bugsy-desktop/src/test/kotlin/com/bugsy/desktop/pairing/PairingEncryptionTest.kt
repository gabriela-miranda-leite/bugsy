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

    // --- code and list flows: sealed to a one-time ECDH key (DEB-67) ---

    /** What `:core` does on the phone, written independently here. */
    private fun sealToDesktop(desktopPublicKey: String, aad: String, json: String): PairingMessage {
        val own = newEcdhKeyPair()
        val peer = java.security.KeyFactory.getInstance("EC").generatePublic(
            java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(desktopPublicKey)),
        )
        val shared = javax.crypto.KeyAgreement.getInstance("ECDH").apply {
            init(own.private)
            doPhase(peer, true)
        }.generateSecret()
        val digest = MessageDigest.getInstance("SHA-256").apply { update("bugsy-pair-v2".encodeToByteArray()) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(digest.digest(shared), "AES"), GCMParameterSpec(128, iv))
            updateAAD(aad.encodeToByteArray())
        }
        return PairingMessage(
            PairingMessage.PAIR, port = 7788, name = "Pixel", os = "Android 15",
            cpk = own.publicKeyBase64(), iv = Base64.getEncoder().encodeToString(iv),
            enc = Base64.getEncoder().encodeToString(cipher.doFinal(json.encodeToByteArray())),
        )
    }

    /** What the phone sends first in the code and list flows: its one-time key, before any token. */
    private fun kexFor(sealed: PairingMessage, code: String? = null, nonce: String? = null) =
        PairingMessage(PairingMessage.KEX, code = code, nonce = nonce, cpk = sealed.cpk)

    @Test
    fun `discover answers with the one-time public key`() {
        val reply = handler.handle(PairingMessage(PairingMessage.DISCOVER, code = "482913"), "192.168.0.24")

        assertEquals(codes.current.value.publicKey, reply?.pk)
        assertTrue(!reply?.pk.isNullOrEmpty())
    }

    @Test
    fun `the code flow opens a pair sealed to the key it answered with`() {
        val sealed = sealToDesktop(codes.current.value.publicKey, aad = "482913", json = """{"token":"tok3n","fp":"ab12"}""").copy(code = "482913")
        handler.handle(kexFor(sealed, code = "482913"), "192.168.0.24")
        val reply = handler.handle(sealed, "192.168.0.24")

        assertEquals(true, reply?.ok)
        assertEquals(PairedDevice("192.168.0.24", 7788, "tok3n", "Pixel", "Android 15", fingerprint = "ab12"), paired.single())
    }

    @Test
    fun `the code flow refuses a pair sealed with another code as authenticated data`() {
        val sealed = sealToDesktop(codes.current.value.publicKey, aad = "000000", json = """{"token":"tok3n"}""").copy(code = "482913")
        handler.handle(kexFor(sealed, code = "482913"), "192.168.0.24")

        assertEquals(false, handler.handle(sealed, "192.168.0.24")?.ok)
        assertTrue(paired.isEmpty())
    }

    @Test
    fun `the code flow refuses a pair sealed to a key that is not the one on screen`() {
        val stale = newEcdhKeyPair().publicKeyBase64()
        val sealed = sealToDesktop(stale, aad = "482913", json = """{"token":"tok3n"}""").copy(code = "482913")
        handler.handle(kexFor(sealed, code = "482913"), "192.168.0.24")

        assertEquals(false, handler.handle(sealed, "192.168.0.24")?.ok)
        assertTrue(paired.isEmpty())
    }

    @Test
    fun `the list flow opens a pair sealed to the key its request carried, once`() {
        val publicKey = handler.expectNonce("n0nce", "192.168.0.24")
        val sealed = sealToDesktop(publicKey, aad = "n0nce", json = """{"token":"tok3n","fp":"cd34"}""").copy(nonce = "n0nce")
        handler.handle(kexFor(sealed, nonce = "n0nce"), "192.168.0.24")

        assertEquals(true, handler.handle(sealed, "192.168.0.24")?.ok)
        assertEquals("cd34", paired.single().fingerprint)
        // The nonce is gone: the same datagram, replayed, gets nothing.
        assertEquals(false, handler.handle(sealed, "192.168.0.24")?.ok)
        assertEquals(1, paired.size)
    }

    @Test
    fun `the list flow refuses a sealed pair from another address`() {
        val publicKey = handler.expectNonce("n0nce", "192.168.0.24")
        val sealed = sealToDesktop(publicKey, aad = "n0nce", json = """{"token":"tok3n"}""").copy(nonce = "n0nce")
        handler.handle(kexFor(sealed, nonce = "n0nce"), "192.168.0.99")

        assertEquals(false, handler.handle(sealed, "192.168.0.99")?.ok)
        assertTrue(paired.isEmpty())
    }

    // --- the short verification code (DEB-67) ---

    @Test
    fun `a kex shows the verification code the phone derives from the same two keys`() {
        val sealed = sealToDesktop(codes.current.value.publicKey, aad = "482913", json = """{"token":"tok3n"}""")

        assertNull(handler.handle(kexFor(sealed, code = "482913"), "192.168.0.24"), "the phone does not wait on a reply")

        val shown = handler.verification.value
        assertNotNull(shown)
        val expected = verificationCode(
            Base64.getDecoder().decode(codes.current.value.publicKey),
            Base64.getDecoder().decode(sealed.cpk),
            "482913",
        )
        assertEquals(expected, shown.code)
        assertEquals(4, shown.code.length)
    }

    @Test
    fun `a kex with a wrong code shows nothing`() {
        val sealed = sealToDesktop(codes.current.value.publicKey, aad = "000000", json = """{"token":"tok3n"}""")
        handler.handle(kexFor(sealed, code = "000000"), "192.168.0.24")

        assertNull(handler.verification.value)
    }

    @Test
    fun `a sealed pair with no kex before it is refused`() {
        val sealed = sealToDesktop(codes.current.value.publicKey, aad = "482913", json = """{"token":"tok3n"}""").copy(code = "482913")

        assertEquals(false, handler.handle(sealed, "192.168.0.24")?.ok)
        assertTrue(paired.isEmpty())
    }

    @Test
    fun `a sealed pair that uses another key than the kex is refused`() {
        val first = sealToDesktop(codes.current.value.publicKey, aad = "482913", json = """{"token":"tok3n"}""")
        val other = sealToDesktop(codes.current.value.publicKey, aad = "482913", json = """{"token":"evil"}""").copy(code = "482913")
        handler.handle(kexFor(first, code = "482913"), "192.168.0.24")

        assertEquals(false, handler.handle(other, "192.168.0.24")?.ok)
        assertTrue(paired.isEmpty())
    }

    @Test
    fun `the code goes away once the phone is paired`() {
        val sealed = sealToDesktop(codes.current.value.publicKey, aad = "482913", json = """{"token":"tok3n"}""").copy(code = "482913")
        handler.handle(kexFor(sealed, code = "482913"), "192.168.0.24")
        assertNotNull(handler.verification.value)

        handler.handle(sealed, "192.168.0.24")

        assertNull(handler.verification.value)
    }

    @Test
    fun `the list flow shows the code for the nonce and only to the address that asked`() {
        val publicKey = handler.expectNonce("n0nce", "192.168.0.24")
        val sealed = sealToDesktop(publicKey, aad = "n0nce", json = """{"token":"tok3n"}""")

        handler.handle(kexFor(sealed, nonce = "n0nce"), "192.168.0.99")
        assertNull(handler.verification.value, "another address cannot make this desktop show a code")

        handler.handle(kexFor(sealed, nonce = "n0nce"), "192.168.0.24")
        assertEquals(
            verificationCode(Base64.getDecoder().decode(publicKey), Base64.getDecoder().decode(sealed.cpk), "n0nce"),
            handler.verification.value?.code,
        )
    }

    @Test
    fun `the verification code matches the fixed vector the phone side asserts`() {
        val code = verificationCode(ByteArray(91) { it.toByte() }, ByteArray(91) { (it + 1).toByte() }, "482913")

        assertEquals("4942", code)
    }
}
