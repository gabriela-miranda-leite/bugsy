package com.bugsy.core.ws

import com.bugsy.core.Bugsy
import com.bugsy.core.DebugRepository
import com.bugsy.core.MockEngine
import kotlin.random.Random

/** Default port the BugsyWsServer listens on (DEB-58); the desktop client connects to `ws://<host>:7788/bugsy`. */
const val BUGSY_WS_DEFAULT_PORT: Int = 7788

/** Shared by both platform implementations: the full state sent right after a successful handshake. */
internal fun currentBugsySnapshot(): BugsyEvent.Snapshot = BugsyEvent.Snapshot(
    requests = DebugRepository.getAll(),
    mockRules = MockEngine.rulesFlow.value,
    flags = Bugsy.flagManager.flags.value,
)

/** Shared by both platform implementations: applies a desktop-sent command to the local engines. */
internal fun applyBugsyCommand(command: BugsyCommand) {
    when (command) {
        is BugsyCommand.AddMockRule -> MockEngine.addRule(command.rule)
        is BugsyCommand.ToggleMockRule -> MockEngine.setActive(command.id, command.enabled)
        is BugsyCommand.DeleteMockRule -> MockEngine.removeRule(command.id)
        is BugsyCommand.ToggleFlag -> Bugsy.flagManager.override(command.key, command.enabled)
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
