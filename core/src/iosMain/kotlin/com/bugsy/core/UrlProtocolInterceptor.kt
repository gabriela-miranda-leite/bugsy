@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.bugsy.core

import com.bugsy.core.model.MockRule
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectBase.OverrideInit
import platform.Foundation.NSCachedURLResponse
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.HTTPMethod
import platform.Foundation.NSNumber
import platform.Foundation.NSString
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
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.dataUsingEncoding
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import kotlin.time.TimeSource

/**
 * Globally registers an [NSURLProtocol] that captures requests from any NSURLSession in the process.
 *
 * `registerClass` only affects `URLSession.shared` and sessions created with the default configuration. Sessions
 * with a custom `NSURLSessionConfiguration` need to include the class in `protocolClasses`.
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
        val url = original.URL?.absoluteString.orEmpty()
        val method = original.HTTPMethod ?: "GET"

        val mockRule = MockEngine.match(method, url, (NSDate().timeIntervalSince1970 * 1000).toLong())
        if (mockRule != null) {
            respondWithMock(original, mockRule)
            return
        }

        val handled = (original.mutableCopy() as NSMutableURLRequest).also {
            NSURLProtocol.setProperty(NSNumber(bool = true), HANDLED_KEY, it)
        }
        val startedAt = TimeSource.Monotonic.markNow()

        task = internalSession.dataTaskWithRequest(handled) { data, response, error ->
            forwardToClient(data, response, error)
            if (error == null) capture(original, data, response, startedAt.elapsedNow().inWholeMilliseconds)
        }.also { it.resume() }
    }

    /** Synthesizes a response from [rule] instead of hitting the network, mirroring the Android OkHttp flow. */
    private fun respondWithMock(original: NSURLRequest, rule: MockRule) {
        val startedAt = TimeSource.Monotonic.markNow()
        val deliver = {
            val data = NSString.create(string = rule.responseBody).dataUsingEncoding(NSUTF8StringEncoding)
            val httpResponse = NSHTTPURLResponse(
                uRL = original.URL!!,
                statusCode = rule.statusCode.toLong(),
                HTTPVersion = "HTTP/1.1",
                headerFields = rule.responseHeaders as Map<Any?, *>,
            )
            client?.URLProtocol(this, didReceiveResponse = httpResponse, cacheStoragePolicy = NSURLCacheStoragePolicy.NSURLCacheStorageNotAllowed)
            if (data != null) client?.URLProtocol(this, didLoadData = data)
            client?.URLProtocolDidFinishLoading(this)

            try {
                val metadata = buildMap {
                    put(MockMetadata.KEY_MOCKED, MockMetadata.VALUE_TRUE)
                    if (rule.isOneShot) put(MockMetadata.KEY_ONE_SHOT, MockMetadata.VALUE_TRUE)
                }
                DebugRepository.add(
                    IosRequestCapture.build(
                        url = original.URL?.absoluteString.orEmpty(),
                        method = original.HTTPMethod ?: "GET",
                        requestHeaders = original.allHTTPHeaderFields.toStringMap(),
                        requestBody = original.bodyData()?.toUtf8String(),
                        statusCode = rule.statusCode,
                        responseHeaders = rule.responseHeaders,
                        responseBody = rule.responseBody,
                        durationMs = startedAt.elapsedNow().inWholeMilliseconds,
                        metadata = metadata,
                    ),
                )
            } catch (e: Exception) {
                // Never log the exception or its message: it may contain fragments of captured headers or body.
            }
        }

        if (rule.delayMs > 0) {
            val delayNanos = rule.delayMs * NANOS_PER_MS
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, delayNanos), dispatch_get_main_queue(), deliver)
        } else {
            deliver()
        }
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
            // Never log the exception or its message: it may contain fragments of captured headers or body.
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
private const val NANOS_PER_MS = 1_000_000L

// Internal session without BugsyUrlProtocol in protocolClasses — avoids recursive interception.
private val internalSession: NSURLSession by lazy {
    val configuration = NSURLSessionConfiguration.ephemeralSessionConfiguration().apply {
        protocolClasses = emptyList<Any>()
    }
    NSURLSession.sessionWithConfiguration(configuration)
}
