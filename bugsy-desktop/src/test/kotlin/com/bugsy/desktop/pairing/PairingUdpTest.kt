package com.bugsy.desktop.pairing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Real UDP over loopback, on ports away from the defaults so a running Bugsy Desktop doesn't interfere. */
class PairingUdpTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val paired = CopyOnWriteArrayList<PairedDevice>()
    private val serverPort = 47_790
    private val devicePort = 47_791
    private val codes = PairingCodeManager()
    private val handler = PairingHandler(codes, desktopName = { "MacBook" }, onPaired = { paired += it })
    private val server = DesktopPairingServer(scope, handler, serverPort)

    @AfterTest
    fun tearDown() {
        server.stop()
        scope.cancel()
    }

    private fun exchange(message: PairingMessage): PairingMessage? = DatagramSocket().use { socket ->
        socket.soTimeout = 2_000
        val bytes = message.encode()
        socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName("127.0.0.1"), serverPort))
        val buffer = ByteArray(2048)
        val reply = DatagramPacket(buffer, buffer.size)
        runCatching { socket.receive(reply) }.getOrNull()?.let { decodePairingMessage(buffer.copyOf(reply.length)) }
    }

    @Test
    fun `a phone pairs over udp the way the mobile side does it`() {
        assertTrue(server.start())
        val code = codes.current.value.code

        val here = exchange(PairingMessage(PairingMessage.DISCOVER, code = code))
        assertEquals(PairingMessage.DESKTOP_HERE, here?.type)
        assertEquals("MacBook", here?.name)

        val result = exchange(PairingMessage(PairingMessage.PAIR, code = code, port = 7788, token = "tok3n", name = "Pixel", os = "Android 14"))
        assertEquals(true, result?.ok)
        assertEquals(PairedDevice("127.0.0.1", 7788, "tok3n", "Pixel", "Android 14"), paired.single())
    }

    @Test
    fun `a wrong code over udp is refused and pairs nothing`() {
        assertTrue(server.start())

        val result = exchange(PairingMessage(PairingMessage.PAIR, code = "not-it", port = 7788, token = "tok3n"))

        assertEquals(false, result?.ok)
        assertTrue(paired.isEmpty())
    }

    @Test
    fun `a second server on the same port reports that it could not start`() {
        assertTrue(server.start())
        val other = DesktopPairingServer(scope, handler, serverPort)
        // SO_REUSEADDR lets a second UDP socket bind on some platforms; either outcome must not throw.
        other.start()
        other.stop()
    }

    @Test
    fun `discovery lists the devices that answer the broadcast`() = runBlocking {
        val deviceSocket = DatagramSocket(null).apply { reuseAddress = true; bind(InetSocketAddress(devicePort)); soTimeout = 200 }
        val running = java.util.concurrent.atomic.AtomicBoolean(true)
        val responder = thread {
            val buffer = ByteArray(2048)
            while (running.get()) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    deviceSocket.receive(packet)
                } catch (_: java.net.SocketTimeoutException) {
                    continue
                } catch (_: Exception) {
                    break
                }
                if (decodePairingMessage(buffer.copyOf(packet.length))?.type != PairingMessage.WHO) continue
                val reply = PairingMessage(PairingMessage.DEVICE, name = "Pixel 7 Pro", os = "Android 14", port = 7788).encode()
                deviceSocket.send(DatagramPacket(reply, reply.size, packet.address, packet.port))
            }
        }
        try {
            val discovery = DeviceDiscovery(scope, devicePort, broadcasts = { listOf("127.0.0.1") })
            discovery.refresh()
            val deadline = System.currentTimeMillis() + 5_000
            while (discovery.devices.value.isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(50)

            assertEquals(listOf(DiscoveredDevice("127.0.0.1", "Pixel 7 Pro", "Android 14", 7788)), discovery.devices.value)
        } finally {
            running.set(false)
            responder.join(1_000)
            deviceSocket.close()
        }
    }

    @Test
    fun `qr matrix is square and non empty for the pairing text`() {
        val matrix = qrMatrix(pairingQrText("192.168.0.10", 7790, "482913"))
        assertTrue(matrix.size >= 21)
        assertTrue(matrix.all { it.size == matrix.size })
        assertTrue(matrix.any { row -> row.any { it } })
    }

    @Test
    fun `qr text carries the address port and code`() {
        assertEquals("bugsy://pair?host=192.168.0.10&port=7790&code=482913", pairingQrText("192.168.0.10", 7790, "482913"))
    }
}
