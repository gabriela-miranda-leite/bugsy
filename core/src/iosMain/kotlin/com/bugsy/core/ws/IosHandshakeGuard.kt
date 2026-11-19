package com.bugsy.core.ws

import platform.Foundation.NSDate
import platform.Foundation.NSLock
import platform.Foundation.timeIntervalSince1970

/** The iOS twin of the Android `HandshakeGuard`: 5 wrong tokens from one address lock it out for a minute (DEB-67). */
internal class IosHandshakeGuard(
    private val maxFailures: Int = 5,
    private val lockMs: Long = 60_000L,
    private val nowMs: () -> Long = { (NSDate().timeIntervalSince1970 * 1000).toLong() },
) {
    private class Entry(var failures: Int = 0, var locked: Boolean = false, var lockedUntilMs: Long = 0L)

    private val lock = NSLock()
    private val entries = mutableMapOf<String, Entry>()

    private inline fun <T> locked(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }

    fun isLocked(key: String): Boolean = locked {
        val entry = entries[key] ?: return@locked false
        if (entry.locked && nowMs() >= entry.lockedUntilMs) {
            entries.remove(key)
            return@locked false
        }
        entry.locked
    }

    fun recordFailure(key: String) = locked {
        if (entries.size > 1_000 && key !in entries) entries.clear()
        val entry = entries.getOrPut(key) { Entry() }
        entry.failures++
        if (entry.failures >= maxFailures) {
            entry.locked = true
            entry.lockedUntilMs = nowMs() + lockMs
        }
    }

    fun recordSuccess(key: String) = locked { entries.remove(key); Unit }
}
