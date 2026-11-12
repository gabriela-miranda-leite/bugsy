package com.bugsy.core

import kotlin.test.Test

class DebugInterceptorTest {

    @Test
    fun `create returns an interceptor that can attach and detach without throwing`() {
        val interceptor = DebugInterceptor.create()
        interceptor.attach()
        interceptor.detach()
    }
}
