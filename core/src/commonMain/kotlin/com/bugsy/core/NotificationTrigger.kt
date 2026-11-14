package com.bugsy.core

/**
 * Trigger "notificação fixa": enquanto o app está aberto, uma notificação persistente abre o overlay ao toque.
 * Só o Android tem esse conceito; no iOS este trigger se comporta como [Trigger.Shake].
 */
expect class NotificationTrigger {
    fun start()
    fun stop()

    companion object {
        fun create(onTap: () -> Unit): NotificationTrigger
    }
}
