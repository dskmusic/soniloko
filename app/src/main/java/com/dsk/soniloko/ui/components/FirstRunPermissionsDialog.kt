package com.dsk.soniloko.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dsk.soniloko.R

@Composable
fun FirstRunPermissionsDialog(onContinue: () -> Unit, onSkip: () -> Unit) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(stringResource(R.string.first_run_title)) },
        text = { Text(stringResource(R.string.first_run_message)) },
        confirmButton = {
            TextButton(onClick = onContinue) { Text(stringResource(R.string.first_run_continue)) }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text(stringResource(R.string.first_run_skip)) }
        }
    )
}
