package com.bugsy.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

actual class ShakeDetector private constructor(
    private val delegate: Delegate?
) {
    actual fun start() {
        delegate?.start()
    }

    actual fun stop() {
        delegate?.stop()
    }

    actual companion object {
        actual fun create(thresholdG: Float, onShake: () -> Unit): ShakeDetector {
            val context = if (BugsyContext.isHostDebuggable()) BugsyContext.applicationContext else null
            return ShakeDetector(context?.let { Delegate(it, thresholdG, onShake) })
        }
    }

    private class Delegate(
        context: Context,
        thresholdG: Float,
        onShake: () -> Unit
    ) : SensorEventListener, DefaultLifecycleObserver {

        private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        private val magnitudeDetector = ShakeMagnitudeDetector(thresholdG = thresholdG, onShake = onShake)

        fun start() {
            registerSensor()
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        }

        fun stop() {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
            unregisterSensor()
        }

        override fun onStart(owner: LifecycleOwner) {
            registerSensor()
        }

        override fun onStop(owner: LifecycleOwner) {
            unregisterSensor()
        }

        private fun registerSensor() {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }

        private fun unregisterSensor() {
            sensorManager.unregisterListener(this)
        }

        override fun onSensorChanged(event: SensorEvent) {
            magnitudeDetector.onAcceleration(
                x = event.values[0],
                y = event.values[1],
                z = event.values[2],
                nowMs = System.currentTimeMillis()
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
}
