package com.bugsy.core

import com.bugsy.core.model.DebugRequest
import com.bugsy.core.model.RequestSecrets

/**
 * Builds the [DebugRequest] for both platforms: everything that goes into the repository comes out redacted and masked.
 * The real values are only kept (in memory, in [RequestSecrets]) when something was masked and
 * `BugsyConfig.allowRevealingSecrets` is turned on.
 */
internal object RequestCapture {

    fun build(
        id: String,
        url: String,
        method: String,
        requestHeaders: Map<String, String>,
        requestBody: String?,
        statusCode: Int?,
        responseHeaders: Map<String, String>,
        responseBody: String?,
        durationMs: Long,
        timestamp: Long,
        metadata: Map<String, String> = emptyMap(),
    ): DebugRequest {
        val maskedRequestHeaders = HeaderRedactor.redact(requestHeaders)
        val maskedResponseHeaders = HeaderRedactor.redact(responseHeaders)
        val maskedRequestBody = BodyFieldMasker.maskTracked(requestBody)
        val maskedResponseBody = BodyFieldMasker.maskTracked(responseBody)

        val request = DebugRequest(
            id = id,
            url = url,
            method = method,
            requestHeaders = maskedRequestHeaders,
            requestBody = maskedRequestBody.value,
            statusCode = statusCode,
            responseHeaders = maskedResponseHeaders,
            responseBody = maskedResponseBody.value,
            durationMs = durationMs,
            timestamp = timestamp,
            curlCommand = "",
            metadata = metadata,
        )
        val masked = request.copy(curlCommand = CurlGenerator.generate(request))

        val hasSecrets = maskedRequestHeaders != requestHeaders || maskedResponseHeaders != responseHeaders ||
            maskedRequestBody.redacted || maskedResponseBody.redacted
        if (!hasSecrets || !CaptureSettings.allowsRevealingSecrets()) return masked

        return masked.copy(
            secrets = RequestSecrets(
                requestHeaders = requestHeaders,
                requestBody = requestBody,
                responseHeaders = responseHeaders,
                responseBody = responseBody,
                curlCommand = CurlGenerator.generate(request.copy(requestHeaders = requestHeaders, requestBody = requestBody)),
            )
        )
    }
}
