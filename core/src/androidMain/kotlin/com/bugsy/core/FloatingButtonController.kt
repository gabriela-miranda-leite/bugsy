package com.bugsy.core

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.hypot

actual class FloatingButtonController private constructor(
    private val delegate: Delegate?
) {
    actual fun start() {
        delegate?.start()
    }

    actual fun stop() {
        delegate?.stop()
    }

    actual companion object {
        actual fun create(onTap: () -> Unit): FloatingButtonController {
            val context = if (BugsyContext.isHostDebuggable()) BugsyContext.applicationContext else null
            return FloatingButtonController(context?.let { Delegate(it, onTap) })
        }
    }

    private class Delegate(
        private val context: Context,
        private val onTap: () -> Unit
    ) {
        private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        private val buttonSizePx = (BUTTON_SIZE_DP * context.resources.displayMetrics.density).toInt()
        private val buttonView = createButtonView()
        private val layoutParams = createLayoutParams()
        private val snapCalculator = FloatingButtonSnapCalculator(buttonSizePx.toFloat())
        private var isAttached = false

        private var downRawX = 0f
        private var downRawY = 0f
        private var downLayoutX = 0
        private var downLayoutY = 0

        init {
            buttonView.setOnTouchListener { _, event -> onTouch(event) }
        }

        fun start() {
            if (!Settings.canDrawOverlays(context)) {
                requestOverlayPermission()
                return
            }
            if (isAttached) return

            windowManager.addView(buttonView, layoutParams)
            isAttached = true
        }

        fun stop() {
            if (!isAttached) return

            windowManager.removeView(buttonView)
            isAttached = false
        }

        private fun requestOverlayPermission() {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        private fun onTouch(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    downLayoutX = layoutParams.x
                    downLayoutY = layoutParams.y
                }

                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = downLayoutX + (event.rawX - downRawX).toInt()
                    layoutParams.y = downLayoutY + (event.rawY - downRawY).toInt()
                    windowManager.updateViewLayout(buttonView, layoutParams)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val movedDistance = hypot(
                        (event.rawX - downRawX).toDouble(),
                        (event.rawY - downRawY).toDouble()
                    )
                    if (movedDistance < TAP_SLOP_PX) {
                        onTap()
                    } else {
                        snapToNearestEdge()
                    }
                }
            }
            return true
        }

        private fun snapToNearestEdge() {
            val screenWidthPx = context.resources.displayMetrics.widthPixels
            val buttonCenterX = layoutParams.x + buttonSizePx / 2
            val targetCenterX = snapCalculator.targetCenterX(buttonCenterX.toFloat(), screenWidthPx.toFloat())
            val targetX = (targetCenterX - buttonSizePx / 2).toInt()

            ValueAnimator.ofInt(layoutParams.x, targetX).apply {
                duration = SNAP_ANIMATION_DURATION_MS
                addUpdateListener { animator ->
                    layoutParams.x = animator.animatedValue as Int
                    if (isAttached) windowManager.updateViewLayout(buttonView, layoutParams)
                }
                start()
            }
        }

        private fun createLayoutParams(): WindowManager.LayoutParams {
            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }

            return WindowManager.LayoutParams(
                buttonSizePx,
                buttonSizePx,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = context.resources.displayMetrics.heightPixels / 3
            }
        }

        // The Bugsy mark on the overlay's dark surface (see res/drawable/bugsy_floating_button.xml), not a plain
        // purple circle, so the button looks like the rest of Bugsy.
        private fun createButtonView(): View = View(context).apply {
            background = context.getDrawable(R.drawable.bugsy_floating_button)
        }

        private companion object {
            const val BUTTON_SIZE_DP = 56
            const val TAP_SLOP_PX = 10
            const val SNAP_ANIMATION_DURATION_MS = 200L
        }
    }
}
