package com.bugsy.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FLASH_MS = 1_200L

/** "Copied" feedback for a row: [copied] is `true` for a moment after [flash]. */
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

/** Subtle icon at the end of a copyable row; turns into a green "check" right after copying. */
@Composable
fun CopyAffordance(copied: Boolean, modifier: Modifier = Modifier) {
    Icon(
        if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
        contentDescription = null,
        tint = if (copied) BugsyColors.Status2xx else BugsyColors.TextDisabled,
        modifier = modifier.size(BugsySpacing.copyAffordanceSize),
    )
}
