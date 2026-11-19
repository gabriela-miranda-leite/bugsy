@file:OptIn(
    ExperimentalForeignApi::class,
    kotlin.io.encoding.ExperimentalEncodingApi::class,
    kotlinx.coroutines.DelicateCoroutinesApi::class,
)

package com.bugsy.core.ws

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile
import kotlin.io.encoding.Base64
import kotlinx.coroutines.runBlocking
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.posix.AF_INET
import platform.posix.INADDR_ANY
import platform.posix.SOCK_STREAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_REUSEADDR
import platform.posix.accept
import platform.posix.bind
import platform.posix.close
import platform.posix.listen
import platform.posix.recv
import platform.posix.send
import platform.posix.setsockopt
import platform.posix.sockaddr_in
import platform.posix.socket

private val json = Json { ignoreUnknownKeys = true }
private const val WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

/**
 * Noop-free: a real BugsyWsServer for iOS, speaking raw WebSocket over a POSIX socket. Ktor's `embeddedServer`
 * has no supported iOS target, so there's no off-the-shelf server to lean on here — this hand-rolls the RFC 6455
 * handshake and frame (un)masking on top of `platform.posix` sockets. See DEB-58.
 */
actual fun startBugsyWsServer(port: Int): BugsyWsServerHandle? = IosBugsyWsServer(port).also { it.start() }

internal class IosBugsyWsServer(private val port: Int) : BugsyWsServerHandle {
    override val sessionToken: String = generateBugsySessionToken()

    private val _connectedClients = MutableStateFlow(0)
    override val connectedClients: StateFlow<Int> = _connectedClients

    @Volatile private var running = false
    private var listenFd: Int = -1
    private var broadcastJob: Job? = null
    private val connections = ConnectionRegistry()

    fun start() {
        val fd = socket(AF_INET, SOCK_STREAM, 0)
        if (fd < 0) return
        listenFd = fd

        memScoped {
            val reuse = alloc<IntVar>()
            reuse.value = 1
            setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, reuse.ptr, sizeOf<IntVar>().convert())

            val address = alloc<sockaddr_in>()
            address.sin_family = AF_INET.convert()
            address.sin_port = port.toUShort().toNetworkByteOrder()
            address.sin_addr.s_addr = INADDR_ANY
            if (bind(fd, address.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()) != 0) {
                close(fd)
                listenFd = -1
                return
            }
        }

        if (listen(fd, 8) != 0) {
            close(fd)
            listenFd = -1
            return
        }

        running = true

        broadcastJob = GlobalScope.launch(Dispatchers.Default) {
            merge(
                com.bugsy.core.DebugRepository.added.map { BugsyEvent.RequestRecorded(it) },
                com.bugsy.core.MockEngine.ruleChanges.map { BugsyEvent.MockRuleChanged(it.rule, it.action.name.lowercase()) },
                com.bugsy.core.MockEngine.oneShotConsumedEvents.map { BugsyEvent.MockRuleConsumed(it) },
                com.bugsy.core.Bugsy.flagManager.flagChanges.map { BugsyEvent.FlagChanged(it) },
            ).collect { event -> connections.broadcast(encodeFrame(OPCODE_TEXT, encodeEvent(event.withoutSecrets()))) }
        }

        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.convert(), 0u)) {
            acceptLoop(fd)
        }
    }

    override fun stop() {
        running = false
        broadcastJob?.cancel()
        if (listenFd >= 0) close(listenFd)
        listenFd = -1
        connections.closeAll()
        _connectedClients.value = 0
    }

    private fun acceptLoop(fd: Int) {
        while (running) {
            val clientFd = accept(fd, null, null)
            if (clientFd < 0) {
                if (!running) break else continue
            }
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.convert(), 0u)) {
                handleConnection(clientFd)
            }
        }
    }

    private fun handleConnection(fd: Int) {
        val reader = SocketReader(fd)
        val headers = reader.readHttpHeaders() ?: run { close(fd); return }
        val key = parseHeaderValue(headers, "Sec-WebSocket-Key") ?: run { close(fd); return }
        val accept = Base64.encode(Sha1.hash((key + WS_GUID).encodeToByteArray()))
        val response = "HTTP/1.1 101 Switching Protocols\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Accept: $accept\r\n\r\n"
        if (!sendAll(fd, response.encodeToByteArray())) run { close(fd); return }

        val handshakePayload = readIncomingFrame(fd, reader) ?: run { close(fd); return }
        val handshake = runCatching { json.decodeFromString(HandshakeMessage.serializer(), handshakePayload.decodeToString()) }.getOrNull()
        if (handshake?.value == null || !constantTimeEquals(handshake.value, sessionToken)) run { close(fd); return }

        // Registered before the snapshot is sent, or a change right after it could be broadcast (missing this
        // connection) before this connection is registered to receive broadcasts at all (DEB-58).
        connections.add(fd)
        _connectedClients.update { it + 1 }
        if (!sendAll(fd, encodeFrame(OPCODE_TEXT, encodeEvent(currentBugsySnapshot().withoutSecrets())))) run {
            connections.remove(fd)
            _connectedClients.update { it - 1 }
            close(fd)
            return
        }

        while (true) {
            val payload = readIncomingFrame(fd, reader) ?: break
            val command = runCatching { json.decodeFromString(BugsyCommand.serializer(), payload.decodeToString()) }.getOrNull()
            if (command != null) applyBugsyCommand(command)
        }

        connections.remove(fd)
        _connectedClients.update { it - 1 }
        close(fd)
    }
}

