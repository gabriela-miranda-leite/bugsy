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
 * Paridade de API: este arquivo usa **toda** a API pública do Bugsy e compila nas duas variantes do sample
 * (debug contra `:ui`, release contra `:bugsy-noop`). Se o artefato de debug ganhar ou mudar uma API e o noop não a
 * acompanhar, o `assembleRelease` deixa de compilar aqui. Nunca é executado. Ao criar uma API pública nova, adicione-a.
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

/** `BugsyAndroidOverlay()` é @Composable no artefato de debug e uma função comum no noop: ambos compilam aqui. */
@Composable
private fun overlaySurface() {
    BugsyAndroidOverlay()
}
