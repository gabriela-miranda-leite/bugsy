package com.bugsy.core

expect class DebugInterceptor {
    fun attach()
    fun detach()

    companion object {
        fun create(): DebugInterceptor
    }
}
