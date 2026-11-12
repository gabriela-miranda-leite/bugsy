package com.bugsy.ui.screens

import com.bugsy.core.model.DebugRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class RequestListScreenTest {

    private fun request(id: String, method: String, url: String) = DebugRequest(
        id = id,
        url = url,
        method = method,
        requestHeaders = emptyMap(),
        requestBody = null,
        statusCode = 200,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 10,
        timestamp = 0,
        curlCommand = "",
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
}
