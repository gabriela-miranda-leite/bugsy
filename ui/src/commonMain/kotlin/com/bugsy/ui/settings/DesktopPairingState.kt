package com.bugsy.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.pairing_error_desktop_not_found
import bugsy.ui.generated.resources.pairing_error_expired_code
import bugsy.ui.generated.resources.pairing_error_invalid_code
import bugsy.ui.generated.resources.pairing_error_invalid_qr
import bugsy.ui.generated.resources.pairing_error_no_network
import bugsy.ui.generated.resources.pairing_error_not_verified
import bugsy.ui.generated.resources.pairing_error_server_unavailable
import bugsy.ui.generated.resources.pairing_error_too_many_attempts
import bugsy.ui.generated.resources.pairing_paired
import bugsy.ui.generated.resources.pairing_paired_unnamed
import com.bugsy.core.pairing.BugsyPairing
import com.bugsy.core.pairing.PAIRING_CODE_LENGTH
import com.bugsy.core.pairing.PairingResult
import org.jetbrains.compose.resources.stringResource

/** What the Desktop card in Settings shows under the code field. */
sealed interface PairingFeedback {
    data object Connecting : PairingFeedback
    data class Done(val result: PairingResult) : PairingFeedback
}

/**
 * State of the "Bugsy Desktop" pairing UI (DEB-64), hoisted out of the Settings screen because the QR scanner is a
 * separate full-screen page that reports its outcome back into the same card.
 */
@Stable
class DesktopPairingState(initialCode: String = "", initialFeedback: PairingFeedback? = null) {
    var code by mutableStateOf(initialCode)
        private set

    var feedback by mutableStateOf(initialFeedback)
        private set

    /** Bumped when something asks for the code field to take focus ("Enter code manually" in the scanner). */
    var focusCodeRequests by mutableIntStateOf(0)
        private set

    val isConnecting: Boolean get() = feedback == PairingFeedback.Connecting

    fun onCodeChange(value: String) {
        code = value.filter(Char::isDigit).take(PAIRING_CODE_LENGTH)
        if (feedback is PairingFeedback.Done) feedback = null
    }

    fun requestCodeFocus() {
        focusCodeRequests++
    }

    suspend fun connectWithCode() {
        if (isConnecting) return
        feedback = PairingFeedback.Connecting
        val result = BugsyPairing.pairWithCode(code)
        finish(result)
    }

    suspend fun connectWithQr(qrText: String) {
        if (isConnecting) return
        feedback = PairingFeedback.Connecting
        finish(BugsyPairing.pairWithQr(qrText))
    }

    private fun finish(result: PairingResult) {
        feedback = PairingFeedback.Done(result)
        if (result is PairingResult.Success) code = ""
    }
}

@Composable
internal fun pairingFeedbackText(result: PairingResult): String = when (result) {
    is PairingResult.Success ->
        result.desktopName?.let { stringResource(Res.string.pairing_paired, it) } ?: stringResource(Res.string.pairing_paired_unnamed)
    PairingResult.InvalidCode -> stringResource(Res.string.pairing_error_invalid_code)
    PairingResult.ExpiredCode -> stringResource(Res.string.pairing_error_expired_code)
    PairingResult.TooManyAttempts -> stringResource(Res.string.pairing_error_too_many_attempts)
    PairingResult.DesktopNotFound -> stringResource(Res.string.pairing_error_desktop_not_found)
    PairingResult.InvalidQr -> stringResource(Res.string.pairing_error_invalid_qr)
    PairingResult.NoNetwork -> stringResource(Res.string.pairing_error_no_network)
    PairingResult.ServerUnavailable -> stringResource(Res.string.pairing_error_server_unavailable)
    PairingResult.NotVerified -> stringResource(Res.string.pairing_error_not_verified)
}
