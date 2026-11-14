package com.bugsy.core

import okhttp3.OkHttpClient

// Instância única: é a mesma que o Bugsy.initialize() liga/desliga e que os clients do app usam.
internal object SharedOkHttpInterceptor {
    val instance = OkHttpDebugInterceptor()
}

/**
 * Adiciona o interceptor do Bugsy ao client do app. Só tem efeito em debug; em release o artefato
 * `bugsy-noop` devolve o builder sem alterações.
 */
fun OkHttpClient.Builder.addBugsyInterceptor(): OkHttpClient.Builder =
    if (BuildConfig.DEBUG) addInterceptor(SharedOkHttpInterceptor.instance) else this
