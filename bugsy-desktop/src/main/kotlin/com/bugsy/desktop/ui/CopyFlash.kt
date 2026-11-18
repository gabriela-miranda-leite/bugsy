package com.bugsy.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FLASH_MS = 1_200L

/** A local copy of `:ui`'s `CopyFlash` (DEB-60), same reasoning as the rest of this module not depending on
 * `:ui`. "Copied" feedback: [copied] is `true` for a moment after [flash]. */
@Stable
class CopyFlash internal constructor(private val scope: CoroutineScope) {
    var copied by mutableStateOf(false)
        private set

    fun flash() {
        copied = true
        scope.launch {
            delay(FLASH_MS)
            copied = false
        }
    }
}

@Composable
fun rememberCopyFlash(): CopyFlash {
    val scope = rememberCoroutineScope()
    return remember { CopyFlash(scope) }
}