private fun encodeEvent(event: BugsyEvent): ByteArray = json.encodeToString(BugsyEvent.serializer(), event).encodeToByteArray()

/** iOS (ARM) is little-endian; network byte order is big-endian — swap the two bytes of the port. */
private fun UShort.toNetworkByteOrder(): UShort {
    val value = toInt()
    return (((value and 0xFF) shl 8) or ((value shr 8) and 0xFF)).toUShort()
}

private fun parseHeaderValue(headers: String, name: String): String? =
    headers.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.startsWith("$name:", ignoreCase = true) }
        ?.substringAfter(":")
        ?.trim()

private const val OPCODE_TEXT = 0x1
private const val OPCODE_CLOSE = 0x8
private const val OPCODE_PING = 0x9
private const val OPCODE_PONG = 0xA

/** RFC 6455 server->client frame: never masked. */
private fun encodeFrame(opcode: Int, payload: ByteArray): ByteArray {
    val header = mutableListOf<Byte>()
    header.add((0x80 or opcode).toByte())
    val len = payload.size
    when {
        len < 126 -> header.add(len.toByte())
        len < 65536 -> {
            header.add(126.toByte())
            header.add(((len shr 8) and 0xFF).toByte())
            header.add((len and 0xFF).toByte())
        }
        else -> {
            header.add(127.toByte())
            for (i in 7 downTo 0) header.add(((len.toLong() shr (i * 8)) and 0xFF).toByte())
        }
    }
    return header.toByteArray() + payload
}

/** Reads one RFC 6455 client->server frame (always masked), replying to pings and skipping pongs until it sees
 * a text payload or the connection closes/errors. */
private fun readIncomingFrame(fd: Int, reader: SocketReader): ByteArray? {
    while (true) {
        val first2 = reader.readExactly(2) ?: return null
        val opcode = first2[0].toInt() and 0x0F
        val masked = (first2[1].toInt() and 0x80) != 0
        var len = (first2[1].toInt() and 0x7F).toLong()
        if (len == 126L) {
            val ext = reader.readExactly(2) ?: return null
            len = ((ext[0].toInt() and 0xFF).toLong() shl 8) or (ext[1].toInt() and 0xFF).toLong()
        } else if (len == 127L) {
            val ext = reader.readExactly(8) ?: return null
            len = 0
            for (b in ext) len = (len shl 8) or (b.toLong() and 0xFF)
        }
        val maskKey = if (masked) reader.readExactly(4) ?: return null else null
        val payload = if (len > 0) reader.readExactly(len.toInt()) ?: return null else ByteArray(0)
        if (maskKey != null) {
            for (i in payload.indices) payload[i] = (payload[i].toInt() xor maskKey[i % 4].toInt()).toByte()
        }
        when (opcode) {
            OPCODE_TEXT -> return payload
            OPCODE_CLOSE -> return null
            OPCODE_PING -> if (!sendAll(fd, encodeFrame(OPCODE_PONG, payload))) return null
            OPCODE_PONG -> Unit
            else -> Unit
        }
    }
}

private fun sendAll(fd: Int, data: ByteArray): Boolean {
    if (data.isEmpty()) return true
    var offset = 0
    while (offset < data.size) {
        val sent = data.usePinned { pinned -> send(fd, pinned.addressOf(offset), (data.size - offset).convert(), 0) }
        if (sent <= 0) return false
        offset += sent.toInt()
    }
    return true
}

/** Accumulates bytes off a blocking socket, letting callers read either "until CRLFCRLF" (HTTP headers) or an
 * exact byte count (WS frame fields), carrying over whatever came in past what was asked for. */
private class SocketReader(private val fd: Int) {
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
            if (data[i] == CR && data[i + 1] == LF && data[i + 2] == CR && data[i + 3] == LF) return i
        }
        return -1
    }

    private companion object {
        const val CR: Byte = 13
        const val LF: Byte = 10
    }
}

/** Connections currently past the handshake, broadcast to from the single [IosBugsyWsServer.broadcastJob]. */
private class ConnectionRegistry {
    private val mutex = Mutex()
    private var fds = listOf<Int>()

    fun add(fd: Int) {
        runBlocking { mutex.withLock { fds = fds + fd } }
    }

    fun remove(fd: Int) {
        runBlocking { mutex.withLock { fds = fds - fd } }
    }

    fun broadcast(frame: ByteArray) {
        val snapshot = runBlocking { mutex.withLock { fds } }
        val dead = snapshot.filterNot { sendAll(it, frame) }
        if (dead.isNotEmpty()) runBlocking { mutex.withLock { fds = fds - dead.toSet() } }
    }

    fun closeAll() {
        val snapshot = runBlocking {
            mutex.withLock {
                val current = fds
                fds = emptyList()
                current
            }
        }
        snapshot.forEach { close(it) }
    }
}
