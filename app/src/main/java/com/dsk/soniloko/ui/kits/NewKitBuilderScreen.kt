package com.dsk.soniloko.ui.kits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dsk.soniloko.R
import com.dsk.soniloko.data.SoundLibrary
import com.dsk.soniloko.data.model.SoundButtonConfig
import com.dsk.soniloko.data.model.SoundKit
import com.dsk.soniloko.ui.components.SoundButtonItem
import com.dsk.soniloko.ui.components.dashedBorder
import com.dsk.soniloko.ui.edit.EditButtonDialog
import com.dsk.soniloko.viewmodel.SoundboardViewModel

/** Same fixed board size as every kit in assets/kits.json (3 columns x 4 rows). */
private const val MAX_BUTTONS = 12

private fun defaultButtonConfig(id: Int, availableSounds: List<String>): SoundButtonConfig =
    SoundButtonConfig(id = id, iconName = "music", soundFile = availableSounds.firstOrNull() ?: "", volume = 1f)

/** The dashed-border "+" tile shown when a kit's grid still has room (under 12 buttons). */
@Composable
fun AddButtonTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .dashedBorder(MaterialTheme.colorScheme.primary, 22.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(R.string.add_button),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewKitBuilderScreen(
    viewModel: SoundboardViewModel,
    faFontFamily: FontFamily?,
    editingKit: SoundKit? = null,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    var kitName by remember { mutableStateOf(editingKit?.displayName() ?: "") }
    var availableSounds by remember { mutableStateOf(SoundLibrary.listAvailableSounds(context)) }
    val kitButtons = remember { mutableStateListOf<SoundButtonConfig>().apply { editingKit?.let { addAll(it.buttons) } } }
    var editingButton by remember { mutableStateOf<SoundButtonConfig?>(null) }

    if (kitButtons.isEmpty()) {
        kitButtons.add(defaultButtonConfig(1, availableSounds))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (editingKit != null) R.string.edit_kit else R.string.new_kit_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        enabled = kitName.isNotBlank(),
                        onClick = {
                            val trimmedName = kitName.trim()
                            if (editingKit != null) {
                                viewModel.updateCustomKit(editingKit.id, trimmedName, kitButtons.toList())
                            } else {
                                viewModel.createCustomKit(trimmedName, kitButtons.toList())
                            }
                            onDone()
                        }
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.save))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = kitName,
                onValueChange = { kitName = it },
                label = { Text(stringResource(R.string.kit_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(kitButtons, key = { it.id }) { btn ->
                    SoundButtonItem(
                        config = btn,
                        editMode = false,
                        faFontFamily = faFontFamily,
                        onTap = { viewModel.previewSound(btn.soundFile) },
                        onLongPress = { editingButton = btn }
                    )
                }
                if (kitButtons.size < MAX_BUTTONS) {
                    item {
                        AddButtonTile(
                            onClick = {
                                val nextId = (kitButtons.maxOfOrNull { it.id } ?: 0) + 1
                                kitButtons.add(defaultButtonConfig(nextId, availableSounds))
                            }
                        )
                    }
                }
            }
        }
    }

    editingButton?.let { editing ->
        EditButtonDialog(
            initial = editing,
            availableSounds = availableSounds,
            faFontFamily = faFontFamily,
            onDismiss = { editingButton = null },
            onSave = { updated ->
                val index = kitButtons.indexOfFirst { it.id == updated.id }
                if (index >= 0) kitButtons[index] = updated
                editingButton = null
            },
            onSoundsChanged = { availableSounds = SoundLibrary.listAvailableSounds(context) },
            onPreviewSound = { viewModel.previewSound(it) },
            onRenameSound = { old, new, callback -> viewModel.renameOwnSound(old, new, callback) }
        )
    }
}
