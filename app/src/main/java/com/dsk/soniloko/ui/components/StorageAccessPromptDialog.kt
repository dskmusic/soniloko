package com.dsk.soniloko.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dsk.soniloko.R
import com.dsk.soniloko.data.AppStorage

/** Explains why full storage access is needed, then sends the user to the system settings screen to grant it. */
@Composable
fun StorageAccessPromptDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.storage_permission_title)) },
        text = { Text(stringResource(R.string.storage_permission_message)) },
        confirmButton = {
            TextButton(onClick = {
                context.startActivity(AppStorage.requestAllFilesAccessIntent(context))
                onDismiss()
            }) {
                Text(stringResource(R.string.grant_storage_access))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
