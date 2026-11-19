package com.bugsy.core.pairing

import kotlinx.coroutines.runBlocking
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private typealias Sent = Triple<String, Int, PairingMessage>

/** An in-memory UDP: whatever the service sends is handed to [desktop], whose replies land in the sender's queue. */
private class FakeTransport(
    private val desktop: (PairingMessage, String) -> List<PairingMessage>,
    private val sent: MutableList<Sent>,
) : UdpTransport {
    val inbox = LinkedBlockingQueue<UdpPacket>()

    @Volatile var closed = false

    override fun send(host: String, port: Int, payload: ByteArray): Boolean {
        val message = decodePairingMessage(payload) ?: return false
        synchronized(sent) { sent += Triple(host, port, message) }
        desktop(message, host).forEach { inbox.add(UdpPacket(it.encode(), "10.0.0.5", PAIRING_DESKTOP_UDP_PORT)) }
        return true
    }

    override fun receive(timeoutMs: Int): UdpPacket? = if (closed) null else inbox.poll(timeoutMs.toLong(), TimeUnit.MILLISECONDS)

    override fun close() {
        closed = true
    }
}

class PairingServiceTest {
    private val sent = mutableListOf<Sent>()
    private val transports = mutableListOf<FakeTransport>()
    private var service: PairingService? = null

    /** A desktop showing code 482913: answers `discover` and `pair` the way the real one does. */
    private fun desktop(codeStatus: String = "valid"): (PairingMessage, String) -> List<PairingMessage> = { message, _ ->
        when {
            message.type == PairingMessage.DISCOVER && message.code != "482913" ->
                listOf(PairingMessage(PairingMessage.PAIR_RESULT, ok = false, reason = PairingMessage.REASON_INVALID))
            message.type == PairingMessage.DISCOVER && codeStatus == "expired" ->
                listOf(PairingMessage(PairingMessage.PAIR_RESULT, ok = false, reason = PairingMessage.REASON_EXPIRED))
            message.type == PairingMessage.DISCOVER -> listOf(PairingMessage(PairingMessage.DESKTOP_HERE, name = "MacBook"))
            message.type == PairingMessage.PAIR && message.code == "482913" -> listOf(PairingMessage(PairingMessage.PAIR_RESULT, ok = true))
            message.type == PairingMessage.PAIR ->
                listOf(PairingMessage(PairingMessage.PAIR_RESULT, ok = false, reason = PairingMessage.REASON_INVALID))
            else -> emptyList()
        }
    }

    private fun newService(
        desktop: (PairingMessage, String) -> List<PairingMessage> = desktop(),
        token: String? = "tok3n",
        fingerprint: String? = null,
    ): PairingService = PairingService(
        wsPort = 7788,
        tokenProvider = { token },
        fingerprintProvider = { fingerprint },
        openTransport = { FakeTransport(desktop, sent).also { transports += it } },
        broadcasts = { listOf("255.255.255.255") },
        deviceName = { "Pixel 7 Pro" },
        osName = { "Android 14" },
        discoverWaitMs = 150,
        pairWaitMs = 300,
    ).also { service = it }

    @AfterTest
    fun tearDown() {
        service?.stop()
    }

    @Test
    fun `pairing by code finds the desktop by broadcast and hands it the token`() = runBlocking {
        val result = newService().pairWithCode("482913")

        assertEquals(PairingResult.Success("MacBook"), result)
        val discover = sent.first { it.third.type == PairingMessage.DISCOVER }
        assertEquals("255.255.255.255", discover.first)
        assertEquals(PAIRING_DESKTOP_UDP_PORT, discover.second)
        // The token must only ever travel in a unicast `pair`, never in the broadcast.
        assertNull(discover.third.token)
        val pair = sent.first { it.third.type == PairingMessage.PAIR }
        assertEquals("10.0.0.5", pair.first)
        assertEquals("tok3n", pair.third.token)
        assertEquals(7788, pair.third.port)
        assertEquals("Pixel 7 Pro", pair.third.name)
        assertEquals("Android 14", pair.third.os)
    }

    @Test
    fun `a wrong code is reported as invalid and no token is sent`() = runBlocking {
        val result = newService().pairWithCode("000000")

        assertEquals(PairingResult.InvalidCode, result)
        assertTrue(sent.none { it.third.token != null })
    }

