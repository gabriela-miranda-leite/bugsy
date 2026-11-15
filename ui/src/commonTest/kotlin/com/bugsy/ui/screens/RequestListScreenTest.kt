package com.bugsy.ui.screens

import com.bugsy.core.MockMetadata
import com.bugsy.core.model.DebugRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class RequestListScreenTest {

    private fun request(
        id: String,
        method: String,
        url: String,
        statusCode: Int? = 200,
        durationMs: Long = 10,
        metadata: Map<String, String> = emptyMap(),
    ) = DebugRequest(
        id = id,
        url = url,
        method = method,
        requestHeaders = emptyMap(),
        requestBody = null,
        statusCode = statusCode,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = durationMs,
        timestamp = 0,
        curlCommand = "",
        metadata = metadata,
    )

    private val getUsers = request("1", "GET", "https://api.bugsy.dev/v1/users")
    private val postOrders = request("2", "POST", "https://api.bugsy.dev/v1/orders")
    private val requests = listOf(getUsers, postOrders)

    @Test
    fun `filterRequests returns everything when the filter is blank`() {
        assertEquals(requests, filterRequests(requests, ""))
        assertEquals(requests, filterRequests(requests, "   "))
    }

    @Test
    fun `filterRequests matches by url substring case-insensitively`() {
        assertEquals(listOf(postOrders), filterRequests(requests, "ORDERS"))
    }

    @Test
    fun `filterRequests matches by method substring case-insensitively`() {
        assertEquals(listOf(getUsers), filterRequests(requests, "get"))
    }

    @Test
    fun `filterRequests returns an empty list when nothing matches`() {
        assertEquals(emptyList(), filterRequests(requests, "does-not-exist"))
    }

    @Test
    fun `All category keeps everything regardless of text filter`() {
        assertEquals(requests, filterRequests(requests, "", RequestCategoryFilter.All))
    }

    @Test
    fun `Errors category only keeps 4xx and 5xx`() {
        val ok = request("1", "GET", "https://api.bugsy.dev/v1/ok", statusCode = 200)
        val notFound = request("2", "GET", "https://api.bugsy.dev/v1/missing", statusCode = 404)
        val serverError = request("3", "GET", "https://api.bugsy.dev/v1/broken", statusCode = 500)

        val result = filterRequests(listOf(ok, notFound, serverError), "", RequestCategoryFilter.Errors)

        assertEquals(listOf(notFound, serverError), result)
    }

    // DebugRepository.slowRequestThresholdMs's setter is internal to :core, so these rely on its default
    // (2_000ms, see BugsyConfig.DEFAULT_SLOW_REQUEST_THRESHOLD_MS) rather than mutating it from :ui's tests.
    @Test
    fun `Slow category only keeps requests at or above the default threshold`() {
        val fast = request("1", "GET", "https://api.bugsy.dev/v1/fast", durationMs = 100)
        val slow = request("2", "GET", "https://api.bugsy.dev/v1/slow", durationMs = 2_000)

        val result = filterRequests(listOf(fast, slow), "", RequestCategoryFilter.Slow)

        assertEquals(listOf(slow), result)
    }

    @Test
    fun `Mocked category only keeps requests tagged mocked`() {
        val real = request("1", "GET", "https://api.bugsy.dev/v1/real")
        val mocked = request("2", "GET", "https://api.bugsy.dev/v1/mocked", metadata = mapOf(MockMetadata.KEY_MOCKED to MockMetadata.VALUE_TRUE))

        val result = filterRequests(listOf(real, mocked), "", RequestCategoryFilter.Mocked)

        assertEquals(listOf(mocked), result)
    }

    @Test
    fun `category and text filters combine`() {
        val mockedOrders = request(
            "1",
            "POST",
            "https://api.bugsy.dev/v1/orders",
            metadata = mapOf(MockMetadata.KEY_MOCKED to MockMetadata.VALUE_TRUE),
        )
        val mockedUsers = request(
            "2",
            "GET",
            "https://api.bugsy.dev/v1/users",
            metadata = mapOf(MockMetadata.KEY_MOCKED to MockMetadata.VALUE_TRUE),
        )

        val result = filterRequests(listOf(mockedOrders, mockedUsers), "orders", RequestCategoryFilter.Mocked)

        assertEquals(listOf(mockedOrders), result)
    }
}
