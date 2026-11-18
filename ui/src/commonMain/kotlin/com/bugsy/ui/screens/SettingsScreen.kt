package com.bugsy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.cd_copy
import bugsy.ui.generated.resources.settings_bugsy_version
import bugsy.ui.generated.resources.settings_desktop_code_connect
import bugsy.ui.generated.resources.settings_desktop_code_connecting
import bugsy.ui.generated.resources.settings_desktop_code_placeholder
import bugsy.ui.generated.resources.settings_desktop_disconnect
import bugsy.ui.generated.resources.settings_desktop_ip
import bugsy.ui.generated.resources.settings_desktop_ip_unavailable
import bugsy.ui.generated.resources.settings_desktop_or
import bugsy.ui.generated.resources.settings_desktop_scan_qr
import bugsy.ui.generated.resources.settings_desktop_scan_qr_hint
import bugsy.ui.generated.resources.settings_desktop_status_connected
import bugsy.ui.generated.resources.settings_desktop_status_not_connected
import bugsy.ui.generated.resources.settings_desktop_token
import bugsy.ui.generated.resources.settings_desktop_token_unavailable
import bugsy.ui.generated.resources.settings_reveal_default
import bugsy.ui.generated.resources.settings_reveal_hint
import bugsy.ui.generated.resources.settings_section_about
import bugsy.ui.generated.resources.settings_section_desktop
import bugsy.ui.generated.resources.settings_section_privacy
import bugsy.ui.generated.resources.settings_title
import com.bugsy.core.Bugsy
import com.bugsy.core.pairing.BugsyPairing
import com.bugsy.core.pairing.PairingResult
import com.bugsy.ui.components.CopyAffordance
import com.bugsy.ui.components.KeyValueRow
import com.bugsy.ui.components.SectionLabel
import com.bugsy.ui.components.rememberCopyFlash
import com.bugsy.ui.settings.BugsySettings
import com.bugsy.ui.settings.DesktopPairingState
import com.bugsy.ui.settings.PairingFeedback
import com.bugsy.ui.settings.pairingFeedbackText
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Preferences for whoever uses the overlay: sensitive data by default, pairing with Bugsy Desktop (DEB-64) and the
 * Bugsy version.
 *
 * @param onScanQr opens the full-screen QR scanner (owned by the overlay, since it covers the whole sheet).
 * @param wsTokenOverride, [deviceAddressOverride], [desktopConnectedOverride] bypass the real values when set, so
 * screenshot tests can pass fixed ones: the token is random per [Bugsy.initialize]/[Bugsy.restartWsServer] call,
 * and the address/connection depend on the machine running the test.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    pairing: DesktopPairingState = remember { DesktopPairingState() },
    onScanQr: () -> Unit = {},
    wsTokenOverride: String? = null,
    deviceAddressOverride: String? = null,
    desktopConnectedOverride: Boolean? = null,
) {
    val revealDefault by BugsySettings.revealSensitiveByDefault.collectAsState()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    // Bumped after Bugsy.restartWsServer() so `wsToken` below re-reads the new value — a plain property read
    // isn't observable on its own, this is what makes the UI actually reflect the new token.
    var tokenGeneration by remember { mutableIntStateOf(0) }
    val wsToken = wsTokenOverride ?: remember(tokenGeneration) { Bugsy.wsServerToken }
    val deviceAddress = deviceAddressOverride ?: remember(tokenGeneration) { BugsyPairing.deviceAddress() }
    val liveConnected by Bugsy.isDesktopConnected.collectAsState()
    val desktopConnected = desktopConnectedOverride ?: liveConnected

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = BugsySpacing.lg, vertical = BugsySpacing.xl),
        verticalArrangement = Arrangement.spacedBy(BugsySpacing.lg),
    ) {
        item { Text(stringResource(Res.string.settings_title), style = BugsyTypography.Title, color = BugsyColors.TextPrimary) }

        item { SectionLabel(stringResource(Res.string.settings_section_privacy)) }
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BugsySpacing.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { BugsySettings.setRevealSensitiveByDefault(!revealDefault) }
                        .padding(vertical = BugsySpacing.clickableRowVertical),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BugsySpacing.xs)) {
                        Text(stringResource(Res.string.settings_reveal_default), style = BugsyTypography.Body, color = BugsyColors.TextPrimary)
                        Text(stringResource(Res.string.settings_reveal_hint), style = BugsyTypography.Caption, color = BugsyColors.TextDisabled)
                    }
                    Switch(
                        checked = revealDefault,
                        onCheckedChange = { BugsySettings.setRevealSensitiveByDefault(it) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = BugsyColors.SwitchOn,
                            checkedThumbColor = BugsyColors.TextPrimary,
                            checkedBorderColor = BugsyColors.SwitchOn,
                            uncheckedTrackColor = BugsyColors.SwitchOff,
                            uncheckedThumbColor = BugsyColors.TextPrimary,
                            uncheckedBorderColor = BugsyColors.SwitchOff,
                        ),
                    )
                }
                HorizontalDivider(thickness = BugsySpacing.rowDividerThickness, color = BugsyColors.BorderHeader)
            }
        }

        item { SectionLabel(stringResource(Res.string.settings_section_desktop)) }
        if (wsToken != null) {
            item { PairingCard(pairing, onScanQr = onScanQr, onConnectWithCode = { scope.launch { pairing.connectWithCode() } }) }
            item {
                Column {
                    CopyableRow(
                        label = stringResource(Res.string.settings_desktop_ip),
                        value = deviceAddress?.replace(":", " : ") ?: stringResource(Res.string.settings_desktop_ip_unavailable),
                        onCopy = deviceAddress?.let { { clipboard.setText(AnnotatedString(it)) } },
                    )
                    CopyableRow(
                        label = stringResource(Res.string.settings_desktop_token),
                        value = wsToken,
                        onCopy = { clipboard.setText(AnnotatedString(wsToken)) },
                    )
                }
            }
            item { ConnectionStatusPill(connected = desktopConnected) }
            item {
                Text(
                    stringResource(Res.string.settings_desktop_disconnect),
                    style = BugsyTypography.Body,
                    color = BugsyColors.DeleteText,
                    modifier = Modifier.clickable {
                        Bugsy.restartWsServer()
                        tokenGeneration++
                    },
                )
            }
        } else {
            item { KeyValueRow(stringResource(Res.string.settings_desktop_token), stringResource(Res.string.settings_desktop_token_unavailable)) }
        }

        item { HorizontalDivider(thickness = BugsySpacing.rowDividerThickness, color = BugsyColors.BorderHeader) }
        item { SectionLabel(stringResource(Res.string.settings_section_about)) }
        item { KeyValueRow(stringResource(Res.string.settings_bugsy_version), Bugsy.version) }
    }
}

