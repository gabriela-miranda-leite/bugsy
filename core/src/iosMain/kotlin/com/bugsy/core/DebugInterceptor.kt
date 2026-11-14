package com.bugsy.core

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

actual class DebugInterceptor private constructor(
    private val delegate: UrlProtocolInterceptor?
) {
    actual fun attach() {
        delegate?.attach()
    }

    actual fun detach() {
        delegate?.detach()
    }

    actual companion object {
        // iOS equivalent of BuildConfig.DEBUG: in release binaries the interceptor is never created.
        @OptIn(ExperimentalNativeApi::class)
        actual fun create(): DebugInterceptor = DebugInterceptor(
            if (Platform.isDebugBinary) UrlProtocolInterceptor() else null
        )
    }
}
