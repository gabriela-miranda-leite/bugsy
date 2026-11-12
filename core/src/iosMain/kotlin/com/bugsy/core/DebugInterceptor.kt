package com.bugsy.core

actual class DebugInterceptor private constructor(
    private val delegate: UrlProtocolInterceptor
) {
    actual fun attach() {
        delegate.attach()
    }

    actual fun detach() {
        delegate.detach()
    }

    actual companion object {
        actual fun create(): DebugInterceptor = DebugInterceptor(UrlProtocolInterceptor())
    }
}
