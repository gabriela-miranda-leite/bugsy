package com.bugsy.core.ws

import com.bugsy.core.Bugsy
import com.bugsy.core.BugsyConfig
import com.bugsy.core.DebugRepository
import com.bugsy.core.MockEngine
import com.bugsy.core.model.DebugRequest
import com.bugsy.core.model.MockRule
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AndroidBugsyWsServerTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val token = "test-token"

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
    fun `sends a snapshot right after a valid handshake`() {
        DebugRepository.add(fakeRequest("1"))

        testApplication {
            application { bugsyModule(token) }
            val client = createClient { install(ClientWebSockets) }

            client.webSocket("/bugsy") {
                send(Frame.Text(json.encodeToString(HandshakeMessage.serializer(), HandshakeMessage(token))))

                val event = json.decodeFromString(BugsyEvent.serializer(), (incoming.receive() as Frame.Text).readText())
                val snapshot = assertIs<BugsyEvent.Snapshot>(event)
                assertEquals(1, snapshot.requests.size)
                assertEquals("1", snapshot.requests.first().id)
            }
        }
    }

    @Test
    fun `the snapshot carries the app's custom info`() {
        Bugsy.initialize(BugsyConfig(customInfoProvider = { listOf("user_id" to "u_42") }))
        try {
            testApplication {
                application { bugsyModule(token) }
                val client = createClient { install(ClientWebSockets) }

                client.webSocket("/bugsy") {
                    send(Frame.Text(json.encodeToString(HandshakeMessage.serializer(), HandshakeMessage(token))))

                    val event = json.decodeFromString(BugsyEvent.serializer(), (incoming.receive() as Frame.Text).readText())
                    val snapshot = assertIs<BugsyEvent.Snapshot>(event)
                    assertEquals(listOf(CustomInfoEntry("user_id", "u_42")), snapshot.customInfo)
                }
            }
        } finally {
            Bugsy.destroy()
        }
    }

    @Test
    fun `the app can keep its custom info off the desktop`() {
        Bugsy.initialize(BugsyConfig(customInfoProvider = { listOf("user_id" to "u_42") }, shareCustomInfoWithDesktop = false))
        try {
            assertEquals(emptyList(), currentBugsySnapshot().customInfo)
        } finally {
            Bugsy.destroy()
        }
    }

    @Test
    fun `closes the connection when the handshake token is wrong`() {
        testApplication {
            application { bugsyModule(token) }
            val client = createClient { install(ClientWebSockets) }

            client.webSocket("/bugsy") {
                send(Frame.Text(json.encodeToString(HandshakeMessage.serializer(), HandshakeMessage("wrong-token"))))
                val frame = incoming.receiveCatching().getOrNull()
                assertTrue(frame == null || frame is Frame.Close)
            }
        }
    }

    @Test
    fun `broadcasts request_recorded for a request added after connecting`() {
        testApplication {
            application { bugsyModule(token) }
            val client = createClient { install(ClientWebSockets) }

            client.webSocket("/bugsy") {
                send(Frame.Text(json.encodeToString(HandshakeMessage.serializer(), HandshakeMessage(token))))
                incoming.receive() // snapshot

                DebugRepository.add(fakeRequest("2"))

                val event = json.decodeFromString(BugsyEvent.serializer(), (incoming.receive() as Frame.Text).readText())
                val recorded = assertIs<BugsyEvent.RequestRecorded>(event)
                assertEquals("2", recorded.request.id)
            }
        }
    }

    @Test
    fun `applies an add_mock_rule command sent by the desktop client`() {
        testApplication {
            application { bugsyModule(token) }
            val client = createClient { install(ClientWebSockets) }

            client.webSocket("/bugsy") {
                send(Frame.Text(json.encodeToString(HandshakeMessage.serializer(), HandshakeMessage(token))))
                incoming.receive() // snapshot

                val rule = MockRule(id = "mock-1", urlPattern = "/v1/users")
                send(Frame.Text(json.encodeToString(BugsyCommand.serializer(), BugsyCommand.AddMockRule(rule))))

                val event = json.decodeFromString(BugsyEvent.serializer(), (incoming.receive() as Frame.Text).readText())
                val changed = assertIs<BugsyEvent.MockRuleChanged>(event)
                assertEquals("mock-1", changed.rule.id)
                assertEquals("added", changed.action)
            }
        }
        assertEquals(1, MockEngine.rulesFlow.value.size)
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
}
