package com.bugsy.core

actual class DebugInterceptor private constructor(
    private val delegate: OkHttpDebugInterceptor?
) {
    actual fun attach() {
        delegate?.attach()
    }

    actual fun detach() {
        delegate?.detach()
    }

    actual companion object {
        actual fun create(): DebugInterceptor = DebugInterceptor(
            if (BuildConfig.DEBUG) SharedOkHttpInterceptor.instance else null
        )
    }
}
