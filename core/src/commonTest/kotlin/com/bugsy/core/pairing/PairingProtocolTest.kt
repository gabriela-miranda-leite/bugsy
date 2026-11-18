package com.bugsy.core.pairing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PairingProtocolTest {
    @Test
    fun `message survives an encode and decode round trip`() {
        val message = PairingMessage(PairingMessage.PAIR, code = "482913", port = 7788, token = "abc", name = "Pixel", os = "Android 14")
        assertEquals(message, decodePairingMessage(message.encode()))
    }

    @Test
    fun `null fields are left out of the wire format`() {
        assertEquals("""{"type":"who"}""", PairingMessage(PairingMessage.WHO).encode().decodeToString())
    }

    @Test
    fun `garbage is not a message`() {
        assertNull(decodePairingMessage("not json".encodeToByteArray()))
    }

    @Test
    fun `unknown fields are ignored so newer peers stay compatible`() {
        val decoded = decodePairingMessage("""{"type":"device","port":7788,"futureField":1}""".encodeToByteArray())
        assertEquals(PairingMessage(PairingMessage.DEVICE, port = 7788), decoded)
    }

    @Test
    fun `qr text round trips`() {
        val qr = PairingQr(host = "192.168.0.10", port = 7790, code = "482913")
        assertEquals(qr, parsePairingQr(qr.toQrText()))
    }

    @Test
    fun `qr text with a foreign scheme is rejected`() {
        assertNull(parsePairingQr("https://example.com/?host=1.2.3.4&port=7790&code=482913"))
    }

    @Test
    fun `qr text with a bad code port or host is rejected`() {
        assertNull(parsePairingQr("bugsy://pair?host=1.2.3.4&port=7790&code=12345"))
        assertNull(parsePairingQr("bugsy://pair?host=1.2.3.4&port=7790&code=12a456"))
        assertNull(parsePairingQr("bugsy://pair?host=1.2.3.4&port=99999&code=482913"))
        assertNull(parsePairingQr("bugsy://pair?host=&port=7790&code=482913"))
        assertNull(parsePairingQr("bugsy://pair?port=7790&code=482913"))
    }
}
