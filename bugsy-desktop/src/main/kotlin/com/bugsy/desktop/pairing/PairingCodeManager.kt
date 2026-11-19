package com.bugsy.desktop.pairing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.SecureRandom

/** The code currently shown on screen and when it stops being accepted. */
data class PairingCode(val code: String, val expiresAtMs: Long, val secret: String = "", val publicKey: String = "")

enum class CodeCheck { VALID, INVALID, EXPIRED, LOCKED }

/**
 * Owns the 6-digit pairing code (DEB-64): 60s lifetime, single use, and a lockout against guessing — a million
 * codes is few enough to brute-force over UDP in a minute, so [maxFailedAttempts] wrong tries rotate the code.
 */
class PairingCodeManager(
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val nextCode: () -> String = ::randomCode,
    private val nextSecret: () -> String = ::randomSecret,
    private val ttlMs: Long = 60_000L,
    /** Wrong guesses, from anyone, that rotate the code: bounds a guesser that changes address on every try. */
    private val maxFailedAttempts: Int = 25,
    /** Wrong guesses from one address before that address is ignored for [sourceLockMs]. */
    private val maxFailedAttemptsPerSource: Int = 5,
    private val sourceLockMs: Long = 60_000L,
) {
    private val lock = Any()
    private var failedAttempts = 0
    private class SourceState(var failures: Int = 0, var blockedUntilMs: Long = 0L)
    private val sources = mutableMapOf<String, SourceState>()
    private var lastRetiredCode: String? = null

    /** The ECDH key pair that goes with the code on screen; replaced whenever the code is. */
    private var ecdh = newEcdhKeyPair()

    private val _current = MutableStateFlow(PairingCode(nextCode(), nowMs() + ttlMs, nextSecret(), ecdh.publicKeyBase64()))
    val current: StateFlow<PairingCode> = _current

    /**
     * Checks a code a device sent. Wrong guesses count towards the lockout; right ones don't. With a [source], one
     * address that keeps guessing is turned away on its own (the code does not rotate for it), so someone on the
     * network spamming wrong codes can't keep the code on screen from ever being usable (DEB-67).
     */
    fun check(code: String?, source: String? = null): CodeCheck = synchronized(lock) {
        val now = nowMs()
        val shown = _current.value
        val state = source?.let { sources[it] }
        if (state != null && now < state.blockedUntilMs) return CodeCheck.LOCKED
        when {
            code != null && code == shown.code && now < shown.expiresAtMs -> CodeCheck.VALID
            code != null && code == shown.code -> CodeCheck.EXPIRED
            code != null && code == lastRetiredCode -> CodeCheck.EXPIRED
            else -> {
                if (source != null) {
                    if (sources.size > MAX_TRACKED_SOURCES && source !in sources) sources.clear()
                    val entry = sources.getOrPut(source) { SourceState() }
                    entry.failures++
                    if (entry.failures >= maxFailedAttemptsPerSource) {
                        entry.failures = 0
                        entry.blockedUntilMs = now + sourceLockMs
                    }
                }
                failedAttempts++
                if (failedAttempts >= maxFailedAttempts) {
                    rotateLocked()
                    CodeCheck.LOCKED
                } else {
                    CodeCheck.INVALID
                }
            }
        }
    }

    /** The QR secret that goes with [code] while it is the one on screen, `null` once it rotated. */
    fun secretFor(code: String): String? = synchronized(lock) { _current.value.takeIf { it.code == code }?.secret }

    /** The one-time ECDH key pair for [code] while it is the one on screen, `null` once it rotated. */
    internal fun ecdhFor(code: String): java.security.KeyPair? = synchronized(lock) { ecdh.takeIf { _current.value.code == code } }

    /** A pairing succeeded: the code is single-use, so replace it. */
    fun consume() = synchronized(lock) { rotateLocked(retire = false) }

    /** Called periodically: swaps in a fresh code once the shown one expired. */
    fun rotateIfExpired() = synchronized(lock) {
        if (nowMs() >= _current.value.expiresAtMs) rotateLocked()
    }

    private fun rotateLocked(retire: Boolean = true) {
        if (retire) lastRetiredCode = _current.value.code
        failedAttempts = 0
        ecdh = newEcdhKeyPair()
        _current.value = PairingCode(nextCode(), nowMs() + ttlMs, nextSecret(), ecdh.publicKeyBase64())
    }
}

private const val MAX_TRACKED_SOURCES = 1_000

private val secureRandom = SecureRandom()

internal fun randomCode(): String = secureRandom.nextInt(1_000_000).toString().padStart(PAIRING_CODE_LENGTH, '0')
