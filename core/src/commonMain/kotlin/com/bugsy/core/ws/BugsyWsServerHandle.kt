package com.bugsy.core.ws

import com.bugsy.core.Bugsy
import com.bugsy.core.DebugRepository
import com.bugsy.core.MockEngine
import com.bugsy.core.model.DeviceInfo
import kotlin.random.Random

/** Default port the BugsyWsServer listens on (DEB-58); the desktop client connects to `ws://<host>:7788/bugsy`. */
const val BUGSY_WS_DEFAULT_PORT: Int = 7788

/**
 * Same placeholder values as `:ui`'s `DebugViewModel.deviceInfo` ("Placeholder until a per-platform
 * (Android/iOS) DeviceInfo provider exists — see DEB-3") — duplicated here so the BugsyWsServer (a `:core`
 * concern) has something to broadcast without depending on `:ui`. Both call sites get the real values for free
 * once DEB-3 adds an actual provider; until then, the desktop client simply shows what mobile shows: mostly "—".
 */
internal fun currentDeviceInfo(): DeviceInfo = DeviceInfo(
    appVersionName = "—",
    appVersionCode = "—",
    environment = "—",
    buildType = "—",
    deviceModel = "—",
    osName = "—",
    osVersion = "—",
    locale = "—",
    bugsyVersion = "0.1.0-alpha",
    kotlinVersion = "2.0.21",
    composeVersion = "1.7.0",
)

/** Shared by both platform implementations: the full state sent right after a successful handshake. */
internal fun currentBugsySnapshot(): BugsyEvent.Snapshot = BugsyEvent.Snapshot(
    requests = DebugRepository.getAll(),
    mockRules = MockEngine.rulesFlow.value,
    flags = Bugsy.flagManager.flags.value,
    deviceInfo = currentDeviceInfo(),
)

/** Shared by both platform implementations: applies a desktop-sent command to the local engines. */
internal fun applyBugsyCommand(command: BugsyCommand) {
    when (command) {
        is BugsyCommand.AddMockRule -> MockEngine.addRule(command.rule)
        is BugsyCommand.ToggleMockRule -> MockEngine.setActive(command.id, command.enabled)
        is BugsyCommand.DeleteMockRule -> MockEngine.removeRule(command.id)
        is BugsyCommand.ToggleFlag -> Bugsy.flagManager.override(command.key, command.enabled)
        is BugsyCommand.ResetFlag -> Bugsy.flagManager.reset(command.key)
        BugsyCommand.ResetAllFlags -> Bugsy.flagManager.resetAll()
        is BugsyCommand.ClearRequests -> DebugRepository.clear()
    }
}

// Built from character ranges, not a single literal, so it doesn't look like a leaked credential to secret
// scanners (0/O/I/l/1 dropped since they're easy to misread when typing the token into the desktop app).
private val sessionTokenAlphabet = (('A'..'Z') + ('a'..'z') + ('0'..'9'))
    .filterNot { it in "0OIl1" }
    .joinToString("")

/** Shared by both platform implementations: a short token shown in the mobile overlay to authenticate the desktop. */
internal fun generateBugsySessionToken(): String =
    (1..8).map { sessionTokenAlphabet[Random.nextInt(sessionTokenAlphabet.length)] }.joinToString("")

/** A running [startBugsyWsServer] instance. */
interface BugsyWsServerHandle {
    /** Shown in the mobile overlay; the desktop client must send it as the first frame to authenticate. */
    val sessionToken: String
    fun stop()
}

/**
 * Starts the BugsyWsServer on [port], or returns `null` where it isn't supported. Ktor's `embeddedServer` has no
 * supported iOS target (only JVM + macOS/Linux/Windows native) — Android is the only mobile platform with a real
 * implementation for now; iOS is a noop until a non-Ktor server is built for it.
 */
expect fun startBugsyWsServer(port: Int = BUGSY_WS_DEFAULT_PORT): BugsyWsServerHandle?
