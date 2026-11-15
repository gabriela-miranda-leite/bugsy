package com.bugsy.core

import com.bugsy.core.model.DebugRequest
import com.bugsy.core.model.MockRule
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException
import java.util.UUID

internal class OkHttpDebugInterceptor : Interceptor {

    @Volatile
    private var isActive = false

    fun attach() {
        isActive = true
    }

    fun detach() {
        isActive = false
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!isActive) return chain.proceed(request)

        val mockRule = MockEngine.match(request.method, request.url.toString(), System.currentTimeMillis())
        if (mockRule != null) return respondWithMock(request, mockRule)

        val startNs = System.nanoTime()
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            // The real call failed (timeout, unreachable host...): record an error entry and rethrow,
            // so the client's caller still gets OkHttp's normal IOException.
            try {
                DebugRepository.add(buildErrorDebugRequest(request, startNs))
            } catch (addError: Exception) {
                // Never log the exception or its message: it could contain pieces of captured headers or body.
            }
            throw e
        }

        try {
            DebugRepository.add(buildDebugRequest(request, response, startNs))
        } catch (e: Exception) {
            // Never log the exception or its message: it could contain pieces of captured headers or body.
        }

        return response
    }

    /** Synthesizes a [Response] from [rule] instead of calling [Interceptor.Chain.proceed], and records it. */
    private fun respondWithMock(request: Request, rule: MockRule): Response {
        val startNs = System.nanoTime()
        if (rule.delayMs > 0) Thread.sleep(rule.delayMs)

        val responseContentType = rule.responseHeaders["Content-Type"]?.toMediaType() ?: MOCK_MEDIA_TYPE
        val responseHeadersBuilder = Headers.Builder()
        rule.responseHeaders.forEach { (name, value) -> responseHeadersBuilder.add(name, value) }

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(rule.statusCode)
            .message("Mocked")
            .headers(responseHeadersBuilder.build())
            .body(rule.responseBody.toResponseBody(responseContentType))
            .build()

        try {
            val requestBody = request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            }
            val metadata = buildMap {
                put(MockMetadata.KEY_MOCKED, MockMetadata.VALUE_TRUE)
                if (rule.isOneShot) put(MockMetadata.KEY_ONE_SHOT, MockMetadata.VALUE_TRUE)
            }
            DebugRepository.add(
                RequestCapture.build(
                    id = UUID.randomUUID().toString(),
                    url = request.url.toString(),
                    method = request.method,
                    requestHeaders = request.headers.toMap(),
                    requestBody = requestBody,
                    statusCode = rule.statusCode,
                    responseHeaders = rule.responseHeaders,
                    responseBody = rule.responseBody,
                    durationMs = (System.nanoTime() - startNs) / 1_000_000,
                    timestamp = System.currentTimeMillis(),
                    metadata = metadata,
                ),
            )
        } catch (e: Exception) {
            // Never log the exception or its message: it could contain pieces of captured headers or body.
        }

        return response
    }

    private fun buildErrorDebugRequest(request: Request, startNs: Long): DebugRequest {
        val requestBody = request.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        }

        return RequestCapture.build(
            id = UUID.randomUUID().toString(),
            url = request.url.toString(),
            method = request.method,
            requestHeaders = request.headers.toMap(),
            requestBody = requestBody,
            statusCode = null,
            responseHeaders = emptyMap(),
            responseBody = null,
            durationMs = (System.nanoTime() - startNs) / 1_000_000,
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun buildDebugRequest(request: Request, response: Response, startNs: Long): DebugRequest {
        val requestBody = request.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        }
        val responseBody = response.peekBody(MAX_BODY_BYTES).string()

        return RequestCapture.build(
            id = UUID.randomUUID().toString(),
            url = request.url.toString(),
            method = request.method,
            requestHeaders = request.headers.toMap(),
            requestBody = requestBody,
            statusCode = response.code,
            responseHeaders = response.headers.toMap(),
            responseBody = responseBody,
            durationMs = (System.nanoTime() - startNs) / 1_000_000,
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun Headers.toMap(): Map<String, String> =
        names().associateWith { name -> get(name).orEmpty() }

    private companion object {
        const val MAX_BODY_BYTES = 1L * 1024 * 1024
        val MOCK_MEDIA_TYPE = "application/json".toMediaType()
    }
}
