package com.bugsy.core

import okhttp3.OkHttpClient

/** No-op: returns the builder unchanged. */
fun OkHttpClient.Builder.addBugsyInterceptor(): OkHttpClient.Builder = this
