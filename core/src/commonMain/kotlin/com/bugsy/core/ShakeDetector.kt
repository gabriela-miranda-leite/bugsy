package com.bugsy.core

expect class ShakeDetector {
    fun start()
    fun stop()

    companion object {
        fun create(thresholdG: Float, onShake: () -> Unit): ShakeDetector
    }
}
