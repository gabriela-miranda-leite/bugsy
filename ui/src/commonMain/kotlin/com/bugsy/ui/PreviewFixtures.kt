package com.bugsy.ui

import com.bugsy.core.MockMetadata
import com.bugsy.core.model.DebugFlag
import com.bugsy.core.model.DebugRequest
import com.bugsy.core.model.MockRule
import com.bugsy.core.model.RequestSecrets
import com.bugsy.core.model.DeviceInfo

/**
 * Sample data used only in @Preview composables — never reaches an app host.
 */
internal object PreviewFixtures {
    val getRequest = DebugRequest(
        id = "1",
        url = "https://api.bugsy.dev/v1/users/42",
        method = "GET",
        requestHeaders = mapOf("Accept" to "application/json"),
        requestBody = null,
        statusCode = 200,
        responseHeaders = mapOf("Content-Type" to "application/json"),
        responseBody = """{"id":42,"name":"Ada Lovelace"}""",
        durationMs = 128,
        timestamp = 1_700_000_000_000,
        curlCommand = "curl https://api.bugsy.dev/v1/users/42",
    )

    val postRequestError = DebugRequest(
        id = "2",
        url = "https://api.bugsy.dev/v1/orders",
        method = "POST",
        requestHeaders = mapOf("Content-Type" to "application/json", "Authorization" to "Bearer ***"),
        requestBody = """{"items":[{"sku":"BUG-1","qty":2}]}""",
        statusCode = 500,
        responseHeaders = emptyMap(),
        responseBody = """{"error":"internal_error"}""",
        durationMs = 340,
        timestamp = 1_700_000_050_000,
        curlCommand = "curl -X POST https://api.bugsy.dev/v1/orders -d '{\"items\":[...]}'",
    )

    val postRequestWithSecrets = postRequestError.copy(
        id = "3",
        requestBody = """{"user":"gabi","password":"***"}""",
        responseBody = """{"token":"***","ok":true}""",
        curlCommand = "curl -X POST 'https://api.bugsy.dev/v1/orders' \\\n  -H 'Authorization: Bearer ***'",
        secrets = RequestSecrets(
            requestHeaders = mapOf("Content-Type" to "application/json", "Authorization" to "Bearer real-token"),
            requestBody = """{"user":"gabi","password":"p4ssw0rd"}""",
            responseHeaders = emptyMap(),
            responseBody = """{"token":"resp-token","ok":true}""",
            curlCommand = "curl -X POST 'https://api.bugsy.dev/v1/orders' \\\n  -H 'Authorization: Bearer real-token'",
        ),
    )

    val getRequestSlow = getRequest.copy(
        id = "4",
        url = "https://api.bugsy.dev/v1/products?page=1&limit=20",
        durationMs = 2_400,
    )

    val postRequestUnauthorized = DebugRequest(
        id = "5",
        url = "https://api.bugsy.dev/v1/auth/login",
        method = "POST",
        requestHeaders = mapOf("Content-Type" to "application/json"),
        requestBody = """{"user":"gabi","password":"***"}""",
        statusCode = 401,
        responseHeaders = emptyMap(),
        responseBody = """{"error":"invalid_credentials"}""",
        durationMs = 89,
        timestamp = 1_700_000_040_000,
        curlCommand = "curl -X POST https://api.bugsy.dev/v1/auth/login -d '{\"user\":\"gabi\",...}'",
    )

    val getRequestMocked = DebugRequest(
        id = "6",
        url = "https://api.bugsy.dev/v1/feed",
        method = "GET",
        requestHeaders = emptyMap(),
        requestBody = null,
        statusCode = 200,
        responseHeaders = mapOf("Content-Type" to "application/json"),
        responseBody = """{"items": []}""",
        durationMs = 12,
        timestamp = 1_700_000_060_000,
        curlCommand = "curl https://api.bugsy.dev/v1/feed",
        metadata = mapOf(MockMetadata.KEY_MOCKED to MockMetadata.VALUE_TRUE),
    )

    val postRequestMockedOnce = postRequestUnauthorized.copy(
        id = "7",
        durationMs = 300,
        metadata = mapOf(MockMetadata.KEY_MOCKED to MockMetadata.VALUE_TRUE, MockMetadata.KEY_ONE_SHOT to MockMetadata.VALUE_TRUE),
    )

    val requests = listOf(getRequest, postRequestError)

    val mockRules = listOf(
        MockRule(id = "m1", urlPattern = "^/api/feed.*", method = "GET", statusCode = 200, responseBody = """{"items": []}"""),
        MockRule(
            id = "m2",
            urlPattern = "/api/v1/auth/login",
            name = "Login failure",
            method = "POST",
            statusCode = 401,
            responseBody = """{"error": "unauthorized"}""",
            delayMs = 300,
            isOneShot = true,
        ),
        MockRule(
            id = "m3",
            urlPattern = "/api/v1/payments/.*",
            method = null,
            statusCode = 503,
            responseBody = """{"error": "service_unavailable"}""",
            delayMs = 2_000,
            isActive = false,
        ),
        MockRule(id = "m4", urlPattern = "/api/v1/users/me", method = "GET", statusCode = 200, responseBody = """{"id": 42}"""),
    )

    val deviceInfo = DeviceInfo(
        appVersionName = "2.1.4",
        appVersionCode = "241",
        environment = "staging",
        buildType = "debug",
        deviceModel = "Pixel 7 Pro",
        osName = "Android",
        osVersion = "14",
        locale = "pt-BR",
        bugsyVersion = "0.1.0-alpha",
        kotlinVersion = "2.0.0",
        composeVersion = "1.7.0",
    )

    val customInfo = listOf(
        "user_id" to "u_42",
        "email" to "ada@bugsy.dev",
        "document" to "123.456.789-00",
    )

    val flags = listOf(
        DebugFlag(key = "featureNewCheckout", label = "featureNewCheckout", value = true, isOverridden = true),
        DebugFlag(key = "featureRecommendations", label = "featureRecommendations", value = false, isOverridden = false),
        DebugFlag(key = "maintenanceMode", label = "maintenanceMode", value = false, isOverridden = false),
        DebugFlag(key = "debugLogging", label = "debugLogging", value = true, isOverridden = true),
        DebugFlag(key = "apiEnvironment", label = "apiEnvironment", value = "sandbox", isOverridden = false),
    )
}
