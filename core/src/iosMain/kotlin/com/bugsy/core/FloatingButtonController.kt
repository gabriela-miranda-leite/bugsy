package com.bugsy.core

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIColor
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateChanged
import platform.UIKit.UIGestureRecognizerStateEnded
import platform.UIKit.UIPanGestureRecognizer
import platform.UIKit.UIScreen
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIView.Companion.animateWithDuration
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowLevelAlert
import platform.darwin.NSObject

private const val BUTTON_SIZE = 56.0
private const val SNAP_ANIMATION_DURATION_S = 0.2

// TODO: validar em device/simulador real (sample-ios ainda não existe) — drag/snap e
// garantir que a UIWindow some corretamente ao destruir o Bugsy. Ver DEB-21.
actual class FloatingButtonController private constructor(
    private val delegate: Delegate
) {
    actual fun start() {
        delegate.start()
    }

    actual fun stop() {
        delegate.stop()
    }

    actual companion object {
        actual fun create(onTap: () -> Unit): FloatingButtonController =
            FloatingButtonController(Delegate(onTap))
    }

    @OptIn(ExperimentalForeignApi::class)
    private class Delegate(private val onTap: () -> Unit) : NSObject() {
        private val snapCalculator = FloatingButtonSnapCalculator(BUTTON_SIZE.toFloat())
        private var window: UIWindow? = null
        private var buttonView: UIView? = null
        private var dragStartCenterX = 0.0
        private var dragStartCenterY = 0.0

        fun start() {
            if (window != null) return

            val screenBounds = UIScreen.mainScreen.bounds
            val screenSize = screenBounds.useContents { size }

            val button = UIView(
                frame = CGRectMake(0.0, screenSize.height / 3, BUTTON_SIZE, BUTTON_SIZE)
            ).apply {
                backgroundColor = UIColor(red = 0.38, green = 0.0, blue = 0.93, alpha = 1.0)
                layer.cornerRadius = BUTTON_SIZE / 2
                clipsToBounds = true
                userInteractionEnabled = true
            }
            button.addGestureRecognizer(
                UIPanGestureRecognizer(target = this, action = NSSelectorFromString("onPan:"))
            )
            button.addGestureRecognizer(
                UITapGestureRecognizer(target = this, action = NSSelectorFromString("onTapGesture:"))
            )

            val newWindow = UIWindow(frame = screenBounds).apply {
                windowLevel = UIWindowLevelAlert + 1
                backgroundColor = UIColor.clearColor
                rootViewController = UIViewController().apply {
                    view.backgroundColor = UIColor.clearColor
                }
                hidden = false
            }
            newWindow.rootViewController?.view?.addSubview(button)

            window = newWindow
            buttonView = button
        }

        fun stop() {
            window?.hidden = true
            window = null
            buttonView = null
        }

        @OptIn(BetaInteropApi::class)
        @ObjCAction
        fun onPan(recognizer: UIPanGestureRecognizer) {
            val view = buttonView ?: return
            val containerView = window?.rootViewController?.view ?: return

            when (recognizer.state) {
                UIGestureRecognizerStateBegan -> {
                    val center = view.center.useContents { this }
                    dragStartCenterX = center.x
                    dragStartCenterY = center.y
                }

                UIGestureRecognizerStateChanged -> {
                    val translation = recognizer.translationInView(containerView).useContents { this }
                    view.center = CGPointMake(
                        dragStartCenterX + translation.x,
                        dragStartCenterY + translation.y
                    )
                }

                UIGestureRecognizerStateEnded -> {
                    snapToNearestEdge(view, containerView)
                }
            }
        }

        @OptIn(BetaInteropApi::class)
        @ObjCAction
        fun onTapGesture(recognizer: UITapGestureRecognizer) {
            onTap()
        }

        private fun snapToNearestEdge(view: UIView, containerView: UIView) {
            val containerWidth = containerView.bounds.useContents { size.width }
            val center = view.center.useContents { this }
            val targetX = snapCalculator.targetCenterX(center.x.toFloat(), containerWidth.toFloat())

            animateWithDuration(duration = SNAP_ANIMATION_DURATION_S) {
                view.center = CGPointMake(targetX.toDouble(), center.y)
            }
        }
    }
}
