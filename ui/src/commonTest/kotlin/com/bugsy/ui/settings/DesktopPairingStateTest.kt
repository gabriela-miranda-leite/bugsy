package com.bugsy.ui.settings

import com.bugsy.core.pairing.PairingResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopPairingStateTest {
    @Test
    fun `the code field only keeps digits and at most six of them`() {
        val state = DesktopPairingState()

        state.onCodeChange("48a2-91 3999")

        assertEquals("482913", state.code)
    }

    @Test
    fun `typing clears the previous error`() {
        val state = DesktopPairingState(initialCode = "482913", initialFeedback = PairingFeedback.Done(PairingResult.InvalidCode))

        state.onCodeChange("48291")

        assertEquals(null, state.feedback)
    }

    @Test
    fun `typing does not interrupt a connection in progress`() {
        val state = DesktopPairingState(initialCode = "482913", initialFeedback = PairingFeedback.Connecting)

        state.onCodeChange("48291")

        assertTrue(state.isConnecting)
    }

    @Test
    fun `connecting without a running server reports it and keeps the code`() = runTest {
        val state = DesktopPairingState(initialCode = "482913")

        state.connectWithCode()

        assertEquals(PairingFeedback.Done(PairingResult.ServerUnavailable), state.feedback)
        assertEquals("482913", state.code)
        assertFalse(state.isConnecting)
    }

    @Test
    fun `a qr scan without a running server reports it`() = runTest {
        val state = DesktopPairingState()

        state.connectWithQr("bugsy://pair?host=192.168.0.10&port=7790&code=482913")

        assertEquals(PairingFeedback.Done(PairingResult.ServerUnavailable), state.feedback)
    }

    @Test
    fun `asking for focus is counted so every request is seen`() {
        val state = DesktopPairingState()
        state.requestCodeFocus()
        state.requestCodeFocus()
        assertEquals(2, state.focusCodeRequests)
    }
}
