package com.bugsy.ui

import com.bugsy.core.model.DebugFlag
import com.bugsy.core.model.DebugRequest
import com.bugsy.core.model.SecretText
import com.bugsy.core.model.DeviceInfo

/**
 * Dados de exemplo usados apenas em @Preview composables — nunca chegam a um app host.
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
        durationMs = 2_340,
        timestamp = 1_700_000_050_000,
        curlCommand = "curl -X POST https://api.bugsy.dev/v1/orders -d '{\"items\":[...]}'",
    )

    val postRequestWithSecrets = postRequestError.copy(
        id = "3",
        curlCommand = "curl -X POST 'https://api.bugsy.dev/v1/orders' \\\n  -H 'Authorization: ***'",
        curlCommandWithSecrets = SecretText("curl -X POST 'https://api.bugsy.dev/v1/orders' \\\n  -H 'Authorization: Bearer real-token'"),
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

    val flags = listOf(
        DebugFlag(key = "featureNewCheckout", label = "featureNewCheckout", value = true, isOverridden = true),
        DebugFlag(key = "featureRecommendations", label = "featureRecommendations", value = false, isOverridden = false),
        DebugFlag(key = "maintenanceMode", label = "maintenanceMode", value = false, isOverridden = false),
        DebugFlag(key = "debugLogging", label = "debugLogging", value = true, isOverridden = true),
        DebugFlag(key = "apiEnvironment", label = "apiEnvironment", value = "sandbox", isOverridden = false),
    )
}
