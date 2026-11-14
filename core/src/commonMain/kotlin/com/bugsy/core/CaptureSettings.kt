package com.bugsy.core

import kotlinx.coroutines.flow.MutableStateFlow

internal object CaptureSettings {
    private val allowRevealingSecrets = MutableStateFlow(true)

    fun configure(allowRevealingSecrets: Boolean) {
        this.allowRevealingSecrets.value = allowRevealingSecrets
    }

    fun allowsRevealingSecrets(): Boolean = allowRevealingSecrets.value
}
