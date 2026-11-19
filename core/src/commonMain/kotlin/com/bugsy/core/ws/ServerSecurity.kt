package com.bugsy.core.ws

import com.bugsy.core.model.DebugRequest

/**
 * Compares two strings without stopping at the first different character, so the time it takes does not tell
 * a caller how much of a guessed token was right.
 */
internal fun constantTimeEquals(a: String, b: String): Boolean {
    var diff = a.length xor b.length
    val length = maxOf(a.length, b.length)
    for (i in 0 until length) {
        val x = if (i < a.length) a[i].code else 0
        val y = if (i < b.length) b[i].code else 0
        diff = diff or (x xor y)
    }
    return diff == 0
}

/**
 * For a connection that is not encrypted (an iOS phone has no TLS yet): the real values behind masked secrets
 * (`allowRevealingSecrets`) must not cross the network in clear, so they are left out and the desktop only ever
 * gets the masked text. The token is still exposed on such a connection, but the secrets of the app are not.
 */
internal fun BugsyEvent.withoutSecrets(): BugsyEvent = when (this) {
    is BugsyEvent.Snapshot -> copy(requests = requests.map { it.withoutSecrets() })
    is BugsyEvent.RequestRecorded -> copy(request = request.withoutSecrets())
    else -> this
}

private fun DebugRequest.withoutSecrets(): DebugRequest = if (secrets == null) this else copy(secrets = null)
