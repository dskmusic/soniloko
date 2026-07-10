package com.dsk.soniloko.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dsk.soniloko.R
import com.dsk.soniloko.audio.AudioTrim
import com.dsk.soniloko.ui.components.TrimControls
import java.io.File

/** Re-opens the trim tool on an already-saved own sound, and lets you rename it too;
 * both are applied in place on save. */
@Composable
fun EditSoundTrimDialog(
    file: File,
    onDismiss: () -> Unit,
    onRename: (newName: String, callback: (String?) -> Unit) -> Unit,
    onSaved: () -> Unit
) {
    var durationMs by remember { mutableIntStateOf(AudioTrim.loadDurationMs(file)) }
    var trimRange by remember { mutableStateOf(0f..1f) }
    var name by remember { mutableStateOf(file.nameWithoutExtension) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 4.dp) {
            Column(Modifier.padding(20.dp).width(300.dp)) {
                Text(stringResource(R.string.edit_trim), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.sound_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (durationMs > 0) {
                    Spacer(Modifier.height(16.dp))
                    TrimControls(file, durationMs, trimRange) { trimRange = it }
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = durationMs > 0 && name.isNotBlank(),
                        onClick = {
                            val startMs = (trimRange.start * durationMs).toLong()
                            val endMs = (trimRange.endInclusive * durationMs).toLong()
                            val isFullRange = trimRange.start <= 0.001f && trimRange.endInclusive >= 0.999f
                            if (!isFullRange) AudioTrim.trimInPlace(file, startMs, endMs)

                            val newBaseName = name.trim()
                            if (newBaseName.isNotBlank() && newBaseName != file.nameWithoutExtension) {
                                onRename(newBaseName) { onSaved() }
                            } else {
                                onSaved()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}
