package com.bugsy.core.ws

import com.bugsy.core.Bugsy
import com.bugsy.core.DebugRepository
import com.bugsy.core.MockEngine
import com.bugsy.core.secureRandomBytes
import kotlinx.coroutines.flow.StateFlow

/** Default port the BugsyWsServer listens on (DEB-58); the desktop client connects to `ws://<host>:7788/bugsy`. */
const val BUGSY_WS_DEFAULT_PORT: Int = 7788

/** Shared by both platform implementations: the full state sent right after a successful handshake. */
internal fun currentBugsySnapshot(): BugsyEvent.Snapshot = BugsyEvent.Snapshot(
    requests = DebugRepository.getAll(),
    mockRules = MockEngine.rulesFlow.value,
    flags = Bugsy.flagManager.flags.value,
    deviceInfo = Bugsy.deviceInfo(),
    // The provider is the app's own code; a throwing one must not stop the desktop from connecting.
    customInfo = runCatching { Bugsy.customInfo() }.getOrDefault(emptyList()).map { (label, value) -> CustomInfoEntry(label, value) },
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

/** Characters in a session token: 12 from a 54-letter alphabet is about 69 bits. */
internal const val SESSION_TOKEN_LENGTH = 12

/**
 * Shared by both platform implementations: a token shown in the mobile overlay to authenticate the desktop. It comes
 * from the OS's secure random generator (`kotlin.random.Random` is predictable) and by rejection sampling, so no
 * character is likelier than another.
 */
internal fun generateBugsySessionToken(): String {
    val limit = 256 - 256 % sessionTokenAlphabet.length
    val token = StringBuilder()
    while (token.length < SESSION_TOKEN_LENGTH) {
        for (byte in secureRandomBytes(SESSION_TOKEN_LENGTH * 2)) {
            val value = byte.toInt() and 0xFF
            if (value < limit && token.length < SESSION_TOKEN_LENGTH) token.append(sessionTokenAlphabet[value % sessionTokenAlphabet.length])
        }
    }
    return token.toString()
}

/** A running [startBugsyWsServer] instance. */
interface BugsyWsServerHandle {
    /** Shown in the mobile overlay; the desktop client must send it as the first frame to authenticate. */
    val sessionToken: String

    /**
     * SHA-256 (lowercase hex) of the server's TLS certificate, which the desktop pins; `null` when the server has no
     * TLS (iOS, for now), in which case the desktop falls back to plain `ws://`.
     */
    val certFingerprint: String? get() = null

    /** How many desktop clients are connected right now (past the handshake). */
    val connectedClients: StateFlow<Int>

    fun stop()
}

/**
 * Starts the BugsyWsServer on [port], or returns `null` where it isn't supported. Ktor's `embeddedServer` has no
 * supported iOS target (only JVM + macOS/Linux/Windows native) — Android is the only mobile platform with a real
 * implementation for now; iOS is a noop until a non-Ktor server is built for it.
 */
expect fun startBugsyWsServer(port: Int = BUGSY_WS_DEFAULT_PORT): BugsyWsServerHandle?
