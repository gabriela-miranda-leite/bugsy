package com.bugsy.core

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.startup.Initializer

internal object BugsyContext {
    @Volatile
    var applicationContext: Context? = null

    /** Test-only: forces the result of [isHostDebuggable]. */
    @Volatile
    var debuggableOverride: Boolean? = null

    /**
     * Whether the **host app** is debuggable. This is the gate that decides whether Bugsy turns on, instead of
     * `BuildConfig.DEBUG`: in a published `.aar`, the library's `BuildConfig` is the release variant's
     * (`DEBUG = false`) even inside a debug app. With no context, the answer is `false` (off).
     */
    fun isHostDebuggable(): Boolean =
        debuggableOverride
            ?: applicationContext?.let { (it.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }
            ?: false
}

internal class BugsyContextInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        BugsyContext.applicationContext = context.applicationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
