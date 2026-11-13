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
        // Equivalente iOS do BuildConfig.DEBUG: em binários release o interceptor nunca é criado.
        @OptIn(ExperimentalNativeApi::class)
        actual fun create(): DebugInterceptor = DebugInterceptor(
            if (Platform.isDebugBinary) UrlProtocolInterceptor() else null
        )
    }
}
