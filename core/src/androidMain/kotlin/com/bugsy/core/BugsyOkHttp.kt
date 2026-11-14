package com.bugsy.core

import okhttp3.OkHttpClient

// Single instance: it's the same one Bugsy.initialize() attaches/detaches and that the app's clients use.
internal object SharedOkHttpInterceptor {
    val instance = OkHttpDebugInterceptor()
}

/**
 * Adds Bugsy's interceptor to the app's client. Only has an effect in debug; in release the
 * `bugsy-noop` artifact returns the builder unchanged.
 */
fun OkHttpClient.Builder.addBugsyInterceptor(): OkHttpClient.Builder =
    if (BugsyContext.isHostDebuggable()) addInterceptor(SharedOkHttpInterceptor.instance) else this
