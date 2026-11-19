package com.bugsy.desktop.pairing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PairingCodeManagerTest {
    private var now = 1_000L
    private var counter = 0

    private fun manager(maxFailedAttempts: Int = 5) = PairingCodeManager(
        nowMs = { now },
        nextCode = { (100_000 + counter++).toString() },
        ttlMs = 60_000,
        maxFailedAttempts = maxFailedAttempts,
    )

    @Test
    fun `starts with a six digit code that lasts a minute`() {
        val current = manager().current.value
        assertEquals("100000", current.code)
        assertEquals(61_000L, current.expiresAtMs)
    }

    @Test
    fun `generated codes are six digits with leading zeros kept`() {
        repeat(200) { assertTrue(randomCode().matches(Regex("\\d{6}"))) }
    }

    @Test
    fun `the shown code is valid until it expires`() {
        val manager = manager()
        assertEquals(CodeCheck.VALID, manager.check("100000"))
        now += 59_999
        assertEquals(CodeCheck.VALID, manager.check("100000"))
        now += 1
        assertEquals(CodeCheck.EXPIRED, manager.check("100000"))
    }

    @Test
    fun `an unknown code is invalid`() {
        assertEquals(CodeCheck.INVALID, manager().check("999999"))
        assertEquals(CodeCheck.INVALID, manager().check(null))
    }

    @Test
    fun `a rotated code is still reported as expired, not invalid`() {
        val manager = manager()
        now += 60_000
        manager.rotateIfExpired()

        assertNotEquals("100000", manager.current.value.code)
        assertEquals(CodeCheck.EXPIRED, manager.check("100000"))
        assertEquals(CodeCheck.VALID, manager.check(manager.current.value.code))
    }

    @Test
    fun `rotateIfExpired leaves a live code alone`() {
        val manager = manager()
        now += 30_000
        manager.rotateIfExpired()
        assertEquals("100000", manager.current.value.code)
    }

    @Test
    fun `consuming replaces the code so it cannot be used twice`() {
        val manager = manager()
        assertEquals(CodeCheck.VALID, manager.check("100000"))
        manager.consume()
        assertEquals("100001", manager.current.value.code)
        assertEquals(CodeCheck.INVALID, manager.check("100000"))
    }

    @Test
    fun `too many wrong guesses lock the code and rotate it`() {
        val manager = manager(maxFailedAttempts = 3)
        assertEquals(CodeCheck.INVALID, manager.check("000001"))
        assertEquals(CodeCheck.INVALID, manager.check("000002"))
        assertEquals(CodeCheck.LOCKED, manager.check("000003"))
        // The code the attacker was guessing against is gone.
        assertEquals("100001", manager.current.value.code)
        assertEquals(CodeCheck.EXPIRED, manager.check("100000"))
    }

    @Test
    fun `right answers do not count towards the lockout`() {
        val manager = manager(maxFailedAttempts = 2)
        repeat(10) { assertEquals(CodeCheck.VALID, manager.check("100000")) }
    }
}

class PairingCodeManagerSourceTest {
    private var now = 0L
    private var counter = 0
    private val manager = PairingCodeManager(
        nowMs = { now },
        nextCode = { (111_111 + counter++).toString() },
        maxFailedAttempts = 25,
        maxFailedAttemptsPerSource = 3,
        sourceLockMs = 1_000,
    )

    @Test
    fun `one address that keeps guessing is turned away without rotating the code`() {
        val shown = manager.current.value.code
        repeat(3) { manager.check("000000", source = "10.0.0.66") }

        assertEquals(CodeCheck.LOCKED, manager.check("000000", source = "10.0.0.66"))
        assertEquals(CodeCheck.LOCKED, manager.check(shown, source = "10.0.0.66"), "even the right code, from the blocked address")
        assertEquals(shown, manager.current.value.code, "the code on screen did not change")
        assertEquals(CodeCheck.VALID, manager.check(shown, source = "10.0.0.24"), "another address still pairs")
    }

    @Test
    fun `the address is let back in once the block runs out`() {
        repeat(3) { manager.check("000000", source = "10.0.0.66") }
        now = 1_001

        assertEquals(CodeCheck.VALID, manager.check(manager.current.value.code, source = "10.0.0.66"))
    }

    @Test
    fun `many addresses together still rotate the code`() {
        val first = manager.current.value.code
        repeat(25) { manager.check("000000", source = "10.0.0.$it") }

        assertNotEquals(first, manager.current.value.code)
    }
}
