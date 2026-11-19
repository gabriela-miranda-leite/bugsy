package com.bugsy.core.ws

/**
 * Slows down token guessing on the BugsyWsServer (DEB-67): after [maxFailures] wrong tokens from one address, that
 * address is turned away for [lockMs] without even reading its handshake. A correct token clears the count.
 */
internal class HandshakeGuard(
    private val maxFailures: Int = 5,
    private val lockMs: Long = 60_000L,
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private class Entry(var failures: Int = 0, var lockedUntilMs: Long = 0L, var locked: Boolean = false)

    private val entries = mutableMapOf<String, Entry>()

    fun isLocked(key: String): Boolean = synchronized(entries) {
        val entry = entries[key] ?: return false
        if (entry.locked && nowMs() >= entry.lockedUntilMs) {
            entries.remove(key)
            return false
        }
        entry.locked
    }

    fun recordFailure(key: String) = synchronized(entries) {
        // A bounded table: an attacker rotating addresses must not be able to grow it without limit.
        if (entries.size > MAX_TRACKED && key !in entries) entries.clear()
        val entry = entries.getOrPut(key) { Entry() }
        entry.failures++
        if (entry.failures >= maxFailures) {
            entry.locked = true
            entry.lockedUntilMs = nowMs() + lockMs
        }
    }

    fun recordSuccess(key: String) = synchronized(entries) { entries.remove(key); Unit }

    private companion object {
        const val MAX_TRACKED = 1_000
    }
}
