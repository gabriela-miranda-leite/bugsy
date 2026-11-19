package com.bugsy.core.pairing

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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

    // 16 bytes, base64url, as the desktop's QR carries it. Computed, not written out: it is a test value, not a credential.
    private val qrSecret = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(16) { it.toByte() })

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

        assertFailsWith<javax.crypto.AEADBadTagException> { openSealed(pair, secret = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(16) { (it + 1).toByte() })) }
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

    // --- DEB-67: code and list flows seal the token to the desktop's one-time ECDH key ---

    private val desktopKeys = java.security.KeyPairGenerator.getInstance("EC")
        .apply { initialize(java.security.spec.ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    private val desktopPk = java.util.Base64.getEncoder().encodeToString(desktopKeys.public.encoded)

    /** What the desktop does with a sealed `pair`: ECDH with the phone's key, then AES-GCM with [aad]. */
    private fun openForDesktop(message: PairingMessage, aad: String): String {
        val phone = java.security.KeyFactory.getInstance("EC").generatePublic(
            java.security.spec.X509EncodedKeySpec(java.util.Base64.getDecoder().decode(message.cpk)),
        )
        val shared = javax.crypto.KeyAgreement.getInstance("ECDH").apply {
            init(desktopKeys.private)
            doPhase(phone, true)
        }.generateSecret()
        val digest = java.security.MessageDigest.getInstance("SHA-256").apply { update("bugsy-pair-v2".encodeToByteArray()) }
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            javax.crypto.Cipher.DECRYPT_MODE,
            javax.crypto.spec.SecretKeySpec(digest.digest(shared), "AES"),
            javax.crypto.spec.GCMParameterSpec(128, java.util.Base64.getDecoder().decode(message.iv)),
        )
        cipher.updateAAD(aad.encodeToByteArray())
        return cipher.doFinal(java.util.Base64.getDecoder().decode(message.enc)).decodeToString()
    }

    /** Runs [block] while a person compares the short codes: waits for the prompt, then answers [match]. */
    private suspend fun pairConfirming(service: PairingService, code: String, match: Boolean): PairingResult = coroutineScope {
        val result = async { service.pairWithCode(code) }
        val deadline = System.currentTimeMillis() + 5_000
        while (service.pendingVerification.value == null && System.currentTimeMillis() < deadline) delay(10)
        service.confirmVerification(match)
        result.await()
    }

    private val answeringWithKey = { message: PairingMessage, _: String ->
        when (message.type) {
            PairingMessage.DISCOVER -> listOf(PairingMessage(PairingMessage.DESKTOP_HERE, name = "MacBook", pk = desktopPk))
            PairingMessage.PAIR -> listOf(PairingMessage(PairingMessage.PAIR_RESULT, ok = true))
            else -> emptyList()
        }
    }

    @Test
    fun `pairing by code seals the token to the key the desktop answered with`() = runBlocking {
        val result = pairConfirming(newService(answeringWithKey, fingerprint = "ab12cd"), "482913", match = true)

        assertEquals(PairingResult.Success("MacBook"), result)
        val pair = sent.first { it.third.type == PairingMessage.PAIR }.third
        assertNull(pair.token, "the token must not travel in clear")
        assertNull(pair.fp)
        val opened = openForDesktop(pair, aad = "482913")
        assertTrue("tok3n" in opened && "ab12cd" in opened, opened)
        assertTrue(sent.none { "tok3n" in it.third.toString() })
    }

    @Test
    fun `a sealed code flow pair cannot be opened with the wrong code as authenticated data`() = runBlocking {
        pairConfirming(newService(answeringWithKey), "482913", match = true)
        val pair = sent.first { it.third.type == PairingMessage.PAIR }.third

        assertFailsWith<javax.crypto.AEADBadTagException> { openForDesktop(pair, aad = "000000") }
        Unit
    }

    @Test
    fun `approving a connect request seals the token to the key the request carried`() = runBlocking {
        val service = newService(fingerprint = "ab12cd")
        service.startResponder()
        val request = PairingMessage(PairingMessage.CONNECT_REQUEST, nonce = "n0nce", name = "MacBook", port = PAIRING_DESKTOP_UDP_PORT, pk = desktopPk)
        transports.single().inbox.add(UdpPacket(request.encode(), "10.0.0.5", 50000))
        awaitPending(service)

        service.respondToConnectRequest(allow = true)

        val pair = awaitSent { it.third.type == PairingMessage.PAIR }.third
        assertNull(pair.token)
        assertEquals("n0nce", pair.nonce)
        val opened = openForDesktop(pair, aad = "n0nce")
        assertTrue("tok3n" in opened && "ab12cd" in opened, opened)
    }

    @Test
    fun `an older desktop that sends no key still gets the token in clear`() = runBlocking {
        newService().pairWithCode("482913")

        assertEquals("tok3n", sent.first { it.third.type == PairingMessage.PAIR }.third.token)
    }

    // --- DEB-67: nothing secret is sent before the person confirms the short codes match ---

    @Test
    fun `the phone sends its key first and no token until the person confirms the code`() = runBlocking {
        val service = newService(answeringWithKey)
        val result = async(kotlinx.coroutines.Dispatchers.Default) { service.pairWithCode("482913") }
        val deadline = System.currentTimeMillis() + 5_000
        while (service.pendingVerification.value == null && System.currentTimeMillis() < deadline) delay(10)

        val prompt = service.pendingVerification.value
        assertEquals("MacBook", prompt?.desktopName)
        assertEquals(4, prompt?.code?.length)
        val kex = sent.first { it.third.type == PairingMessage.KEX }.third
        assertEquals("482913", kex.code)
        assertTrue(!kex.cpk.isNullOrEmpty())
        assertTrue(sent.none { it.third.type == PairingMessage.PAIR }, "no pair before the person confirms")

        service.confirmVerification(true)
        assertEquals(PairingResult.Success("MacBook"), result.await())
        // The code the phone showed is the one the desktop would compute from the same two keys.
        assertEquals(prompt?.code, verificationCode(java.util.Base64.getDecoder().decode(desktopPk), java.util.Base64.getDecoder().decode(kex.cpk), "482913"))
    }

    @Test
    fun `saying the codes differ stops the pairing and nothing secret is sent`() = runBlocking {
        val result = pairConfirming(newService(answeringWithKey), "482913", match = false)

        assertEquals(PairingResult.NotVerified, result)
        assertTrue(sent.none { it.third.type == PairingMessage.PAIR }, "no pair after a mismatch")
        assertTrue(sent.none { "tok3n" in it.third.toString() })
    }

    @Test
    fun `not answering in time counts as not verified`() = runBlocking {
        val service = PairingService(
            wsPort = 7788,
            tokenProvider = { "tok3n" },
            openTransport = { FakeTransport(answeringWithKey, sent).also { transports += it } },
            broadcasts = { listOf("255.255.255.255") },
            deviceName = { "Pixel 7 Pro" },
            osName = { "Android 14" },
            discoverWaitMs = 150,
            pairWaitMs = 300,
            verificationWaitMs = 200,
        ).also { this@PairingServiceTest.service = it }

        assertEquals(PairingResult.NotVerified, service.pairWithCode("482913"))
        assertNull(service.pendingVerification.value)
        assertTrue(sent.none { it.third.type == PairingMessage.PAIR })
    }

    @Test
    fun `a connect request with a key sends kex right away and carries the code to compare`() = runBlocking {
        val service = newService()
        service.startResponder()
        val request = PairingMessage(PairingMessage.CONNECT_REQUEST, nonce = "n0nce", name = "MacBook", port = PAIRING_DESKTOP_UDP_PORT, pk = desktopPk)
        transports.single().inbox.add(UdpPacket(request.encode(), "10.0.0.5", 50000))

        val pending = awaitPending(service)
        val kex = awaitSent { it.third.type == PairingMessage.KEX }
        assertEquals("n0nce", kex.third.nonce)
        assertEquals("10.0.0.5", kex.first)
        assertEquals(PAIRING_DESKTOP_UDP_PORT, kex.second)
        assertEquals(verificationCode(java.util.Base64.getDecoder().decode(desktopPk), java.util.Base64.getDecoder().decode(kex.third.cpk), "n0nce"), pending.verificationCode)
        assertTrue(sent.none { it.third.type == PairingMessage.PAIR }, "no pair before the person allows")
    }

    @Test
    fun `a connect request from an older desktop has no code and sends no kex`() = runBlocking {
        val service = newService()
        service.startResponder()
        val request = PairingMessage(PairingMessage.CONNECT_REQUEST, nonce = "n0nce", name = "MacBook", port = PAIRING_DESKTOP_UDP_PORT)
        transports.single().inbox.add(UdpPacket(request.encode(), "10.0.0.5", 50000))

        assertNull(awaitPending(service).verificationCode)
        assertTrue(sent.none { it.third.type == PairingMessage.KEX })
    }

    @Test
    fun `the verification code is the same on both sides and changes with any input`() {
        val desktop = ByteArray(91) { it.toByte() }
        val phone = ByteArray(91) { (it + 1).toByte() }

        val code = verificationCode(desktop, phone, "482913")
        assertEquals(code, verificationCode(desktop, phone, "482913"))
        assertEquals("4942", code, "fixed vector: the desktop computes the same value")
        assertTrue(code != verificationCode(desktop, phone, "482914"))
        assertTrue(code != verificationCode(phone, desktop, "482913"))
    }
}
