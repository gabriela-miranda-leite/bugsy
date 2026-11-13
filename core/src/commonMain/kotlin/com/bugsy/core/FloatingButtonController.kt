package com.bugsy.core

expect class FloatingButtonController {
    fun start()
    fun stop()

    companion object {
        fun create(onTap: () -> Unit): FloatingButtonController
    }
}
