package com.bugsy.core

/** Cryptographically secure random bytes from the OS. `kotlin.random.Random` is not: its output is predictable. */
internal expect fun secureRandomBytes(size: Int): ByteArray
