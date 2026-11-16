@file:OptIn(ExperimentalForeignApi::class)

package com.bugsy.core.ws

import com.bugsy.core.Bugsy
import com.bugsy.core.DebugRepository
import com.bugsy.core.MockEngine
import com.bugsy.core.model.DebugRequest
import com.bugsy.core.model.MockRule
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.serialization.json.Json
import platform.posix.AF_INET
import platform.posix.SOCK_STREAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_RCVTIMEO
import platform.posix.close
import platform.posix.connect
import platform.posix.recv
import platform.posix.send
import platform.posix.setsockopt
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.posix.timeval
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.fail

/**
 * Exercises the real [IosBugsyWsServer] end to end over an actual loopback TCP socket (the iOS simulator this
 * runs in supports real local networking) — not just a compile check. A raw client here hand-rolls the RFC 6455
 * handshake/framing the same way a desktop client would, since there's no Ktor WS client available to borrow
 * for iosTest either. See DEB-58.
 */
private const val LOOPBACK_ADDRESS: UInt = 0x7F000001u // 127.0.0.1

class IosBugsyWsServerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setUp() {
        DebugRepository.clear()
        MockEngine.clear()
        Bugsy.initialize()
    }

    @AfterTest
    fun tearDown() {
        Bugsy.destroy()
        DebugRepository.clear()
        MockEngine.clear()
    }

    @Test
    fun `handshakes then sends a snapshot and a request_recorded event and applies a command`() {
        val port = (20000..40000).random()
        val handle = startBugsyWsServer(port) ?: fail("startBugsyWsServer returned null on iOS")
        try {
            val fd = connectLoopback(port)
            try {
                performHttpUpgrade(fd)
                sendMaskedText(fd, json.encodeToString(HandshakeMessage.serializer(), HandshakeMessage(handle.sessionToken)))

                val snapshotEvent = json.decodeFromString(BugsyEvent.serializer(), receiveText(fd))
                assertIs<BugsyEvent.Snapshot>(snapshotEvent)

                DebugRepository.add(fakeRequest("1"))
                val recordedEvent = json.decodeFromString(BugsyEvent.serializer(), receiveText(fd))
                val recorded = assertIs<BugsyEvent.RequestRecorded>(recordedEvent)
                assertEquals("1", recorded.request.id)

                val rule = MockRule(id = "mock-1", urlPattern = "/v1/users")
                sendMaskedText(fd, json.encodeToString(BugsyCommand.serializer(), BugsyCommand.AddMockRule(rule)))
                val ruleEvent = json.decodeFromString(BugsyEvent.serializer(), receiveText(fd))
                val ruleChanged = assertIs<BugsyEvent.MockRuleChanged>(ruleEvent)
                assertEquals("mock-1", ruleChanged.rule.id)
                assertEquals("added", ruleChanged.action)
            } finally {
                close(fd)
            }
        } finally {
            handle.stop()
        }
        assertEquals(1, MockEngine.rulesFlow.value.size)
    }

    @Test
    fun `rejects a handshake with the wrong token`() {
        val port = (20000..40000).random()
        val handle = startBugsyWsServer(port) ?: fail("startBugsyWsServer returned null on iOS")
        try {
            val fd = connectLoopback(port)
            try {
                performHttpUpgrade(fd)
                sendMaskedText(fd, json.encodeToString(HandshakeMessage.serializer(), HandshakeMessage("wrong-token")))
                // The server closes the socket without sending a snapshot; recv should see EOF (0) or an error.
                val buffer = ByteArray(16)
                val n = buffer.usePinned { pinned -> recv(fd, pinned.addressOf(0), buffer.size.convert(), 0) }
                assertEquals(true, n <= 0)
            } finally {
                close(fd)
            }
        } finally {
            handle.stop()
        }
    }

    private fun fakeRequest(id: String) = DebugRequest(
        id = id,
        url = "https://api.bugsy.dev/v1/users",
        method = "GET",
        requestHeaders = emptyMap(),
        requestBody = null,
        statusCode = 200,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 10,
        timestamp = 0,
        curlCommand = "curl ...",
    )

    private fun connectLoopback(port: Int): Int {
        val fd = socket(AF_INET, SOCK_STREAM, 0)
        check(fd >= 0) { "socket() failed" }
        memScoped {
            // Give the blocking recv() calls below a bound so a protocol bug fails the test instead of hanging it.
            val timeout = alloc<timeval>()
            timeout.tv_sec = 5
            timeout.tv_usec = 0
            setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, timeout.ptr, sizeOf<timeval>().convert())

            val address = alloc<sockaddr_in>()
            address.sin_family = AF_INET.convert()
            address.sin_port = port.toUShort().toNetworkByteOrderForTest()
            address.sin_addr.s_addr = LOOPBACK_ADDRESS.toNetworkByteOrderForTest32()
            check(connect(fd, address.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()) == 0) { "connect() failed" }
        }
        return fd
    }

    private fun performHttpUpgrade(fd: Int) {
        val request = "GET /bugsy HTTP/1.1\r\n" +
            "Host: localhost\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" + // gitleaks:allow -- RFC 6455's own public example key, not a secret
            "Sec-WebSocket-Version: 13\r\n\r\n"
        sendAllTest(fd, request.encodeToByteArray())

        val reader = TestSocketReader(fd)
        val headers = reader.readHttpHeaders() ?: fail("no HTTP response from server")
        check(headers.startsWith("HTTP/1.1 101")) { "expected 101 Switching Protocols, got: $headers" }
        leftoverReaders[fd] = reader
    }

    private fun sendMaskedText(fd: Int, text: String) {
        val payload = text.encodeToByteArray()
        val mask = byteArrayOf(0x12, 0x34, 0x56, 0x78)
        val masked = ByteArray(payload.size) { i -> (payload[i].toInt() xor mask[i % 4].toInt()).toByte() }
        val header = mutableListOf<Byte>()
        header.add((0x80 or 0x1).toByte())
        val len = masked.size
        when {
            len < 126 -> header.add((0x80 or len).toByte())
            else -> {
                header.add((0x80 or 126).toByte())
                header.add(((len shr 8) and 0xFF).toByte())
                header.add((len and 0xFF).toByte())
            }
        }
        header.addAll(mask.toList())
        sendAllTest(fd, header.toByteArray() + masked)
    }

    private fun receiveText(fd: Int): String {
        val reader = leftoverReaders.getValue(fd)
        val first2 = reader.readExactly(2) ?: fail("connection closed while waiting for a frame")
        val opcode = first2[0].toInt() and 0x0F
        var len = (first2[1].toInt() and 0x7F).toLong()
        if (len == 126L) {
            val ext = reader.readExactly(2) ?: fail("connection closed mid-frame")
            len = ((ext[0].toInt() and 0xFF).toLong() shl 8) or (ext[1].toInt() and 0xFF).toLong()
        }
        val payload = if (len > 0) reader.readExactly(len.toInt()) ?: fail("connection closed mid-payload") else ByteArray(0)
        assertEquals(0x1, opcode, "expected a text frame")
        return payload.decodeToString()
    }

    private val leftoverReaders = mutableMapOf<Int, TestSocketReader>()

    private fun UShort.toNetworkByteOrderForTest(): UShort {
        val value = toInt()
        return (((value and 0xFF) shl 8) or ((value shr 8) and 0xFF)).toUShort()
    }

    /** iOS (ARM) is little-endian; network byte order is big-endian — reverse the 4 bytes of the address. */
    private fun UInt.toNetworkByteOrderForTest32(): UInt =
        ((this and 0xFFu) shl 24) or (((this shr 8) and 0xFFu) shl 16) or (((this shr 16) and 0xFFu) shl 8) or ((this shr 24) and 0xFFu)

    private fun sendAllTest(fd: Int, data: ByteArray) {
        var offset = 0
        while (offset < data.size) {
            val sent = data.usePinned { pinned -> send(fd, pinned.addressOf(offset), (data.size - offset).convert(), 0) }
            check(sent > 0) { "send() failed" }
            offset += sent.toInt()
        }
    }

    /** Same accumulate-then-read-exact approach as the production `SocketReader`, duplicated here since that one is private. */
    private class TestSocketReader(private val fd: Int) {
        private var buffer = ByteArray(0)

        fun readHttpHeaders(maxBytes: Int = 8192): String? {
            while (true) {
                val index = indexOfHeaderTerminator(buffer)
                if (index >= 0) {
                    val header = buffer.copyOfRange(0, index).decodeToString()
                    buffer = buffer.copyOfRange(index + 4, buffer.size)
                    return header
                }
                if (buffer.size > maxBytes) return null
                buffer += (recvChunk() ?: return null)
            }
        }

        fun readExactly(n: Int): ByteArray? {
            while (buffer.size < n) {
                buffer += (recvChunk() ?: return null)
            }
            val result = buffer.copyOfRange(0, n)
            buffer = buffer.copyOfRange(n, buffer.size)
            return result
        }

        private fun recvChunk(): ByteArray? {
            val tmp = ByteArray(4096)
            val n = tmp.usePinned { pinned -> recv(fd, pinned.addressOf(0), tmp.size.convert(), 0) }
            return if (n <= 0) null else tmp.copyOfRange(0, n.toInt())
        }

        private fun indexOfHeaderTerminator(data: ByteArray): Int {
            if (data.size < 4) return -1
            for (i in 0..data.size - 4) {
                if (data[i] == 13.toByte() && data[i + 1] == 10.toByte() && data[i + 2] == 13.toByte() && data[i + 3] == 10.toByte()) return i
            }
            return -1
        }
    }
}
