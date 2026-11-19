package com.bugsy.desktop.pairing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PairingHandlerTest {
    private var now = 1_000L
    private var counter = 0
    private val paired = mutableListOf<PairedDevice>()
    private val codes = PairingCodeManager(nowMs = { now }, nextCode = { (482_913 + counter++).toString() }, maxFailedAttempts = 3)
    private val handler = PairingHandler(codes, desktopName = { "MacBook" }, onPaired = { paired += it }, nowMs = { now })

    private fun pair(code: String? = "482913", nonce: String? = null, port: Int? = 7788, token: String? = "tok3n", from: String = "192.168.0.24") =
        handler.handle(PairingMessage(PairingMessage.PAIR, code = code, nonce = nonce, port = port, token = token, name = "Pixel 7 Pro", os = "Android 14"), from)

    @Test
    fun `discover with the right code is answered with the desktop's name`() {
        val reply = handler.handle(PairingMessage(PairingMessage.DISCOVER, code = "482913"), "192.168.0.24")
        assertEquals(PairingMessage.DESKTOP_HERE, reply?.type)
        assertEquals("MacBook", reply?.name)
        assertTrue(!reply?.pk.isNullOrEmpty(), "carries the one-time public key")
        assertTrue(paired.isEmpty())
    }

    @Test
    fun `discover with a wrong code is refused`() {
        val reply = handler.handle(PairingMessage(PairingMessage.DISCOVER, code = "000000"), "192.168.0.24")
        assertEquals(PairingMessage(PairingMessage.PAIR_RESULT, ok = false, reason = PairingMessage.REASON_INVALID), reply)
    }

    @Test
    fun `pairing with the shown code reports the phone and its token to the app`() {
        val reply = pair()

        assertEquals(PairingMessage(PairingMessage.PAIR_RESULT, ok = true), reply)
        assertEquals(listOf(PairedDevice("192.168.0.24", 7788, "tok3n", "Pixel 7 Pro", "Android 14")), paired)
    }

    @Test
    fun `a code works only once`() {
        pair()
        val second = pair()

        assertEquals(false, second?.ok)
        assertEquals(1, paired.size)
    }

    @Test
    fun `an expired code is refused with the expired reason`() {
        now += 60_000
        assertEquals(PairingMessage.REASON_EXPIRED, pair()?.reason)
        assertTrue(paired.isEmpty())
    }

    @Test
    fun `guessing is locked out`() {
        pair(code = "000001")
        pair(code = "000002")
        assertEquals(PairingMessage.REASON_LOCKED, pair(code = "000003")?.reason)
        // Even the real code is gone now: it was rotated, so the guesser gains nothing.
        assertEquals(false, pair(code = "482913")?.ok)
        assertTrue(paired.isEmpty())
    }

    @Test
    fun `a pair without a usable port or token is refused`() {
        assertEquals(false, pair(port = null)?.ok)
        assertEquals(false, pair(port = 0)?.ok)
        assertEquals(false, pair(port = 70_000)?.ok)
        assertEquals(false, pair(token = null)?.ok)
        assertEquals(false, pair(token = " ")?.ok)
        assertTrue(paired.isEmpty())
    }

    @Test
    fun `a nonce from the device the user picked pairs without a code`() {
        handler.expectNonce("n0nce", "192.168.0.24")

        val reply = pair(code = null, nonce = "n0nce")

        assertEquals(true, reply?.ok)
        assertEquals("tok3n", paired.single().token)
        // Approving from the list must not burn the code shown for scanning.
        assertEquals("482913", codes.current.value.code)
    }

    @Test
    fun `a nonce cannot be replayed`() {
        handler.expectNonce("n0nce", "192.168.0.24")
        pair(code = null, nonce = "n0nce")
        assertEquals(false, pair(code = null, nonce = "n0nce")?.ok)
        assertEquals(1, paired.size)
    }

    @Test
    fun `a nonce from another host is refused`() {
        handler.expectNonce("n0nce", "192.168.0.24")
        assertEquals(false, pair(code = null, nonce = "n0nce", from = "192.168.0.99")?.ok)
        assertTrue(paired.isEmpty())
    }

    @Test
    fun `a nonce nobody asked for is refused`() {
        assertEquals(false, pair(code = null, nonce = "made-up")?.ok)
    }

    @Test
    fun `an expired nonce is refused`() {
        handler.expectNonce("n0nce", "192.168.0.24", ttlMs = 5_000)
        now += 5_000
        assertEquals(false, pair(code = null, nonce = "n0nce")?.ok)
    }

    @Test
    fun `other message types get silence`() {
        assertNull(handler.handle(PairingMessage(PairingMessage.WHO), "192.168.0.24"))
        assertNull(handler.handle(PairingMessage("whatever"), "192.168.0.24"))
    }
}
