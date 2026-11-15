package com.bugsy.core

import com.bugsy.core.model.MockRule
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OkHttpDebugInterceptorTest {

    private val server = MockWebServer()
    private val interceptor = OkHttpDebugInterceptor()

    @BeforeTest
    fun setUp() {
        BugsyContext.debuggableOverride = true
        server.start()
        DebugRepository.clear()
    }

    @AfterTest
    fun tearDown() {
        HeaderRedactor.reset()
        BodyFieldMasker.reset()
        BugsyContext.debuggableOverride = null
        CaptureSettings.configure(allowRevealingSecrets = true)
        MockEngine.clear()
        server.shutdown()
    }

    @Test
    fun `captures request and response with headers and body masked, without altering the response`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Authorization", "Bearer response-secret")
                .setBody("""{"token":"xyz","ok":true}""")
        )

        interceptor.attach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val requestBody = """{"username":"gabi","password":"1234"}""".toRequestBody()
        val request = Request.Builder()
            .url(server.url("/login"))
            .header("Authorization", "Bearer request-secret")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBodyString = response.body!!.string()

        assertEquals(200, response.code)
        assertEquals("""{"token":"xyz","ok":true}""", responseBodyString)

        val captured = DebugRepository.getAll()
        assertEquals(1, captured.size)

        val entry = captured.first()
        assertEquals("***", entry.requestHeaders["Authorization"])
        assertEquals("***", entry.responseHeaders["Authorization"])
        assertTrue(entry.requestBody!!.contains("\"password\":\"***\""))
        assertTrue(entry.responseBody!!.contains("\"token\":\"***\""))
        assertEquals(200, entry.statusCode)
    }

    @Test
    fun `does not capture when detached`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        interceptor.detach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val request = Request.Builder().url(server.url("/ping")).build()
        client.newCall(request).execute().close()

        assertTrue(DebugRepository.getAll().isEmpty())
    }

    @Test
    fun `redacts custom headers added through Bugsy in the captured request and response`() {
        Bugsy.addSensitiveHeader("X-Custom-Token")
        server.enqueue(MockResponse().setResponseCode(200).setHeader("X-Custom-Token", "resp-secret").setBody("{}"))

        interceptor.attach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        client.newCall(
            Request.Builder().url(server.url("/")).header("X-Custom-Token", "req-secret").build()
        ).execute().close()

        val entry = DebugRepository.getAll().single()
        assertEquals("***", entry.requestHeaders["X-Custom-Token"])
        assertEquals("***", entry.responseHeaders["X-Custom-Token"])
        assertTrue("req-secret" !in entry.curlCommand)
    }

    @Test
    fun `masks sensitive fields of a response body that is cut at the capture limit`() {
        val padding = "x".repeat(1_100_000)
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"password":"leaked-secret","blob":"$padding"}"""))

        interceptor.attach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        val captured = DebugRepository.getAll().single().responseBody!!
        assertTrue("leaked-secret" !in captured)
    }

    @Test
    fun `a client built with addBugsyInterceptor is captured once Bugsy is initialized`() {
        Bugsy.initialize()
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val client = OkHttpClient.Builder().addBugsyInterceptor().build()

        client.newCall(Request.Builder().url(server.url("/ping")).build()).execute().close()
        Bugsy.destroy()

        val entry = DebugRepository.getAll().single()
        assertEquals(200, entry.statusCode)
        assertTrue(entry.url.endsWith("/ping"))
    }

    @Test
    fun `a client built with addBugsyInterceptor captures nothing after destroy`() {
        Bugsy.initialize()
        Bugsy.destroy()
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val client = OkHttpClient.Builder().addBugsyInterceptor().build()

        client.newCall(Request.Builder().url(server.url("/ping")).build()).execute().close()

        assertEquals(0, DebugRepository.getAll().size)
    }

    @Test
    fun `keeps the real values in memory next to the masked ones`() {
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Set-Cookie", "session=abc").setBody("""{"token":"resp-token"}"""))
        interceptor.attach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        client.newCall(
            Request.Builder().url(server.url("/login")).header("Authorization", "Bearer real-token")
                .post("""{"password":"p4ss"}""".toRequestBody()).build()
        ).execute().close()

        val entry = DebugRepository.getAll().single()
        assertTrue("real-token" !in entry.curlCommand && "p4ss" !in entry.curlCommand)
        assertEquals("***", entry.requestHeaders["Authorization"])
        assertEquals("""{"token":"***"}""", entry.responseBody)
        val secrets = entry.secrets!!
        assertEquals("Bearer real-token", secrets.requestHeaders["Authorization"])
        assertEquals("""{"token":"resp-token"}""", secrets.responseBody)
        assertTrue("p4ss" in secrets.curlCommand)
        assertTrue("real-token" !in entry.toString())
    }

    @Test
    fun `does not keep the real values when revealing is disabled`() {
        CaptureSettings.configure(allowRevealingSecrets = false)
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        interceptor.attach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        client.newCall(Request.Builder().url(server.url("/")).header("Authorization", "Bearer real-token").build()).execute().close()

        assertEquals(null, DebugRepository.getAll().single().secrets)
    }

    @Test
    fun `addBugsyInterceptor adds nothing when the host app is not debuggable`() {
        BugsyContext.debuggableOverride = false

        val client = OkHttpClient.Builder().addBugsyInterceptor().build()

        assertEquals(0, client.interceptors.size)
    }

    @Test
    fun `addBugsyInterceptor adds the interceptor when the host app is debuggable`() {
        BugsyContext.debuggableOverride = true

        val client = OkHttpClient.Builder().addBugsyInterceptor().build()

        assertEquals(1, client.interceptors.size)
    }

    @Test
    fun `the host is not debuggable when there is no application context`() {
        BugsyContext.debuggableOverride = null
        BugsyContext.applicationContext = null

        assertTrue(!BugsyContext.isHostDebuggable())
    }

    @Test
    fun `captures a simple GET request with the correct method, URL and status`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        interceptor.attach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        client.newCall(Request.Builder().url(server.url("/ping")).build()).execute().close()

        val entry = DebugRepository.getAll().single()
        assertEquals("GET", entry.method)
        assertTrue(entry.url.endsWith("/ping"))
        assertEquals(200, entry.statusCode)
    }

    @Test
    fun `redacts the Cookie header in the captured request`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        interceptor.attach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        client.newCall(
            Request.Builder().url(server.url("/")).header("Cookie", "session=real-session-id").build()
        ).execute().close()

        val entry = DebugRepository.getAll().single()
        assertEquals("***", entry.requestHeaders["Cookie"])
    }

    @Test
    fun `a network failure does not crash the interceptor and captures an entry with an error status`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        interceptor.attach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val request = Request.Builder().url(server.url("/down")).build()

        assertFailsWith<IOException> { client.newCall(request).execute() }

        val entry = DebugRepository.getAll().single()
        assertEquals("GET", entry.method)
        assertTrue(entry.url.endsWith("/down"))
        assertEquals(null, entry.statusCode)
    }

    @Test
    fun `10 concurrent requests through the interceptor do not throw ConcurrentModificationException`() {
        repeat(10) { server.enqueue(MockResponse().setResponseCode(200).setBody("{}")) }

        interceptor.attach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val executor = Executors.newFixedThreadPool(10)

        val calls = (0 until 10).map {
            executor.submit {
                client.newCall(Request.Builder().url(server.url("/concurrent")).build()).execute().close()
            }
        }
        calls.forEach { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        assertEquals(10, DebugRepository.getAll().size)
    }

    @Test
    fun `ring buffer with capacity 2 discards the oldest entry on the 3rd real request`() {
        DebugRepository.configureMaxEntries(2)
        repeat(3) { server.enqueue(MockResponse().setResponseCode(200).setBody("{}")) }

        interceptor.attach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        repeat(3) { index ->
            client.newCall(Request.Builder().url(server.url("/req-$index")).build()).execute().close()
        }

        val captured = DebugRepository.getAll()
        assertEquals(2, captured.size)
        assertTrue(captured.none { it.url.endsWith("/req-0") })
        assertTrue(captured.any { it.url.endsWith("/req-1") })
        assertTrue(captured.any { it.url.endsWith("/req-2") })
        DebugRepository.configureMaxEntries(200)
    }

    @Test
    fun `a matching mock rule answers without hitting the real server, and is captured as mocked`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/mocked", statusCode = 401, responseBody = """{"error":"nope"}"""))

        interceptor.attach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val response = client.newCall(Request.Builder().url(server.url("/mocked")).build()).execute()

        assertEquals(401, response.code)
        assertEquals("""{"error":"nope"}""", response.body!!.string())
        assertEquals(0, server.requestCount)

        val captured = DebugRepository.getAll().single()
        assertEquals(401, captured.statusCode)
        assertEquals(MockMetadata.VALUE_TRUE, captured.metadata[MockMetadata.KEY_MOCKED])
        assertEquals(null, captured.metadata[MockMetadata.KEY_ONE_SHOT])
    }

    @Test
    fun `a one-shot mock rule fires once, then falls through to the real network`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/once", statusCode = 200, responseBody = "{}", isOneShot = true))
        server.enqueue(MockResponse().setResponseCode(204))

        interceptor.attach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val first = client.newCall(Request.Builder().url(server.url("/once")).build()).execute()
        val second = client.newCall(Request.Builder().url(server.url("/once")).build()).execute()

        assertEquals(200, first.code)
        assertEquals(204, second.code)
        assertEquals(1, server.requestCount)

        val entries = DebugRepository.getAll()
        assertEquals(MockMetadata.VALUE_TRUE, entries.single { it.statusCode == 200 }.metadata[MockMetadata.KEY_ONE_SHOT])
        assertEquals(null, entries.single { it.statusCode == 204 }.metadata[MockMetadata.KEY_MOCKED])
    }

    @Test
    fun `a request not matching any mock rule hits the real server as usual`() {
        MockEngine.addRule(MockRule(id = "1", urlPattern = "/mocked-only"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        interceptor.attach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        client.newCall(Request.Builder().url(server.url("/unrelated")).build()).execute().close()

        assertEquals(1, server.requestCount)
    }
}
