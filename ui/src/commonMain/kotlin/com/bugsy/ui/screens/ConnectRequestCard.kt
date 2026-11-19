package com.bugsy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.connect_request_allow
import bugsy.ui.generated.resources.connect_request_body
import bugsy.ui.generated.resources.connect_request_deny
import bugsy.ui.generated.resources.connect_request_title
import bugsy.ui.generated.resources.connect_request_verify
import bugsy.ui.generated.resources.verify_code_body
import bugsy.ui.generated.resources.verify_code_different
import bugsy.ui.generated.resources.verify_code_match
import bugsy.ui.generated.resources.verify_code_title
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Asks the user to approve a desktop that picked this device from its "Devices on your network" list. Handing the
 * session token to whoever asks on the network would defeat the token, so nothing is sent until "Allow".
 */
@Composable
fun ConnectRequestCard(
    desktopName: String,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier,
    /** The short code to compare with the desktop's screen before allowing (DEB-67); `null` for an older desktop. */
    verificationCode: String? = null,
) {
    val shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge)
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(BugsySpacing.md),
            modifier = Modifier
                .padding(horizontal = BugsySpacing.xxl)
                .fillMaxWidth()
                .background(BugsyColors.Surface, shape)
                .border(BugsySpacing.strokeBorder, BugsyColors.Border, shape)
                .padding(BugsySpacing.lg),
        ) {
            Text(stringResource(Res.string.connect_request_title, desktopName), style = BugsyTypography.Title, color = BugsyColors.TextPrimary)
            Text(stringResource(Res.string.connect_request_body), style = BugsyTypography.Body, color = BugsyColors.TextDisabled)
            if (verificationCode != null) {
                VerificationCodeText(verificationCode)
                Text(stringResource(Res.string.connect_request_verify), style = BugsyTypography.Body, color = BugsyColors.TextDisabled)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm)) {
                OutlinedButton(
                    onClick = onDeny,
                    shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge),
                    modifier = Modifier.weight(1f).height(BugsySpacing.resetAllButtonHeight),
                ) {
                    Text(stringResource(Res.string.connect_request_deny), style = BugsyTypography.Body, color = BugsyColors.TextPrimary)
                }
                Button(
                    onClick = onAllow,
                    colors = ButtonDefaults.buttonColors(containerColor = BugsyColors.Brand, contentColor = BugsyColors.TextPrimary),
                    shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge),
                    modifier = Modifier.weight(1f).height(BugsySpacing.resetAllButtonHeight),
                ) {
                    Text(stringResource(Res.string.connect_request_allow), style = BugsyTypography.Body)
                }
            }
        }
    }
}

/** The 4-digit code, large and spaced in pairs ("49 42"), the same way the desktop shows it. */
@Composable
private fun VerificationCodeText(code: String) {
    Text(
        code.chunked(2).joinToString(" "),
        style = BugsyTypography.Title.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold),
        color = BugsyColors.Brand,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Pairing by code: before this phone sends its token, the person checks the desktop shows the same short code. A
 * different code means someone may be answering in the desktop's place, so "Different" sends nothing (DEB-67).
 */
@Composable
fun VerifyCodeCard(code: String, onMatch: () -> Unit, onDifferent: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge)
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(BugsySpacing.md),
            modifier = Modifier
                .padding(horizontal = BugsySpacing.xxl)
                .fillMaxWidth()
                .background(BugsyColors.Surface, shape)
                .border(BugsySpacing.strokeBorder, BugsyColors.Border, shape)
                .padding(BugsySpacing.lg),
        ) {
            Text(stringResource(Res.string.verify_code_title), style = BugsyTypography.Title, color = BugsyColors.TextPrimary)
            VerificationCodeText(code)
            Text(stringResource(Res.string.verify_code_body), style = BugsyTypography.Body, color = BugsyColors.TextDisabled)
            Row(horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm)) {
                OutlinedButton(
                    onClick = onDifferent,
                    shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge),
                    modifier = Modifier.weight(1f).height(BugsySpacing.resetAllButtonHeight),
                ) {
                    Text(stringResource(Res.string.verify_code_different), style = BugsyTypography.Body, color = BugsyColors.TextPrimary)
                }
                Button(
                    onClick = onMatch,
                    colors = ButtonDefaults.buttonColors(containerColor = BugsyColors.Brand, contentColor = BugsyColors.TextPrimary),
                    shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge),
                    modifier = Modifier.weight(1f).height(BugsySpacing.resetAllButtonHeight),
                ) {
                    Text(stringResource(Res.string.verify_code_match), style = BugsyTypography.Body)
                }
            }
        }
    }
}
