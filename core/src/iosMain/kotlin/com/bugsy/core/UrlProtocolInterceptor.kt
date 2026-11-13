@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.bugsy.core

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectBase.OverrideInit
import platform.Foundation.NSCachedURLResponse
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.HTTPMethod
import platform.Foundation.NSNumber
import platform.Foundation.NSURLProtocolMeta
import platform.Foundation.allHTTPHeaderFields
import platform.Foundation.NSURLCacheStoragePolicy
import platform.Foundation.NSURLProtocol
import platform.Foundation.NSURLProtocolClientProtocol
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.dataTaskWithRequest
import kotlin.time.TimeSource

/**
 * Registra globalmente um [NSURLProtocol] que captura as requests de qualquer NSURLSession do processo.
 *
 * `registerClass` só afeta `URLSession.shared` e sessões criadas com a configuração padrão. Sessões
 * com `NSURLSessionConfiguration` customizada precisam incluir a classe em `protocolClasses`.
 */
internal class UrlProtocolInterceptor {
    fun attach() {
        NSURLProtocol.registerClass(BugsyUrlProtocol.`class`()!!)
    }

    fun detach() {
        NSURLProtocol.unregisterClass(BugsyUrlProtocol.`class`()!!)
    }
}

internal class BugsyUrlProtocol @OverrideInit constructor(
    request: NSURLRequest,
    cachedResponse: NSCachedURLResponse?,
    client: NSURLProtocolClientProtocol?,
) : NSURLProtocol(request, cachedResponse, client) {

    private var task: NSURLSessionDataTask? = null

    override fun startLoading() {
        val original = request
        val handled = (original.mutableCopy() as NSMutableURLRequest).also {
            NSURLProtocol.setProperty(NSNumber(bool = true), HANDLED_KEY, it)
        }
        val startedAt = TimeSource.Monotonic.markNow()

        task = internalSession.dataTaskWithRequest(handled) { data, response, error ->
            forwardToClient(data, response, error)
            if (error == null) capture(original, data, response, startedAt.elapsedNow().inWholeMilliseconds)
        }.also { it.resume() }
    }

    override fun stopLoading() {
        task?.cancel()
        task = null
    }

    private fun forwardToClient(data: NSData?, response: NSURLResponse?, error: NSError?) {
        val client = client ?: return
        when {
            error != null -> client.URLProtocol(this, didFailWithError = error)
            else -> {
                if (response != null) {
                    client.URLProtocol(this, didReceiveResponse = response, cacheStoragePolicy = NSURLCacheStoragePolicy.NSURLCacheStorageNotAllowed)
                }
                if (data != null) client.URLProtocol(this, didLoadData = data)
                client.URLProtocolDidFinishLoading(this)
            }
        }
    }

    private fun capture(request: NSURLRequest, data: NSData?, response: NSURLResponse?, durationMs: Long) {
        try {
            val http = response as? NSHTTPURLResponse
            DebugRepository.add(
                IosRequestCapture.build(
                    url = request.URL?.absoluteString.orEmpty(),
                    method = request.HTTPMethod ?: "GET",
                    requestHeaders = request.allHTTPHeaderFields.toStringMap(),
                    requestBody = request.bodyData()?.toUtf8String(),
                    statusCode = http?.statusCode?.toInt(),
                    responseHeaders = http?.allHeaderFields.toStringMap(),
                    responseBody = data?.toUtf8String(IosRequestCapture.MAX_BODY_BYTES),
                    durationMs = durationMs,
                )
            )
        } catch (e: Exception) {
            println("Bugsy: failed to capture debug request — ${e.message}")
        }
    }

    companion object : NSURLProtocolMeta() {
        override fun canInitWithRequest(request: NSURLRequest): Boolean =
            isInterceptable(request.URL?.scheme, handled = NSURLProtocol.propertyForKey(HANDLED_KEY, request) != null)

        override fun canonicalRequestForRequest(request: NSURLRequest): NSURLRequest = request

        internal fun isInterceptable(scheme: String?, handled: Boolean): Boolean =
            !handled && (scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true))
    }
}

private const val HANDLED_KEY = "com.bugsy.core.handled"

// Sessão interna sem BugsyUrlProtocol em protocolClasses — evita a interceptação recursiva.
private val internalSession: NSURLSession by lazy {
    val configuration = NSURLSessionConfiguration.ephemeralSessionConfiguration().apply {
        protocolClasses = emptyList<Any>()
    }
    NSURLSession.sessionWithConfiguration(configuration)
}
