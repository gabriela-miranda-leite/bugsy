@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.bugsy.core

import com.bugsy.core.model.DebugRequest
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.Foundation.HTTPBody
import platform.Foundation.HTTPBodyStream
import platform.Foundation.NSMutableData
import platform.Foundation.appendBytes
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSInputStream
import platform.Foundation.NSString
import platform.Foundation.NSURLRequest
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.dataWithBytes
import platform.Foundation.subdataWithRange
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSMakeRange

internal object IosRequestCapture {
    const val MAX_BODY_BYTES = 1L * 1024 * 1024

    /** Redação e mascaramento acontecem aqui, antes de qualquer dado chegar ao DebugRepository. */
    fun build(
        url: String,
        method: String,
        requestHeaders: Map<String, String>,
        requestBody: String?,
        statusCode: Int?,
        responseHeaders: Map<String, String>,
        responseBody: String?,
        durationMs: Long,
    ): DebugRequest {
        val request = DebugRequest(
            id = NSUUID().UUIDString,
            url = url,
            method = method,
            requestHeaders = HeaderRedactor.redact(requestHeaders),
            requestBody = BodyFieldMasker.mask(requestBody),
            statusCode = statusCode,
            responseHeaders = HeaderRedactor.redact(responseHeaders),
            responseBody = BodyFieldMasker.mask(responseBody),
            durationMs = durationMs,
            timestamp = (NSDate().timeIntervalSince1970 * 1000).toLong(),
            curlCommand = "",
        )
        return request.copy(curlCommand = CurlGenerator.generate(request))
    }
}

internal fun Map<Any?, *>?.toStringMap(): Map<String, String> =
    this.orEmpty().entries.associate { (k, v) -> k.toString() to v.toString() }

internal fun NSData.toUtf8String(maxBytes: Long = Long.MAX_VALUE): String? {
    val slice = if (length.toLong() > maxBytes) subdataWithRange(NSMakeRange(0u, maxBytes.toULong())) else this
    return NSString.create(data = slice, encoding = NSUTF8StringEncoding)?.toString()
}

/** Lê `httpBody` ou, se a request usa stream, drena o `httpBodyStream`. */
internal fun NSURLRequest.bodyData(): NSData? {
    HTTPBody?.let { return it }
    val stream = HTTPBodyStream ?: return null
    return stream.readAll()
}

private fun NSInputStream.readAll(): NSData {
    val result = NSMutableData()
    val buffer = ByteArray(BUFFER_SIZE)
    open()
    try {
        while (true) {
            val read = buffer.usePinned { read(it.addressOf(0).reinterpret(), BUFFER_SIZE.toULong()) }
            if (read <= 0) break
            buffer.usePinned { result.appendBytes(it.addressOf(0), read.toULong()) }
        }
    } finally {
        close()
    }
    return result
}

private const val BUFFER_SIZE = 8 * 1024
