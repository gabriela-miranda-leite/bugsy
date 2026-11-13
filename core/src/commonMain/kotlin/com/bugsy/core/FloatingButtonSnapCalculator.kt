package com.bugsy.core

/**
 * Decides which screen edge a dragged floating button should snap to.
 * Kept free of platform view APIs so the logic is testable in commonTest.
 */
internal class FloatingButtonSnapCalculator(
    private val buttonSizePx: Float
) {
    fun targetCenterX(currentCenterX: Float, containerWidthPx: Float): Float {
        val halfButton = buttonSizePx / 2
        return if (currentCenterX < containerWidthPx / 2) {
            halfButton
        } else {
            containerWidthPx - halfButton
        }
    }
}
