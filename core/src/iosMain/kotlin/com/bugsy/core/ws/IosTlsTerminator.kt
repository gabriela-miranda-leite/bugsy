@file:OptIn(ExperimentalForeignApi::class)

package com.bugsy.core.ws

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.plus
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toLong
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFArrayCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFTypeArrayCallBacks
import platform.Security.SSLClose
import platform.Security.SSLCreateContext
import platform.Security.SSLGetBufferedReadSize
import platform.Security.SSLHandshake
import platform.Security.SSLRead
import platform.Security.SSLSetCertificate
import platform.Security.SSLSetConnection
import platform.Security.SSLSetIOFuncs
import platform.Security.SSLSetProtocolVersionMin
import platform.Security.SSLWrite
import platform.Security.errSSLClosedAbort
import platform.Security.errSSLClosedGraceful
import platform.Security.errSSLWouldBlock
import platform.Security.SSLConnectionType
import platform.Security.SSLProtocolSide
import platform.Security.kTLSProtocol12
import platform.posix.AF_INET
import platform.posix.EAGAIN
import platform.posix.EWOULDBLOCK
import platform.posix.F_GETFL
import platform.posix.F_SETFL
import platform.posix.INADDR_ANY
import platform.posix.O_NONBLOCK
import platform.posix.POLLIN
import platform.posix.POLLOUT
import platform.posix.SHUT_RDWR
import platform.posix.SOCK_STREAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_REUSEADDR
import platform.posix.accept
import platform.posix.bind
import platform.posix.close
import platform.posix.connect
import platform.posix.errno
import platform.posix.fcntl
import platform.posix.getsockname
import platform.posix.listen
import platform.posix.poll
import platform.posix.pollfd
import platform.posix.recv
import platform.posix.send
import platform.posix.setsockopt
import platform.posix.shutdown
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import kotlin.concurrent.Volatile

internal const val IOS_INTERNAL_HEADER = "X-Bugsy-Internal"
internal const val IOS_PEER_HEADER = "X-Bugsy-Peer"
private const val MAX_HEAD_BYTES = 16 * 1024
private const val IO_TIMEOUT_MS = 10_000

/**
 * Puts TLS in front of the iOS BugsyWsServer (DEB-67), the same way [TlsTerminator] does on Android: it accepts TLS
 * on the public [port], decrypts with Apple's SecureTransport and relays to the plain server, which listens on
 * loopback only. SecureTransport speaks TLS 1.2 (the TLS 1.3 stack is Network.framework, which would mean a
 * different server), with a forward secret ECDHE-ECDSA suite for the P-256 certificate.
 */
