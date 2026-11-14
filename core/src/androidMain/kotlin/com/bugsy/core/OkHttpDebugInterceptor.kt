package com.bugsy.core

import com.bugsy.core.model.DebugRequest
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
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

        val startNs = System.nanoTime()
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            // A chamada real falhou (timeout, host inacessível...): registra uma entry de erro e relança,
            // pra quem chamou o client continuar recebendo a IOException normal do OkHttp.
            try {
                DebugRepository.add(buildErrorDebugRequest(request, startNs))
            } catch (addError: Exception) {
                // Nunca registrar a exceção nem sua mensagem: ela pode conter trechos de headers ou do body capturado.
            }
            throw e
        }

        try {
            DebugRepository.add(buildDebugRequest(request, response, startNs))
        } catch (e: Exception) {
            // Nunca registrar a exceção nem sua mensagem: ela pode conter trechos de headers ou do body capturado.
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
    }
}
