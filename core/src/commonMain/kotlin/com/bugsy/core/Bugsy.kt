package com.bugsy.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object Bugsy {
    val version: String = BuildKonfig.VERSION

    private val _isVisible = MutableStateFlow(false)
    val isVisible: StateFlow<Boolean> = _isVisible

    private var shakeDetector: ShakeDetector? = null

    fun initialize(config: BugsyConfig = BugsyConfig()) {
        destroy()

        if (config.trigger == Trigger.Shake) {
            shakeDetector = ShakeDetector.create(config.shakeThresholdG, onShake = ::show)
                .also { it.start() }
        }
    }

    fun destroy() {
        shakeDetector?.stop()
        shakeDetector = null
    }

    fun show() {
        _isVisible.value = true
    }

    fun hide() {
        _isVisible.value = false
    }
}