/** The "Scan QR code on desktop" button, or a 6-digit code to type, plus the outcome of the last attempt. */
@Composable
private fun PairingCard(pairing: DesktopPairingState, onScanQr: () -> Unit, onConnectWithCode: () -> Unit) {
    val shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge)
    val focus = remember { FocusRequester() }
    LaunchedEffect(pairing.focusCodeRequests) {
        if (pairing.focusCodeRequests > 0) runCatching { focus.requestFocus() }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(BugsySpacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .background(BugsyColors.SurfaceCard, shape)
            .border(BugsySpacing.strokeBorder, BugsyColors.Border, shape)
            .padding(BugsySpacing.lg),
    ) {
        Button(
            onClick = onScanQr,
            colors = ButtonDefaults.buttonColors(containerColor = BugsyColors.Brand, contentColor = BugsyColors.TextPrimary),
            shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge),
            modifier = Modifier.fillMaxWidth().height(BugsySpacing.resetAllButtonHeight),
        ) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(BugsySpacing.iconSizeLarge))
            Spacer(Modifier.width(BugsySpacing.md))
            Text(stringResource(Res.string.settings_desktop_scan_qr), style = BugsyTypography.Title)
        }
        Text(
            stringResource(Res.string.settings_desktop_scan_qr_hint),
            style = BugsyTypography.Caption,
            color = BugsyColors.TextDisabled,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsySpacing.md)) {
            HorizontalDivider(Modifier.weight(1f), thickness = BugsySpacing.strokeBorder, color = BugsyColors.Border)
            Text(stringResource(Res.string.settings_desktop_or), style = BugsyTypography.Code, color = BugsyColors.TextDisabled)
            HorizontalDivider(Modifier.weight(1f), thickness = BugsySpacing.strokeBorder, color = BugsyColors.Border)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            val inputShape = RoundedCornerShape(BugsySpacing.cornerCard)
            BasicTextField(
                value = pairing.code,
                onValueChange = pairing::onCodeChange,
                textStyle = BugsyTypography.Code.copy(color = BugsyColors.TextPrimary),
                singleLine = true,
                cursorBrush = SolidColor(BugsyColors.Brand),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { if (pairing.code.length == com.bugsy.core.pairing.PAIRING_CODE_LENGTH) onConnectWithCode() }),
                decorationBox = { inner ->
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BugsySpacing.controlHeight)
                            .border(BugsySpacing.strokeBorder, BugsyColors.Border, inputShape)
                            .padding(horizontal = BugsySpacing.md),
                    ) {
                        if (pairing.code.isEmpty()) {
                            Text(stringResource(Res.string.settings_desktop_code_placeholder), style = BugsyTypography.Code, color = BugsyColors.TextDisabled)
                        }
                        inner()
                    }
                },
                modifier = Modifier.weight(1f).focusRequester(focus),
            )
            val canConnect = pairing.code.length == com.bugsy.core.pairing.PAIRING_CODE_LENGTH && !pairing.isConnecting
            Button(
                onClick = onConnectWithCode,
                enabled = canConnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BugsyColors.SurfaceElevated,
                    contentColor = BugsyColors.TextPrimary,
                    disabledContainerColor = BugsyColors.SurfaceElevated,
                    disabledContentColor = BugsyColors.TextPrimary,
                ),
                shape = inputShape,
                modifier = Modifier.height(BugsySpacing.controlHeight),
            ) {
                Text(
                    stringResource(if (pairing.isConnecting) Res.string.settings_desktop_code_connecting else Res.string.settings_desktop_code_connect),
                    style = BugsyTypography.Body,
                )
            }
        }

        val feedback = pairing.feedback
        if (feedback is PairingFeedback.Done) {
            Text(
                pairingFeedbackText(feedback.result),
                style = BugsyTypography.Caption,
                color = if (feedback.result is PairingResult.Success) BugsyColors.Status2xx else BugsyColors.DeleteText,
            )
        }
    }
}

