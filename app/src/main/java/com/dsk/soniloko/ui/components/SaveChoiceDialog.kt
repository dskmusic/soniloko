package com.dsk.soniloko.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dsk.soniloko.R

/** Lets the user save a just-downloaded/recorded image or sound either into the app's own
 * folder (so it shows up back in the picker lists) or anywhere else via the system file picker. */
@Composable
fun SaveChoiceDialog(
    title: String,
    onSaveHere: () -> Unit,
    onSaveAs: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(stringResource(R.string.save_choice_message)) },
        confirmButton = {
            TextButton(onClick = onSaveHere) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSaveAs) { Text(stringResource(R.string.save_as)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}
