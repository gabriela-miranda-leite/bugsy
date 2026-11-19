package com.bugsy.desktop.pairing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.SecureRandom

/** The code currently shown on screen and when it stops being accepted. */
data class PairingCode(val code: String, val expiresAtMs: Long, val secret: String = "")

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
    private val maxFailedAttempts: Int = 5,
) {
    private val lock = Any()
    private var failedAttempts = 0
    private var lastRetiredCode: String? = null

    private val _current = MutableStateFlow(PairingCode(nextCode(), nowMs() + ttlMs, nextSecret()))
    val current: StateFlow<PairingCode> = _current

    /** Checks a code a device sent. Wrong guesses count towards the lockout; right ones don't. */
    fun check(code: String?): CodeCheck = synchronized(lock) {
        val now = nowMs()
        val shown = _current.value
        when {
            code != null && code == shown.code && now < shown.expiresAtMs -> CodeCheck.VALID
            code != null && code == shown.code -> CodeCheck.EXPIRED
            code != null && code == lastRetiredCode -> CodeCheck.EXPIRED
            else -> {
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

    /** A pairing succeeded: the code is single-use, so replace it. */
    fun consume() = synchronized(lock) { rotateLocked(retire = false) }

    /** Called periodically: swaps in a fresh code once the shown one expired. */
    fun rotateIfExpired() = synchronized(lock) {
        if (nowMs() >= _current.value.expiresAtMs) rotateLocked()
    }

    private fun rotateLocked(retire: Boolean = true) {
        if (retire) lastRetiredCode = _current.value.code
        failedAttempts = 0
        _current.value = PairingCode(nextCode(), nowMs() + ttlMs, nextSecret())
    }
}

private val secureRandom = SecureRandom()

internal fun randomCode(): String = secureRandom.nextInt(1_000_000).toString().padStart(PAIRING_CODE_LENGTH, '0')
