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
 * Escuta o gesto de "sacudir" do próprio iOS (o evento de movimento do UIKit), que é o que o Simulator envia em
 * Device > Shake (Ctrl+Cmd+Z) e o que o sistema entrega em aparelhos reais. O acelerômetro (CoreMotion) não existe
 * no Simulator, então só ele não basta.
 *
 * Para isso um único método é interceptado: `UIWindow.motionEnded(_:with:)`. O comportamento original é sempre
 * preservado (é chamado em seguida), então o app host continua recebendo o gesto normalmente.
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
