package com.dsk.soniloko.ui.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dsk.soniloko.R
import com.dsk.soniloko.data.AppStorage
import com.dsk.soniloko.data.SoundLibrary
import java.io.File

@Composable
fun SoundPickerDialog(
    sounds: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onPreview: (String) -> Unit,
    onSoundImported: () -> Unit,
    onRenameSound: (old: String, new: String, callback: (String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showRecordDialog by remember { mutableStateOf(false) }
    var showYoutubeDialog by remember { mutableStateOf(false) }
    var showStoragePrompt by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var soundPendingDelete by remember { mutableStateOf<String?>(null) }
    var soundPendingTrim by remember { mutableStateOf<File?>(null) }

    fun runWithStorageAccess(action: () -> Unit) {
        if (AppStorage.hasAllFilesAccess()) action() else showStoragePrompt = true
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val name = SoundLibrary.importSound(context, it)
            if (name != null) {
                onSoundImported()
                onSelect(name)
            }
        }
    }

    val filteredSounds = remember(sounds, query) {
        if (query.isBlank()) sounds else sounds.filter { it.contains(query, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 4.dp) {
            Column(Modifier.padding(8.dp).width(300.dp).heightIn(max = 560.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.search_sounds)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(4.dp)
                )
                Text(
                    text = stringResource(R.string.record_sound_option),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { runWithStorageAccess { showRecordDialog = true } }
                        .padding(12.dp)
                )
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.import_sound_from_device),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { runWithStorageAccess { importLauncher.launch(arrayOf("audio/*")) } }
                        .padding(12.dp)
                )
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.search_youtube_option),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { runWithStorageAccess { showYoutubeDialog = true } }
                        .padding(12.dp)
                )
                HorizontalDivider()
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(filteredSounds, key = { it }) { s ->
                        val isOwn = remember(s) { SoundLibrary.isOwnSound(s) }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPreview(s) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(s, modifier = Modifier.weight(1f).padding(12.dp))
                            IconButton(onClick = { onSelect(s) }) {
                                Text(
                                    "✓",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            if (isOwn) {
                                IconButton(onClick = { SoundLibrary.resolveOwnSoundFile(s)?.let { soundPendingTrim = it } }) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_trim))
                                }
                                IconButton(onClick = { soundPendingDelete = s }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showRecordDialog) {
        RecordSoundDialog(
            onDismiss = { showRecordDialog = false },
            onSaved = { name, tempFile ->
                val fileName = SoundLibrary.saveRecording(tempFile, name)
                showRecordDialog = false
                if (fileName != null) {
                    onSoundImported()
                    onSelect(fileName)
                }
            }
        )
    }

    if (showYoutubeDialog) {
        YoutubeSearchDialog(
            onDismiss = { showYoutubeDialog = false },
            onSaved = { name, file ->
                val fileName = SoundLibrary.saveRecording(file, name)
                showYoutubeDialog = false
                if (fileName != null) {
                    onSoundImported()
                    onSelect(fileName)
                }
            }
        )
    }

    soundPendingTrim?.let { file ->
        EditSoundTrimDialog(
            file = file,
            onDismiss = { soundPendingTrim = null },
            onRename = { newName, callback -> onRenameSound(file.name, newName, callback) },
            onSaved = {
                soundPendingTrim = null
                onSoundImported()
            }
        )
    }

    if (showStoragePrompt) {
        AlertDialog(
            onDismissRequest = { showStoragePrompt = false },
            title = { Text(stringResource(R.string.storage_permission_title)) },
            text = { Text(stringResource(R.string.storage_permission_message)) },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(AppStorage.requestAllFilesAccessIntent(context))
                    showStoragePrompt = false
                }) {
                    Text(stringResource(R.string.grant_storage_access))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStoragePrompt = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    soundPendingDelete?.let { name ->
        var deleteFromDevice by remember(name) { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { soundPendingDelete = null },
            title = { Text(stringResource(R.string.delete_sound_confirm_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_sound_confirm_message, name))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable { deleteFromDevice = !deleteFromDevice },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = deleteFromDevice, onCheckedChange = { deleteFromDevice = it })
                        Text(stringResource(R.string.delete_sound_from_device_option))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (deleteFromDevice) SoundLibrary.deleteOwnSound(name) else SoundLibrary.removeFromList(context, name)
                    onSoundImported()
                    soundPendingDelete = null
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { soundPendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
