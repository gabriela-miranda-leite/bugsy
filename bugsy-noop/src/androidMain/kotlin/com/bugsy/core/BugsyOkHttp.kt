package com.bugsy.core

import okhttp3.OkHttpClient

/** No-op: devolve o builder sem alterações. */
fun OkHttpClient.Builder.addBugsyInterceptor(): OkHttpClient.Builder = this
