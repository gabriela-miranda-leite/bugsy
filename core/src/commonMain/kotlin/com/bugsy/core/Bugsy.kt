package com.bugsy.core

import com.bugsy.core.model.DeviceInfo
import com.bugsy.core.pairing.BugsyPairing
import com.bugsy.core.ws.BugsyWsServerHandle
import com.bugsy.core.ws.startBugsyWsServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object Bugsy {
    val version: String = BuildKonfig.VERSION

    private val _isVisible = MutableStateFlow(false)
    val isVisible: StateFlow<Boolean> = _isVisible

    private var _flagManager = FlagManager { emptyList() }
    val flagManager: FlagManager get() = _flagManager

    private var customInfoProvider: () -> List<Pair<String, String>> = { emptyList() }

    /** Read again on every call — see [BugsyConfig.customInfoProvider]. */
    fun customInfo(): List<Pair<String, String>> = customInfoProvider()

    private var environment: String? = null

    /**
     * What the Device tab shows (and the BugsyWsServer sends to the desktop): read from the platform on every
     * call, so nothing here is cached or made up. Fields that can't be read stay "—".
     */
    fun deviceInfo(): DeviceInfo = buildDeviceInfo(readPlatformDeviceInfo(), environment)

    private var debugInterceptor: DebugInterceptor? = null
    private var shakeDetector: ShakeDetector? = null
    private var floatingButtonController: FloatingButtonController? = null
    private var notificationTrigger: NotificationTrigger? = null
    private var wsServer: BugsyWsServerHandle? = null
    private val wsScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var connectionWatchJob: Job? = null

    private val _isDesktopConnected = MutableStateFlow(false)

    /** Whether a Bugsy Desktop client is connected to this device's BugsyWsServer right now (DEB-64). */
    val isDesktopConnected: StateFlow<Boolean> = _isDesktopConnected

    /**
     * Session token the desktop client (DEB-58) must send to connect to this device's BugsyWsServer, or `null`
     * where the server isn't running (not yet [initialize]d, or on a platform without a real server — see
     * [startBugsyWsServer]). Shown in the mobile overlay for the dev to type/scan into the desktop app.
     */
    val wsServerToken: String? get() = wsServer?.sessionToken

    /**
     * Stops and restarts the BugsyWsServer, generating a new [wsServerToken] and dropping any desktop client
     * currently connected with the old one — a manual "disconnect" since there's no per-connection control.
     * A noop where the server isn't running (not yet [initialize]d, or no real server on this platform).
     */
    fun restartWsServer() {
        val current = wsServer ?: return
        current.stop()
        startWsServer()
    }

    private fun startWsServer() {
        connectionWatchJob?.cancel()
        val server = startBugsyWsServer()
        wsServer = server
        _isDesktopConnected.value = false
        connectionWatchJob = server?.let { handle ->
            wsScope.launch { handle.connectedClients.collect { _isDesktopConnected.value = it > 0 } }
        }
    }

    fun initialize(config: BugsyConfig = BugsyConfig()) {
        destroy()
        _flagManager = FlagManager(config.flagsProvider)
        customInfoProvider = config.customInfoProvider
        environment = config.environment
        CaptureSettings.configure(allowRevealingSecrets = config.allowRevealingSecrets)
        DebugRepository.slowRequestThresholdMs = config.slowRequestThresholdMs
        debugInterceptor = DebugInterceptor.create().also { it.attach() }
        startWsServer()
        BugsyPairing.start()

        when (config.trigger) {
            Trigger.Shake -> {
                shakeDetector = ShakeDetector.create(config.shakeThresholdG, onShake = ::show)
                    .also { it.start() }
            }
            Trigger.FloatingButton -> {
                floatingButtonController = FloatingButtonController.create(onTap = ::show)
                    .also { it.start() }
            }
            Trigger.Notification -> {
                notificationTrigger = NotificationTrigger.create(onTap = ::show)
                    .also { it.start() }
            }
        }
    }

    fun destroy() {
        BugsyPairing.stop()
        connectionWatchJob?.cancel()
        connectionWatchJob = null
        _isDesktopConnected.value = false
        wsServer?.stop()
        wsServer = null
        debugInterceptor?.detach()
        debugInterceptor = null
        shakeDetector?.stop()
        shakeDetector = null
        floatingButtonController?.stop()
        floatingButtonController = null
        notificationTrigger?.stop()
        notificationTrigger = null
    }

    /**
     * Starts masking the value of [name] (case-insensitive) in requests captured from this point on.
     * Already-captured requests are not altered.
     */
    fun addSensitiveHeader(name: String) = HeaderRedactor.add(name)

    /**
     * Stops masking [name], including one of the defaults (`Authorization`, `Cookie`, `X-Api-Key`).
     * Warning: removing a credential header exposes the real value in the UI.
     */
    fun removeSensitiveHeader(name: String) = HeaderRedactor.remove(name)

    /**
     * Starts masking the value of field [name] (case-insensitive, at any JSON depth or as `field=value`)
     * in bodies captured from this point on.
     */
    fun addSensitiveBodyField(name: String) = BodyFieldMasker.add(name)

    /**
     * Stops masking [name], including one of the defaults (`password`, `cpf`, `token`, `secret`).
     * Warning: removing a credential field exposes the real value in the UI.
     */
    fun removeSensitiveBodyField(name: String) = BodyFieldMasker.remove(name)

    fun show() {
        _isVisible.value = true
    }

    fun hide() {
        _isVisible.value = false
    }
}
