package com.bugsy.core

import com.bugsy.core.model.DebugRequest
import com.bugsy.core.model.RequestSecrets

/**
 * Monta o [DebugRequest] das duas plataformas: tudo que vai para o repositório sai redatado e mascarado.
 * Os valores reais só são guardados (em memória, em [RequestSecrets]) quando há algo mascarado e
 * `BugsyConfig.allowRevealingSecrets` está ligado.
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