internal class IosTlsTerminator(
    private val identity: IosTlsIdentity,
    private val port: Int,
    private val backendPort: Int,
    private val internalSecret: String,
) {
    @Volatile private var running = false
    private var listenFd = -1

    /** `false` when the public port could not be bound. */
    fun start(): Boolean {
        val fd = socket(AF_INET, SOCK_STREAM, 0)
        if (fd < 0) return false
        val bound = memScoped {
            val reuse = alloc<IntVar>().also { it.value = 1 }
            setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, reuse.ptr, sizeOf<IntVar>().convert())
            val address = alloc<sockaddr_in>()
            address.sin_family = AF_INET.convert()
            address.sin_port = port.toUShort().swapBytes()
            address.sin_addr.s_addr = INADDR_ANY
            bind(fd, address.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()) == 0 && listen(fd, 16) == 0
        }
        if (!bound) {
            close(fd)
            return false
        }
        listenFd = fd
        running = true
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.convert(), 0u)) { acceptLoop(fd) }
        return true
    }

    fun stop() {
        running = false
        if (listenFd >= 0) close(listenFd)
        listenFd = -1
    }

    private fun acceptLoop(fd: Int) {
        while (running) {
            val client = accept(fd, null, null)
            if (client < 0) {
                if (!running) break else continue
            }
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.convert(), 0u)) { serve(client) }
        }
    }

    private fun serve(client: Int) {
        setNonBlocking(client)
        val context = SSLCreateContext(null, SSLProtocolSide.kSSLServerSide, SSLConnectionType.kSSLStreamType)
        if (context == null) {
            close(client)
            return
        }
        try {
            SSLSetIOFuncs(context, readCallback, writeCallback)
            SSLSetConnection(context, client.toLong().toCPointer<kotlinx.cinterop.COpaque>())
            SSLSetProtocolVersionMin(context, kTLSProtocol12)
            memScoped {
                val values = allocArray<COpaquePointerVar>(1)
                values[0] = identity.identity
                val chain = CFArrayCreate(null, values, 1, kCFTypeArrayCallBacks.ptr)
                SSLSetCertificate(context, chain)
                CFRelease(chain)
            }
            if (!handshake(context, client)) return
            relay(context, client)
        } finally {
            SSLClose(context)
            CFRelease(context)
            shutdown(client, SHUT_RDWR)
            close(client)
        }
    }

    private fun handshake(context: platform.Security.SSLContextRef, client: Int): Boolean {
        while (true) {
            val status = SSLHandshake(context)
            when (status) {
                0 -> return true
                errSSLWouldBlock -> if (!waitFor(client, POLLIN.toInt(), IO_TIMEOUT_MS)) return false
                else -> return false
            }
        }
    }

    private fun relay(context: platform.Security.SSLContextRef, client: Int) {
        val backend = connectLoopback(backendPort)
        if (backend < 0) return
        try {
            val head = readRequestHead(context, client) ?: return
            val (rewritten, leftover) = rewriteHead(head, peerAddress = peerAddressOf(client))
            if (!sendAllBlocking(backend, rewritten) || (leftover.isNotEmpty() && !sendAllBlocking(backend, leftover))) return
            pump(context, client, backend)
        } finally {
            shutdown(backend, SHUT_RDWR)
            close(backend)
        }
    }

    /** Reads the HTTP upgrade request (up to its blank line) through TLS. */
    private fun readRequestHead(context: platform.Security.SSLContextRef, client: Int): ByteArray? {
        var all = ByteArray(0)
        while (all.size <= MAX_HEAD_BYTES) {
            if (indexOfHeadEnd(all) >= 0) return all
            val chunk = sslRead(context, client, 4096, IO_TIMEOUT_MS) ?: return null
            all += chunk
        }
        return null
    }

    /**
     * Drops any `X-Bugsy-*` header the client sent and adds the internal secret, so only this relay can reach the
     * server. Returns the rewritten request head and whatever followed its blank line.
     */
    private fun rewriteHead(all: ByteArray, peerAddress: String): Pair<ByteArray, ByteArray> {
        val end = indexOfHeadEnd(all)
        val lines = all.copyOfRange(0, end).decodeToString().split("\r\n")
        val kept = lines.filterIndexed { index, line -> index == 0 || !line.startsWith("x-bugsy-", ignoreCase = true) }
        val rewritten = (kept + "$IOS_INTERNAL_HEADER: $internalSecret" + "$IOS_PEER_HEADER: $peerAddress").joinToString("\r\n") + "\r\n\r\n"
        return rewritten.encodeToByteArray() to all.copyOfRange(end + 4, all.size)
    }

    private fun indexOfHeadEnd(bytes: ByteArray): Int {
        for (i in 0..bytes.size - 4) {
            if (bytes[i] == 13.toByte() && bytes[i + 1] == 10.toByte() && bytes[i + 2] == 13.toByte() && bytes[i + 3] == 10.toByte()) return i
        }
        return -1
    }

    /** One thread per connection: wait for either side, move what arrived to the other. */
    private fun pump(context: platform.Security.SSLContextRef, client: Int, backend: Int) {
        val buffer = ByteArray(8192)
        while (running) {
            if (!waitEither(client, backend, context)) return
            // Everything SecureTransport already decrypted counts as readable even if the socket has nothing new.
            while (true) {
                val chunk = sslRead(context, client, buffer.size, 0) ?: return // closed or failed
                if (chunk.isEmpty()) break
                if (!sendAllBlocking(backend, chunk)) return
            }
            val n = buffer.usePinned { recv(backend, it.addressOf(0), buffer.size.convert(), platform.posix.MSG_DONTWAIT) }
            if (n == 0L) return
            if (n > 0) {
                if (!sslWriteAll(context, buffer.copyOfRange(0, n.toInt()))) return
            }
        }
    }

    private fun waitEither(client: Int, backend: Int, context: platform.Security.SSLContextRef): Boolean {
        val buffered = memScoped {
            val size = alloc<platform.posix.size_tVar>()
            SSLGetBufferedReadSize(context, size.ptr)
            size.value.toLong()
        }
        if (buffered > 0) return true
        memScoped {
            val fds = allocArray<pollfd>(2)
            fds[0].fd = client
            fds[0].events = POLLIN.convert()
            fds[1].fd = backend
            fds[1].events = POLLIN.convert()
            val ready = poll(fds, 2u, 1000)
            return ready >= 0
        }
    }
}

/** Reads up to [max] decrypted bytes: `null` on close or error, an empty array when there is nothing to read now. */
private fun sslRead(context: platform.Security.SSLContextRef, client: Int, max: Int, waitMs: Int): ByteArray? {
    val deadline = waitMs
    while (true) {
        val buffer = ByteArray(max)
        var processed = 0L
        val status = memScoped {
            val count = alloc<platform.posix.size_tVar>()
            val result = buffer.usePinned { SSLRead(context, it.addressOf(0), max.convert(), count.ptr) }
            processed = count.value.toLong()
            result
        }
        if (processed > 0) return buffer.copyOfRange(0, processed.toInt())
        when (status) {
            0 -> continue
            errSSLWouldBlock -> {
                if (waitMs == 0) return ByteArray(0)
                if (!waitFor(client, POLLIN.toInt(), deadline)) return null
            }
            else -> return null
        }
    }
}

