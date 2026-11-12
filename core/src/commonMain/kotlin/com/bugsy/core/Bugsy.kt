package com.bugsy.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object Bugsy {
    val version: String = BuildKonfig.VERSION

    private val _isVisible = MutableStateFlow(false)
    val isVisible: StateFlow<Boolean> = _isVisible

    fun show() {
        _isVisible.value = true
    }

    fun hide() {
        _isVisible.value = false
    }
}
