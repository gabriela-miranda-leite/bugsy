package com.bugsy.core

import kotlin.test.Test
import kotlin.test.assertEquals

private const val EARTH_GRAVITY_MS2 = 9.80665f

class ShakeMagnitudeDetectorTest {

    @Test
    fun `does not trigger below the threshold`() {
        var shakes = 0
        val detector = ShakeMagnitudeDetector(thresholdG = 2.5f, onShake = { shakes++ })

        detector.onAcceleration(x = 1f * EARTH_GRAVITY_MS2, y = 0f, z = 0f, nowMs = 0L)

        assertEquals(0, shakes)
    }

    @Test
    fun `triggers once the magnitude crosses the threshold`() {
        var shakes = 0
        val detector = ShakeMagnitudeDetector(thresholdG = 2.5f, onShake = { shakes++ })

        detector.onAcceleration(x = 3f * EARTH_GRAVITY_MS2, y = 0f, z = 0f, nowMs = 0L)

        assertEquals(1, shakes)
    }

    @Test
    fun `two fast shakes within the debounce window only trigger once`() {
        var shakes = 0
        val detector = ShakeMagnitudeDetector(thresholdG = 2.5f, debounceMs = 1_000L, onShake = { shakes++ })

        detector.onAcceleration(x = 3f * EARTH_GRAVITY_MS2, y = 0f, z = 0f, nowMs = 0L)
        detector.onAcceleration(x = 3f * EARTH_GRAVITY_MS2, y = 0f, z = 0f, nowMs = 500L)

        assertEquals(1, shakes)
    }

    @Test
    fun `a shake after the debounce window triggers again`() {
        var shakes = 0
        val detector = ShakeMagnitudeDetector(thresholdG = 2.5f, debounceMs = 1_000L, onShake = { shakes++ })

        detector.onAcceleration(x = 3f * EARTH_GRAVITY_MS2, y = 0f, z = 0f, nowMs = 0L)
        detector.onAcceleration(x = 3f * EARTH_GRAVITY_MS2, y = 0f, z = 0f, nowMs = 1_500L)

        assertEquals(2, shakes)
    }
}
