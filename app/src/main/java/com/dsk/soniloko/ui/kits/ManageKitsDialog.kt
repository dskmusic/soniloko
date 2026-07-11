package com.dsk.soniloko.ui.kits

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dsk.soniloko.R
import com.dsk.soniloko.data.model.SoundKit

@Composable
fun ManageKitsDialog(
    kits: List<SoundKit>,
    onDismiss: () -> Unit,
    onRename: (id: String, newName: String) -> Unit,
    onEdit: (kit: SoundKit) -> Unit,
    onDelete: (id: String) -> Unit
) {
    var kitPendingDelete by remember { mutableStateOf<SoundKit?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 4.dp) {
            Column(Modifier.padding(20.dp).width(320.dp)) {
                Text(stringResource(R.string.manage_kits_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                if (kits.isEmpty()) {
                    Text(stringResource(R.string.no_custom_kits), style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(kits, key = { it.id }) { kit ->
                            var text by remember(kit.id) { mutableStateOf(kit.displayName()) }
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = text,
                                    onValueChange = {
                                        text = it
                                        onRename(kit.id, it)
                                    },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onEdit(kit) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_kit))
                                }
                                IconButton(onClick = { kitPendingDelete = kit }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }

    kitPendingDelete?.let { kit ->
        AlertDialog(
            onDismissRequest = { kitPendingDelete = null },
            title = { Text(stringResource(R.string.delete_kit_confirm_title)) },
            text = { Text(stringResource(R.string.delete_kit_confirm_message, kit.displayName())) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(kit.id)
                    kitPendingDelete = null
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { kitPendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
