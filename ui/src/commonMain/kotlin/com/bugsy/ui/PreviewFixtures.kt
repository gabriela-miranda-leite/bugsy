package com.bugsy.ui

import com.bugsy.core.model.DebugFlag
import com.bugsy.core.model.DebugRequest
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

    val requests = listOf(getRequest, postRequestError)

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