    @Test
    fun `an expired code is reported as expired`() = runBlocking {
        assertEquals(PairingResult.ExpiredCode, newService(desktop("expired")).pairWithCode("482913"))
    }

    @Test
    fun `a locked desktop is reported as too many attempts`() = runBlocking {
        val locked = { _: PairingMessage, _: String ->
            listOf(PairingMessage(PairingMessage.PAIR_RESULT, ok = false, reason = PairingMessage.REASON_LOCKED))
        }
        assertEquals(PairingResult.TooManyAttempts, newService(locked).pairWithCode("482913"))
    }

    @Test
    fun `silence from the network means the desktop was not found`() = runBlocking {
        assertEquals(PairingResult.DesktopNotFound, newService(desktop = { _, _ -> emptyList() }).pairWithCode("482913"))
    }

    @Test
    fun `a malformed code never touches the network`() = runBlocking {
        assertEquals(PairingResult.InvalidCode, newService().pairWithCode("12ab"))
        assertEquals(PairingResult.InvalidCode, newService().pairWithCode("12345"))
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `pairing by qr skips the broadcast and talks straight to the address in the qr`() = runBlocking {
        val result = newService().pairWithQr(PairingQr("192.168.0.10", PAIRING_DESKTOP_UDP_PORT, "482913").toQrText())

        assertEquals(PairingResult.Success(null), result)
        assertTrue(sent.none { it.third.type == PairingMessage.DISCOVER })
        val pair = sent.single { it.third.type == PairingMessage.PAIR }
        assertEquals("192.168.0.10", pair.first)
        assertEquals(PAIRING_DESKTOP_UDP_PORT, pair.second)
    }

    @Test
    fun `a qr that is not from bugsy is rejected`() = runBlocking {
        assertEquals(PairingResult.InvalidQr, newService().pairWithQr("https://example.com"))
    }

    @Test
    fun `pairing needs the ws server to be running`() = runBlocking {
        assertEquals(PairingResult.ServerUnavailable, newService(token = null).pairWithCode("482913"))
        assertEquals(PairingResult.ServerUnavailable, newService(token = null).pairWithQr(PairingQr("1.2.3.4", 7790, "482913").toQrText()))
    }

    @Test
    fun `the responder answers who with this device's name os and ws port`() {
        val service = newService()
        service.startResponder()
        val responder = transports.single()

        responder.inbox.add(UdpPacket(PairingMessage(PairingMessage.WHO).encode(), "10.0.0.5", 50000))

        val reply = awaitSent { it.third.type == PairingMessage.DEVICE }
        assertEquals("10.0.0.5", reply.first)
        assertEquals(50000, reply.second)
        assertEquals(PairingMessage(PairingMessage.DEVICE, name = "Pixel 7 Pro", os = "Android 14", port = 7788), reply.third)
    }

    @Test
    fun `approving a connect request sends the token with the desktop's nonce`() = runBlocking {
        val service = newService()
        service.startResponder()
        val request = PairingMessage(PairingMessage.CONNECT_REQUEST, nonce = "n0nce", name = "MacBook", port = PAIRING_DESKTOP_UDP_PORT)
        transports.single().inbox.add(UdpPacket(request.encode(), "10.0.0.5", 50000))

        val pending = awaitPending(service)
        assertEquals(ConnectRequest("MacBook", "10.0.0.5", PAIRING_DESKTOP_UDP_PORT, "n0nce"), pending)

        service.respondToConnectRequest(allow = true)

        assertNull(service.pendingConnectRequest.value)
        val pair = awaitSent { it.third.type == PairingMessage.PAIR }
        assertEquals("10.0.0.5", pair.first)
        assertEquals(PAIRING_DESKTOP_UDP_PORT, pair.second)
        assertEquals("n0nce", pair.third.nonce)
        assertEquals("tok3n", pair.third.token)
    }

    @Test
    fun `denying a connect request sends nothing`() = runBlocking {
        val service = newService()
        service.startResponder()
        val request = PairingMessage(PairingMessage.CONNECT_REQUEST, nonce = "n0nce", name = "MacBook")
        transports.single().inbox.add(UdpPacket(request.encode(), "10.0.0.5", 50000))
        awaitPending(service)

        service.respondToConnectRequest(allow = false)

        assertNull(service.pendingConnectRequest.value)
        assertTrue(sent.none { it.third.type == PairingMessage.PAIR })
    }

    private fun awaitPending(service: PairingService): ConnectRequest {
        val deadline = System.currentTimeMillis() + 2_000
        while (System.currentTimeMillis() < deadline) {
            service.pendingConnectRequest.value?.let { return it }
            Thread.sleep(10)
        }
        throw AssertionError("no connect request arrived")
    }

    private fun awaitSent(predicate: (Sent) -> Boolean): Sent {
        val deadline = System.currentTimeMillis() + 2_000
        while (System.currentTimeMillis() < deadline) {
            synchronized(sent) { sent.firstOrNull(predicate) }?.let { return it }
            Thread.sleep(10)
        }
        throw AssertionError("expected datagram was never sent; sent=$sent")
    }

    // --- DEB-67: nothing secret in clear on the QR flow, and the fingerprint to pin on every flow ---

    private val qrSecret = "AAECAwQFBgcICQoLDA0ODw" // 16 bytes, base64url, as the desktop's QR carries it

    private fun openSealed(message: PairingMessage, secret: String = qrSecret): String {
        val key = deriveAesKey(java.util.Base64.getUrlDecoder().decode(secret))
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            javax.crypto.Cipher.DECRYPT_MODE,
            key,
            javax.crypto.spec.GCMParameterSpec(128, java.util.Base64.getDecoder().decode(message.iv)),
        )
        cipher.updateAAD(message.code!!.encodeToByteArray())
        return cipher.doFinal(java.util.Base64.getDecoder().decode(message.enc)).decodeToString()
    }

