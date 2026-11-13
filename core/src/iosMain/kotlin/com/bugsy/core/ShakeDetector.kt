package com.bugsy.core

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSDate
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification

// TODO: validar em device/simulador real (sample-ios ainda não existe) — shake abre overlay,
// debounce não abre duas vezes, e CMMotionManager não vaza em background. Ver DEB-20.
actual class ShakeDetector private constructor(
    private val delegate: Delegate
) {
    actual fun start() {
        delegate.start()
    }

    actual fun stop() {
        delegate.stop()
    }

    actual companion object {
        actual fun create(thresholdG: Float, onShake: () -> Unit): ShakeDetector =
            ShakeDetector(Delegate(thresholdG, onShake))
    }

    @OptIn(ExperimentalForeignApi::class)
    private class Delegate(
        thresholdG: Float,
        onShake: () -> Unit
    ) {
        private val motionManager = CMMotionManager()
        private val magnitudeDetector = ShakeMagnitudeDetector(thresholdG = thresholdG, onShake = onShake)

        private var becomeActiveObserver: Any? = null
        private var enterBackgroundObserver: Any? = null

        fun start() {
            registerUpdates()

            becomeActiveObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                name = UIApplicationDidBecomeActiveNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue
            ) { registerUpdates() }

            enterBackgroundObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                name = UIApplicationDidEnterBackgroundNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue
            ) { unregisterUpdates() }
        }

        fun stop() {
            unregisterUpdates()
            becomeActiveObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
            enterBackgroundObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
            becomeActiveObserver = null
            enterBackgroundObserver = null
        }

        private fun registerUpdates() {
            if (!motionManager.accelerometerAvailable || motionManager.accelerometerActive) return

            motionManager.startAccelerometerUpdatesToQueue(NSOperationQueue.mainQueue) { data, _ ->
                data?.acceleration?.useContents {
                    magnitudeDetector.onAcceleration(
                        x = (x * EARTH_GRAVITY_MS2).toFloat(),
                        y = (y * EARTH_GRAVITY_MS2).toFloat(),
                        z = (z * EARTH_GRAVITY_MS2).toFloat(),
                        nowMs = (NSDate().timeIntervalSince1970 * 1000).toLong()
                    )
                }
            }
        }

        private fun unregisterUpdates() {
            if (motionManager.accelerometerActive) {
                motionManager.stopAccelerometerUpdates()
            }
        }

        private companion object {
            const val EARTH_GRAVITY_MS2 = 9.80665
        }
    }
}
