package com.bugsy.core

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OkHttpDebugInterceptorTest {

    private val server = MockWebServer()
    private val interceptor = OkHttpDebugInterceptor()

    @BeforeTest
    fun setUp() {
        server.start()
        DebugRepository.clear()
    }

    @AfterTest
    fun tearDown() {
        HeaderRedactor.reset()
        BodyFieldMasker.reset()
        CaptureSettings.configure(allowCopyingSecrets = true)
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
    fun `keeps a curl with the real values in memory next to the masked one`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        interceptor.attach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        client.newCall(
            Request.Builder().url(server.url("/login")).header("Authorization", "Bearer real-token")
                .post("""{"password":"p4ss"}""".toRequestBody()).build()
        ).execute().close()

        val entry = DebugRepository.getAll().single()
        assertTrue("real-token" !in entry.curlCommand && "p4ss" !in entry.curlCommand)
        assertTrue("Bearer real-token" in entry.curlCommandWithSecrets!!.value)
        assertTrue("p4ss" in entry.curlCommandWithSecrets!!.value)
        assertTrue("real-token" !in entry.toString())
    }

    @Test
    fun `does not keep the real values when copying secrets is disabled`() {
        CaptureSettings.configure(allowCopyingSecrets = false)
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        interceptor.attach()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        client.newCall(Request.Builder().url(server.url("/")).header("Authorization", "Bearer real-token").build()).execute().close()

        assertEquals(null, DebugRepository.getAll().single().curlCommandWithSecrets)
    }
}