/** `Label    value    [copy]`, the whole row tapping to copy; the icon turns into a check for a moment. */
@Composable
private fun CopyableRow(label: String, value: String, onCopy: (() -> Unit)?) {
    val flash = rememberCopyFlash()
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onCopy != null) Modifier.clickable { onCopy(); flash.flash() } else Modifier)
                .padding(vertical = BugsySpacing.clickableRowVertical),
        ) {
            Text(label, style = BugsyTypography.Body, color = BugsyColors.TextDisabled, modifier = Modifier.width(130.dp))
            Text(
                value,
                style = BugsyTypography.Code.copy(color = if (flash.copied) BugsyColors.Status2xx else BugsyColors.TextSecondary),
                modifier = Modifier.weight(1f),
            )
            if (onCopy != null) CopyAffordance(copied = flash.copied)
        }
        HorizontalDivider(thickness = BugsySpacing.rowDividerThickness, color = BugsyColors.BorderHeader)
    }
}

@Composable
private fun ConnectionStatusPill(connected: Boolean) {
    val shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BugsySpacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .background(BugsyColors.SurfaceCard, shape)
            .border(BugsySpacing.strokeBorder, BugsyColors.Border, shape)
            .padding(horizontal = BugsySpacing.md, vertical = BugsySpacing.md),
    ) {
        Box(Modifier.size(BugsySpacing.iconSizeLarge).background(if (connected) BugsyColors.Status2xx.copy(alpha = 0.2f) else BugsyColors.SurfaceElevated, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(BugsySpacing.sm + BugsySpacing.xs).background(if (connected) BugsyColors.Status2xx else BugsyColors.TextDisabled, CircleShape))
        }
        Text(
            stringResource(if (connected) Res.string.settings_desktop_status_connected else Res.string.settings_desktop_status_not_connected),
            style = BugsyTypography.Body,
            color = if (connected) BugsyColors.TextPrimary else BugsyColors.TextSecondary,
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    BugsyTheme { Box(Modifier.background(BugsyColors.Surface)) { SettingsScreen() } }
}
