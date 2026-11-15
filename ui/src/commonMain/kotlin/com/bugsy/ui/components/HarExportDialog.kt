package com.bugsy.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.action_cancel_selection
import bugsy.ui.generated.resources.export_har_dialog_masked
import bugsy.ui.generated.resources.export_har_dialog_message
import bugsy.ui.generated.resources.export_har_dialog_real
import bugsy.ui.generated.resources.export_har_dialog_title
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsyTypography
import org.jetbrains.compose.resources.stringResource

/**
 * Asks before real values (tokens, passwords) leave the device in a HAR file. Masked is the safe default
 * and the first choice; real values are an explicit opt-in.
 */
@Composable
fun HarExportDialog(
    onExportMasked: () -> Unit,
    onExportReal: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BugsyColors.SurfaceCard,
        title = { Text(stringResource(Res.string.export_har_dialog_title), style = BugsyTypography.Body, color = BugsyColors.TextPrimary) },
        text = { Text(stringResource(Res.string.export_har_dialog_message), style = BugsyTypography.Caption, color = BugsyColors.TextMuted) },
        confirmButton = {
            TextButton(onClick = onExportMasked) {
                Text(stringResource(Res.string.export_har_dialog_masked), color = BugsyColors.Brand)
            }
        },
        dismissButton = {
            TextButton(onClick = onExportReal) {
                Text(stringResource(Res.string.export_har_dialog_real), color = BugsyColors.Status4xx)
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel_selection), color = BugsyColors.TextDisabled)
            }
        },
    )
}
