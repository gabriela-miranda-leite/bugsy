package com.bugsy.core

// O iOS não tem notificação persistente equivalente: este trigger usa o shake.
actual class NotificationTrigger private constructor(
    private val shakeDetector: ShakeDetector
) {
    actual fun start() {
        shakeDetector.start()
    }

    actual fun stop() {
        shakeDetector.stop()
    }

    actual companion object {
        actual fun create(onTap: () -> Unit): NotificationTrigger =
            NotificationTrigger(ShakeDetector.create(BugsyConfig.DEFAULT_SHAKE_THRESHOLD_G, onShake = onTap))
    }
}