    @Test
    fun `pairing by a qr with a secret seals the token and the fingerprint`() = runBlocking {
        val result = newService(fingerprint = "ab12cd").pairWithQr("bugsy://pair?host=10.0.0.5&port=7790&code=482913&key=$qrSecret")

        assertEquals(PairingResult.Success(null), result)
        val pair = sent.first { it.third.type == PairingMessage.PAIR }.third
        assertNull(pair.token, "the token must not travel in clear")
        assertNull(pair.fp, "the fingerprint must not travel in clear")
        assertEquals("482913", pair.code)
        val opened = openSealed(pair)
        assertTrue("tok3n" in opened && "ab12cd" in opened, opened)
        assertTrue(sent.none { "tok3n" in (it.third.toString()) }, "no datagram may contain the token in clear")
    }

    @Test
    fun `a sealed pair cannot be opened with another secret`() = runBlocking {
        newService().pairWithQr("bugsy://pair?host=10.0.0.5&port=7790&code=482913&key=$qrSecret")
        val pair = sent.first { it.third.type == PairingMessage.PAIR }.third

        assertFailsWith<javax.crypto.AEADBadTagException> { openSealed(pair, secret = "AQIDBAUGBwgJCgsMDQ4PEA") }
        Unit
    }

    @Test
    fun `a qr without a secret (older desktop) still pairs in clear`() = runBlocking {
        newService(fingerprint = "ab12cd").pairWithQr("bugsy://pair?host=10.0.0.5&port=7790&code=482913")

        val pair = sent.first { it.third.type == PairingMessage.PAIR }.third
        assertEquals("tok3n", pair.token)
        assertEquals("ab12cd", pair.fp)
        assertNull(pair.enc)
    }

    @Test
    fun `pairing by code sends the fingerprint so the desktop can pin it`() = runBlocking {
        newService(fingerprint = "ab12cd").pairWithCode("482913")

        assertEquals("ab12cd", sent.first { it.third.type == PairingMessage.PAIR }.third.fp)
    }

    @Test
    fun `the qr round trips its secret`() {
        val qr = parsePairingQr("bugsy://pair?host=10.0.0.5&port=7790&code=482913&key=$qrSecret")

        assertEquals(PairingQr("10.0.0.5", 7790, "482913", qrSecret), qr)
        assertEquals("bugsy://pair?host=10.0.0.5&port=7790&code=482913&key=$qrSecret", qr!!.toQrText())
    }
}
