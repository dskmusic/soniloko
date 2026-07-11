package com.dsk.soniloko.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dsk.soniloko.R
import com.dsk.soniloko.data.SoundLibrary
import com.dsk.soniloko.data.model.SoundKit
import com.dsk.soniloko.ui.components.Footer
import com.dsk.soniloko.ui.components.HelpDialog
import com.dsk.soniloko.ui.components.SoundButtonItem
import com.dsk.soniloko.ui.components.SpeakerGrille
import com.dsk.soniloko.ui.edit.EditButtonDialog
import com.dsk.soniloko.ui.kits.ManageKitsDialog
import com.dsk.soniloko.ui.kits.SaveKitDialog
import com.dsk.soniloko.viewmodel.SoundboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundboardScreen(
    viewModel: SoundboardViewModel,
    faFontFamily: FontFamily?,
    onOpenSettings: () -> Unit
) {
    val buttons by viewModel.buttons.collectAsState()
    val customKits by viewModel.customKits.collectAsState()
    val context = LocalContext.current
    var availableSounds by remember { mutableStateOf(SoundLibrary.listAvailableSounds(context)) }
    var showMenu by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showSaveKit by remember { mutableStateOf(false) }
    var showManageKits by remember { mutableStateOf(false) }

    val gameModeActive = viewModel.gameModeActive

    // Swipe left/right to cycle through kits (built-in first, then custom), showing the kit
    // name briefly in the speaker. Purely a navigation cursor — doesn't try to track whether
    // the board still matches whatever kit it last landed on.
    val allKits: List<SoundKit> = remember(customKits) { viewModel.kits + customKits }
    var kitIndex by remember { mutableStateOf(0) }
    var kitFlashName by remember { mutableStateOf("") }
    var kitFlashTrigger by remember { mutableStateOf(0) }
    val canSwipeKits = allKits.size > 1 && !gameModeActive && !viewModel.editModeActive

    fun goToKit(newIndex: Int) {
        if (allKits.isEmpty()) return
        val wrapped = ((newIndex % allKits.size) + allKits.size) % allKits.size
        kitIndex = wrapped
        val kit = allKits[wrapped]
        viewModel.applyKit(kit)
        kitFlashName = kit.displayName()
        kitFlashTrigger++
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.app_name))
                    }
                },
                actions = {
                    if (gameModeActive) {
                        TextButton(onClick = { viewModel.exitGameMode() }) {
                            Text(stringResource(R.string.exit_game_mode), color = MaterialTheme.colorScheme.onSurface)
                        }
                    } else {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                allKits.forEachIndexed { index, kit ->
                                    DropdownMenuItem(
                                        text = { Text(kit.displayName()) },
                                        onClick = {
                                            kitIndex = index
                                            viewModel.applyKit(kit)
                                            showMenu = false
                                        }
                                    )
                                    if (index == viewModel.kits.lastIndex) HorizontalDivider()
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.save_current_kit)) },
                                    onClick = {
                                        showSaveKit = true
                                        showMenu = false
                                    }
                                )
                                if (customKits.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.manage_kits_title)) },
                                        onClick = {
                                            showManageKits = true
                                            showMenu = false
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    text = {
                                        Text(
                                            if (viewModel.editModeActive) stringResource(R.string.exit_edit_mode)
                                            else stringResource(R.string.customize_sounds)
                                        )
                                    },
                                    onClick = {
                                        viewModel.toggleEditMode()
                                        showMenu = false
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.game_mode)) },
                                    onClick = {
                                        viewModel.startGameMode()
                                        showMenu = false
                                    }
                                )
                            }
                        }
                        IconButton(onClick = { showHelp = true }) {
                            Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.help))
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = { Footer(Modifier.fillMaxWidth().navigationBarsPadding()) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .then(
                    if (canSwipeKits) {
                        Modifier.pointerInput(allKits) {
                            var dragAccum = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { dragAccum = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragAccum += dragAmount
                                },
                                onDragEnd = {
                                    val threshold = 100f
                                    when {
                                        dragAccum <= -threshold -> goToKit(kitIndex + 1)
                                        dragAccum >= threshold -> goToKit(kitIndex - 1)
                                    }
                                    dragAccum = 0f
                                }
                            )
                        }
                    } else Modifier
                )
        ) {
            SpeakerGrille(
                Modifier.fillMaxWidth().padding(16.dp),
                dotColor = MaterialTheme.colorScheme.primary,
                pulseTrigger = viewModel.playPulse,
                gameLevel = if (gameModeActive) viewModel.gameLevel else null,
                gameLives = viewModel.gameLives,
                kitNameFlash = kitFlashName,
                kitNameFlashTrigger = kitFlashTrigger,
                onPanicTap = { viewModel.stopAllSounds() }
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(buttons, key = { it.id }) { btn ->
                    SoundButtonItem(
                        config = btn,
                        editMode = viewModel.editModeActive,
                        faFontFamily = faFontFamily,
                        highlighted = gameModeActive && viewModel.gameHighlightedButtonId == btn.id,
                        onTap = {
                            if (gameModeActive) viewModel.onGameButtonTap(btn.id) else viewModel.onButtonTap(btn)
                        },
                        onLongPress = { if (!gameModeActive) viewModel.onButtonLongPress(btn) }
                    )
                }
            }
        }
    }

    viewModel.editingButton?.let { editing ->
        EditButtonDialog(
            initial = editing,
            availableSounds = availableSounds,
            faFontFamily = faFontFamily,
            onDismiss = { viewModel.dismissEditDialog() },
            onSave = { viewModel.saveButtonConfig(it) },
            onSoundsChanged = { availableSounds = SoundLibrary.listAvailableSounds(context) },
            onPreviewSound = { viewModel.previewSound(it) },
            onRenameSound = { old, new, callback -> viewModel.renameOwnSound(old, new, callback) }
        )
    }

    if (showHelp) {
        HelpDialog(onDismiss = { showHelp = false })
    }

    if (showSaveKit) {
        SaveKitDialog(
            onDismiss = { showSaveKit = false },
            onSave = { name ->
                viewModel.saveCurrentAsKit(name)
                showSaveKit = false
            }
        )
    }

    if (showManageKits) {
        ManageKitsDialog(
            kits = customKits,
            onDismiss = { showManageKits = false },
            onRename = { id, newName -> viewModel.renameCustomKit(id, newName) },
            onDelete = { id -> viewModel.deleteCustomKit(id) }
        )
    }

    if (gameModeActive && viewModel.gameOver) {
        AlertDialog(
            onDismissRequest = { viewModel.exitGameMode() },
            title = { Text(stringResource(R.string.game_over_title)) },
            text = { Text(stringResource(R.string.game_over_message, viewModel.gameLevel)) },
            confirmButton = {
                Button(onClick = { viewModel.startGameMode() }) {
                    Text(stringResource(R.string.game_retry))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.exitGameMode() }) {
                    Text(stringResource(R.string.exit_game_mode))
                }
            }
        )
    }
}
