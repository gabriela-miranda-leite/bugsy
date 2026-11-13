package com.bugsy.core

import android.content.Context
import androidx.startup.Initializer

internal object BugsyContext {
    @Volatile
    var applicationContext: Context? = null
}

internal class BugsyContextInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        BugsyContext.applicationContext = context.applicationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
