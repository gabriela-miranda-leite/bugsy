@file:OptIn(ExperimentalForeignApi::class)

package com.bugsy.core

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import platform.UIKit.UIEventSubtypeMotionShake
import platform.UIKit.UIWindow
import platform.objc.class_getInstanceMethod
import platform.objc.class_replaceMethod
import platform.objc.method_getImplementation
import platform.objc.sel_registerName

/**
 * Listens to iOS's own "shake" gesture (the UIKit motion event), which is what the Simulator sends on
 * Device > Shake (Ctrl+Cmd+Z) and what the system delivers on real devices. The accelerometer (CoreMotion) doesn't
 * exist on the Simulator, so it alone isn't enough.
 *
 * To do this, a single method is intercepted: `UIWindow.motionEnded(_:with:)`. The original behavior is always
 * preserved (it's called right after), so the host app keeps receiving the gesture normally.
 */
private typealias MotionEnded = CFunction<(COpaquePointer?, COpaquePointer?, Long, COpaquePointer?) -> Unit>

private var originalMotionEnded: CPointer<MotionEnded>? = null
private val motionShakeListeners = mutableListOf<() -> Unit>()

internal fun addMotionShakeListener(listener: () -> Unit) {
    installMotionHook()
    motionShakeListeners += listener
}

internal fun removeMotionShakeListener(listener: () -> Unit) {
    motionShakeListeners -= listener
}

private fun dispatchMotionShake() {
    motionShakeListeners.toList().forEach { it() }
}

private fun installMotionHook() {
    if (originalMotionEnded != null) return
    val selector = sel_registerName("motionEnded:withEvent:")
    val windowClass = UIWindow.`class`()
    val method = class_getInstanceMethod(windowClass, selector) ?: return
    originalMotionEnded = method_getImplementation(method)?.reinterpret()

    val replacement = staticCFunction<COpaquePointer?, COpaquePointer?, Long, COpaquePointer?, Unit> { self, cmd, motion, event ->
        if (motion == UIEventSubtypeMotionShake) dispatchMotionShake()
        originalMotionEnded?.invoke(self, cmd, motion, event)
    }
    class_replaceMethod(windowClass, selector, replacement.reinterpret(), "v@:q@")
}
