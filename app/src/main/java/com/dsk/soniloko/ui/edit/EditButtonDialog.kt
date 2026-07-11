package com.dsk.soniloko.ui.edit

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dsk.soniloko.R
import com.dsk.soniloko.data.FontAwesomeIcons
import com.dsk.soniloko.data.ImageLibrary
import com.dsk.soniloko.data.model.SoundButtonConfig
import com.dsk.soniloko.ui.components.AutoSizeText

@Composable
fun EditButtonDialog(
    initial: SoundButtonConfig,
    availableSounds: List<String>,
    faFontFamily: FontFamily?,
    onDismiss: () -> Unit,
    onSave: (SoundButtonConfig) -> Unit,
    onSoundsChanged: () -> Unit,
    onPreviewSound: (String) -> Unit,
    onRenameSound: (old: String, new: String, callback: (String?) -> Unit) -> Unit
) {
    var iconName by remember { mutableStateOf(initial.iconName) }
    var soundFile by remember { mutableStateOf(initial.soundFile) }
    var volume by remember { mutableFloatStateOf(initial.volume) }
    var customImageFile by remember { mutableStateOf(initial.customImageFile) }
    var customText by remember { mutableStateOf(initial.customText ?: "") }
    var showIconPicker by remember { mutableStateOf(false) }
    var showSoundPicker by remember { mutableStateOf(false) }
    var showImageSearch by remember { mutableStateOf(false) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) pendingImageUri = uri
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 4.dp) {
            Column(Modifier.padding(20.dp).width(320.dp)) {
                Text(stringResource(R.string.edit_button_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                val previewBitmap = remember(customImageFile) {
                    customImageFile
                        ?.let { ImageLibrary.resolveImageFile(it) }
                        ?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
                }
                Box(
                    Modifier
                        .size(72.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .then(
                            if (previewBitmap == null) Modifier.background(MaterialTheme.colorScheme.primary)
                            else Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        .clickable { showIconPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        previewBitmap != null -> Image(
                            bitmap = previewBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        customText.isNotBlank() -> AutoSizeText(
                            text = customText,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.fillMaxWidth().padding(6.dp)
                        )
                        else -> Text(
                            FontAwesomeIcons.byName(iconName)?.codepoint ?: "?",
                            fontFamily = faFontFamily,
                            fontSize = 30.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Row(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { showIconPicker = true }) {
                        Text(stringResource(R.string.choose_icon))
                    }
                    TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text(stringResource(R.string.choose_image))
                    }
                    TextButton(onClick = { showImageSearch = true }) {
                        Text(stringResource(R.string.search_image_online))
                    }
                }
                if (customImageFile != null) {
                    TextButton(
                        onClick = { customImageFile = null },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(stringResource(R.string.remove_image))
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.button_text), style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = customText,
                    onValueChange = { newValue ->
                        customText = newValue
                        if (newValue.isNotBlank()) customImageFile = null
                    },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.sound), style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { showSoundPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(soundFile)
                }

                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.volume), style = MaterialTheme.typography.labelLarge)
                Slider(value = volume, onValueChange = { volume = it })

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        // The old file (if replaced or removed) is only safe to delete once the
                        // change is actually committed — cancelling the dialog must leave it intact.
                        if (initial.customImageFile != null && initial.customImageFile != customImageFile) {
                            ImageLibrary.deleteImage(initial.customImageFile)
                        }
                        onSave(
                            initial.copy(
                                iconName = iconName,
                                soundFile = soundFile,
                                volume = volume,
                                customImageFile = customImageFile,
                                customText = customText.trim().ifBlank { null }
                            )
                        )
                    }) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }

    if (showIconPicker) {
        IconPickerDialog(
            faFontFamily = faFontFamily,
            onDismiss = { showIconPicker = false },
            onSelect = {
                iconName = it
                customImageFile = null
                customText = ""
                showIconPicker = false
            }
        )
    }
    if (showSoundPicker) {
        SoundPickerDialog(
            sounds = availableSounds,
            onDismiss = { showSoundPicker = false },
            onSelect = { soundFile = it; showSoundPicker = false },
            onPreview = onPreviewSound,
            onSoundImported = onSoundsChanged,
            onRenameSound = { old, new, callback ->
                onRenameSound(old, new) { finalName ->
                    if (finalName != null && soundFile == old) soundFile = finalName
                    callback(finalName)
                }
            }
        )
    }
    if (showImageSearch) {
        ImageSearchDialog(
            onDismiss = { showImageSearch = false },
            onImageReady = { bitmap ->
                customImageFile = ImageLibrary.saveImage(bitmap)
                customText = ""
                showImageSearch = false
            }
        )
    }
    pendingImageUri?.let { uri ->
        ImageCropDialog(
            imageUri = uri,
            onDismiss = { pendingImageUri = null },
            onCropped = { bitmap ->
                customImageFile = ImageLibrary.saveImage(bitmap)
                customText = ""
                pendingImageUri = null
            }
        )
    }
}
