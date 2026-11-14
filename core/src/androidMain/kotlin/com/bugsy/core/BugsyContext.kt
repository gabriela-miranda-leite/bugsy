package com.bugsy.core

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.startup.Initializer

internal object BugsyContext {
    @Volatile
    var applicationContext: Context? = null

    /** Só para testes: força o resultado de [isHostDebuggable]. */
    @Volatile
    var debuggableOverride: Boolean? = null

    /**
     * Se o **app host** é debuggable. É a barreira que decide se o Bugsy liga, no lugar de `BuildConfig.DEBUG`: num
     * `.aar` publicado, o `BuildConfig` da lib é o da variante release (`DEBUG = false`) mesmo dentro de um app de
     * debug. Sem contexto, a resposta é `false` (desligado).
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
