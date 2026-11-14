@file:Suppress("unused", "UNUSED_VARIABLE")

package dev.bugsy.sample

import androidx.compose.runtime.Composable
import com.bugsy.core.Bugsy
import com.bugsy.core.BugsyConfig
import com.bugsy.core.Trigger
import com.bugsy.core.addBugsyInterceptor
import com.bugsy.core.model.DebugFlag
import com.bugsy.ui.BugsyAndroidOverlay
import okhttp3.OkHttpClient

/**
 * API parity: this file uses **the entire** public Bugsy API and compiles in both sample variants
 * (debug against `:ui`, release against `:bugsy-noop`). If the debug artifact gains or changes an API and the noop
 * doesn't keep up, `assembleRelease` stops compiling here. Never executed. Add any new public API here when you create it.
 */
private fun apiSurface() {
    val version: String = Bugsy.version
    val visible = Bugsy.isVisible.value
    Bugsy.initialize()
    Bugsy.initialize(
        BugsyConfig(
            trigger = Trigger.Shake,
            shakeThresholdG = BugsyConfig.DEFAULT_SHAKE_THRESHOLD_G,
            flagsProvider = { listOf(DebugFlag(key = "k", label = "l", value = true, isOverridden = false)) },
            allowRevealingSecrets = true,
            customInfoProvider = { listOf("k" to "v") },
        )
    )
    Trigger.entries.forEach { trigger -> BugsyConfig(trigger = trigger) }
    Bugsy.show()
    Bugsy.hide()
    Bugsy.destroy()

    Bugsy.addSensitiveHeader("X-Token")
    Bugsy.removeSensitiveHeader("Cookie")
    Bugsy.addSensitiveBodyField("pin")
    Bugsy.removeSensitiveBodyField("token")

    val flags = Bugsy.flagManager.flags.value
    Bugsy.flagManager.override("k", true)
    Bugsy.flagManager.reset("k")
    Bugsy.flagManager.resetAll()
    Bugsy.flagManager.refresh()
    val value: Any? = Bugsy.flagManager.value("k")

    val client: OkHttpClient = OkHttpClient.Builder().addBugsyInterceptor().build()
}

/** `BugsyAndroidOverlay()` is @Composable in the debug artifact and a plain function in the noop: both compile here. */
@Composable
private fun overlaySurface() {
    BugsyAndroidOverlay()
}
