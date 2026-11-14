package com.bugsy.core

/**
 * "Sticky notification" trigger: while the app is open, a persistent notification opens the overlay on tap.
 * Only Android has this concept; on iOS this trigger behaves like [Trigger.Shake].
 */
expect class NotificationTrigger {
    fun start()
    fun stop()

    companion object {
        fun create(onTap: () -> Unit): NotificationTrigger
    }
}
