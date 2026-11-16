package com.bugsy.core.ws

import kotlin.test.Test
import kotlin.test.assertEquals

class Sha1Test {

    @Test
    fun `hashes the empty string`() {
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", Sha1.hash(ByteArray(0)).toHex())
    }

    @Test
    fun `hashes a short ascii string`() {
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", Sha1.hash("abc".encodeToByteArray()).toHex())
    }

    @Test
    fun `hashes a string longer than one 64-byte block`() {
        assertEquals(
            "84983e441c3bd26ebaae4aa1f95129e5e54670f1",
            Sha1.hash("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray()).toHex(),
        )
    }

    @Test
    fun `matches the RFC 6455 handshake example`() {
        // RFC 6455 section 1.3's own example: Sec-WebSocket-Key "dGhlIHNhbXBsZSBub25jZQ==" + the WebSocket GUID.
        val input = "dGhlIHNhbXBsZSBub25jZQ==258EAFA5-E914-47DA-95CA-C5AB0DC85B11".encodeToByteArray()
        assertEquals("b37a4f2cc0624f1690f64606cf385945b2bec4ea", Sha1.hash(input).toHex())
    }

    private fun ByteArray.toHex(): String = joinToString("") { byte -> ((byte.toInt() and 0xFF) + 0x100).toString(16).substring(1) }
}
