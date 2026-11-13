package com.bugsy.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object Bugsy {
    val version: String = BuildKonfig.VERSION

    private val _isVisible = MutableStateFlow(false)
    val isVisible: StateFlow<Boolean> = _isVisible

    private var _flagManager = FlagManager { emptyList() }
    val flagManager: FlagManager get() = _flagManager

    private var debugInterceptor: DebugInterceptor? = null
    private var shakeDetector: ShakeDetector? = null
    private var floatingButtonController: FloatingButtonController? = null

    fun initialize(config: BugsyConfig = BugsyConfig()) {
        destroy()
        _flagManager = FlagManager(config.flagsProvider)
        debugInterceptor = DebugInterceptor.create().also { it.attach() }

        when (config.trigger) {
            Trigger.Shake -> {
                shakeDetector = ShakeDetector.create(config.shakeThresholdG, onShake = ::show)
                    .also { it.start() }
            }
            Trigger.FloatingButton -> {
                floatingButtonController = FloatingButtonController.create(onTap = ::show)
                    .also { it.start() }
            }
        }
    }

    fun destroy() {
        debugInterceptor?.detach()
        debugInterceptor = null
        shakeDetector?.stop()
        shakeDetector = null
        floatingButtonController?.stop()
        floatingButtonController = null
    }

    /**
     * Passa a mascarar o valor de [name] (case-insensitive) nas requests capturadas a partir deste ponto.
     * Requests já capturadas não são alteradas.
     */
    fun addSensitiveHeader(name: String) = HeaderRedactor.add(name)

    /**
     * Deixa de mascarar [name], inclusive um dos padrões (`Authorization`, `Cookie`, `X-Api-Key`).
     * Atenção: remover um header de credenciais expõe o valor real na UI.
     */
    fun removeSensitiveHeader(name: String) = HeaderRedactor.remove(name)

    fun show() {
        _isVisible.value = true
    }

    fun hide() {
        _isVisible.value = false
    }
}
