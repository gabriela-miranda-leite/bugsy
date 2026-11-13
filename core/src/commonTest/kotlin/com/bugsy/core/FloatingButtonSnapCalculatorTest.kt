package com.bugsy.core

import kotlin.test.Test
import kotlin.test.assertEquals

class FloatingButtonSnapCalculatorTest {

    private val calculator = FloatingButtonSnapCalculator(buttonSizePx = 56f)

    @Test
    fun `snaps to the left edge when the button center is left of the midpoint`() {
        val targetCenterX = calculator.targetCenterX(currentCenterX = 100f, containerWidthPx = 1000f)

        assertEquals(28f, targetCenterX)
    }

    @Test
    fun `snaps to the right edge when the button center is right of the midpoint`() {
        val targetCenterX = calculator.targetCenterX(currentCenterX = 900f, containerWidthPx = 1000f)

        assertEquals(972f, targetCenterX)
    }

    @Test
    fun `snaps to the right edge when the button center is exactly at the midpoint`() {
        val targetCenterX = calculator.targetCenterX(currentCenterX = 500f, containerWidthPx = 1000f)

        assertEquals(972f, targetCenterX)
    }
}