private fun sslWriteAll(context: platform.Security.SSLContextRef, data: ByteArray): Boolean {
    var offset = 0
    while (offset < data.size) {
        var written = 0L
        val status = memScoped {
            val count = alloc<platform.posix.size_tVar>()
            val result = data.usePinned { SSLWrite(context, it.addressOf(offset), (data.size - offset).convert(), count.ptr) }
            written = count.value.toLong()
            result
        }
        offset += written.toInt()
        if (status != 0 && status != errSSLWouldBlock) return false
    }
    return true
}

private fun setNonBlocking(fd: Int) {
    val flags = fcntl(fd, F_GETFL)
    fcntl(fd, F_SETFL, flags or O_NONBLOCK)
}

private fun waitFor(fd: Int, events: Int, timeoutMs: Int): Boolean = memScoped {
    val entry = alloc<pollfd>()
    entry.fd = fd
    entry.events = events.convert()
    poll(entry.ptr, 1u, timeoutMs) > 0
}

private fun connectLoopback(port: Int): Int {
    val fd = socket(AF_INET, SOCK_STREAM, 0)
    if (fd < 0) return -1
    val ok = memScoped {
        val address = alloc<sockaddr_in>()
        address.sin_family = AF_INET.convert()
        address.sin_port = port.toUShort().swapBytes()
        address.sin_addr.s_addr = 0x0100007Fu // 127.0.0.1, already in network order on a little-endian host
        connect(fd, address.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()) == 0
    }
    if (!ok) {
        close(fd)
        return -1
    }
    return fd
}

private fun sendAllBlocking(fd: Int, data: ByteArray): Boolean {
    var offset = 0
    while (offset < data.size) {
        val sent = data.usePinned { send(fd, it.addressOf(offset), (data.size - offset).convert(), 0) }
        if (sent <= 0) return false
        offset += sent.toInt()
    }
    return true
}

internal fun UShort.swapBytes(): UShort {
    val value = toInt()
    return (((value and 0xFF) shl 8) or ((value shr 8) and 0xFF)).toUShort()
}

/** SSLReadFunc: hands SecureTransport bytes from the socket; reports "would block" when it has fewer than asked. */
private val readCallback = staticCFunction { connection: platform.Security.SSLConnectionRef?, data: kotlinx.cinterop.COpaquePointer?, length: CPointer<platform.posix.size_tVar>? ->
    val fd = connection.toLong().toInt()
    val requested = length!!.pointed.value.toInt()
    val buffer = data!!.reinterpret<ByteVar>()
    var done = 0
    var status = 0
    while (done < requested) {
        val n = recv(fd, buffer + done, (requested - done).convert(), 0)
        if (n > 0) {
            done += n.toInt()
            continue
        }
        status = if (n == 0L) errSSLClosedGraceful else if (errno == EAGAIN || errno == EWOULDBLOCK) errSSLWouldBlock else errSSLClosedAbort
        break
    }
    length.pointed.value = done.convert()
    status
}

/** SSLWriteFunc: sends everything, waiting out a full socket buffer, so SecureTransport never sees a partial write. */
private val writeCallback = staticCFunction { connection: platform.Security.SSLConnectionRef?, data: kotlinx.cinterop.COpaquePointer?, length: CPointer<platform.posix.size_tVar>? ->
    val fd = connection.toLong().toInt()
    val total = length!!.pointed.value.toInt()
    val buffer = data!!.reinterpret<ByteVar>()
    var done = 0
    var status = 0
    while (done < total) {
        val n = send(fd, buffer + done, (total - done).convert(), 0)
        if (n > 0) {
            done += n.toInt()
            continue
        }
        if (n < 0 && (errno == EAGAIN || errno == EWOULDBLOCK)) {
            memScoped {
                val entry = alloc<pollfd>()
                entry.fd = fd
                entry.events = POLLOUT.convert()
                poll(entry.ptr, 1u, 5000)
            }
            continue
        }
        status = errSSLClosedAbort
        break
    }
    length.pointed.value = done.convert()
    status
}

/** The dotted IPv4 address on the other end of [fd], or "" when it cannot be read. */
private fun peerAddressOf(fd: Int): String = memScoped {
    val address = alloc<sockaddr_in>()
    val length = alloc<platform.posix.socklen_tVar>().also { it.value = sizeOf<sockaddr_in>().convert() }
    if (platform.posix.getpeername(fd, address.ptr.reinterpret(), length.ptr) != 0) return@memScoped ""
    val raw = address.sin_addr.s_addr.toLong()
    "${raw and 0xFF}.${(raw shr 8) and 0xFF}.${(raw shr 16) and 0xFF}.${(raw shr 24) and 0xFF}"
}
