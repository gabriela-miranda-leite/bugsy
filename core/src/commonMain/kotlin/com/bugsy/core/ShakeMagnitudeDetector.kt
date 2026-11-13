package com.bugsy.core

import kotlin.math.sqrt

/**
 * Converts raw accelerometer samples into shake events.
 * Kept free of platform sensor APIs so the threshold/debounce logic is testable in commonTest.
 */
internal class ShakeMagnitudeDetector(
    private val thresholdG: Float,
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    private val onShake: () -> Unit
) {
    private var lastShakeAtMs: Long? = null

    fun onAcceleration(x: Float, y: Float, z: Float, nowMs: Long) {
        val magnitudeG = sqrt(x * x + y * y + z * z) / EARTH_GRAVITY_MS2
        if (magnitudeG < thresholdG) return

        val lastShake = lastShakeAtMs
        if (lastShake != null && nowMs - lastShake < debounceMs) return

        lastShakeAtMs = nowMs
        onShake()
    }

    private companion object {
        const val EARTH_GRAVITY_MS2 = 9.80665f
        const val DEFAULT_DEBOUNCE_MS = 1_000L
    }
}
