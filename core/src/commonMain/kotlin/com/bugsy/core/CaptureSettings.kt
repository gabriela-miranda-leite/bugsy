package com.bugsy.core

import kotlinx.coroutines.flow.MutableStateFlow

internal object CaptureSettings {
    private val allowCopyingSecrets = MutableStateFlow(true)

    fun configure(allowCopyingSecrets: Boolean) {
        this.allowCopyingSecrets.value = allowCopyingSecrets
    }

    fun allowsCopyingSecrets(): Boolean = allowCopyingSecrets.value
}
