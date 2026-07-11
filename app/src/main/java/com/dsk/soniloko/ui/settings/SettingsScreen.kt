package com.dsk.soniloko.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.dsk.soniloko.R
import com.dsk.soniloko.data.AppStorage
import com.dsk.soniloko.data.ImageLibrary
import com.dsk.soniloko.data.SoundLibrary
import com.dsk.soniloko.ui.theme.AppThemeOption
import com.dsk.soniloko.update.UpdateChecker
import com.dsk.soniloko.update.UpdateInfo
import com.dsk.soniloko.util.CreateDocumentWithHint
import com.dsk.soniloko.util.OpenDocumentWithHint
import com.dsk.soniloko.viewmodel.SoundboardViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SoundboardViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    var showResetConfirm by remember { mutableStateOf(false) }
    var hasStorageAccess by remember { mutableStateOf(AppStorage.hasAllFilesAccess()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checkingUpdate by remember { mutableStateOf(false) }
    var manualUpdateResult by remember { mutableStateOf<UpdateInfo?>(null) }

    val storagePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasStorageAccess = AppStorage.hasAllFilesAccess()
    }

    val exportLauncher = rememberLauncherForActivityResult(remember { CreateDocumentWithHint("application/json") }) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                os.write(viewModel.exportConfig().toByteArray())
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(remember { OpenDocumentWithHint() }) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r ->
                viewModel.importConfig(r.readText())
            }
        }
    }

    val exportSoundsZipLauncher = rememberLauncherForActivityResult(remember { CreateDocumentWithHint("application/zip") }) { uri ->
        uri?.let {
            val count = SoundLibrary.exportOwnSoundsZip(context, it)
            val message = if (count != null) context.getString(R.string.export_sounds_result, count) else context.getString(R.string.youtube_error)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    var pendingSoundsImportUri by remember { mutableStateOf<Uri?>(null) }
    val importSoundsZipLauncher = rememberLauncherForActivityResult(remember { OpenDocumentWithHint() }) { uri ->
        if (uri != null) pendingSoundsImportUri = uri
    }

    val exportImagesZipLauncher = rememberLauncherForActivityResult(remember { CreateDocumentWithHint("application/zip") }) { uri ->
        uri?.let {
            val count = ImageLibrary.exportOwnImagesZip(context, it)
            val message = if (count != null) context.getString(R.string.export_images_result, count) else context.getString(R.string.youtube_error)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    var pendingImagesImportUri by remember { mutableStateOf<Uri?>(null) }
    val importImagesZipLauncher = rememberLauncherForActivityResult(remember { OpenDocumentWithHint() }) { uri ->
        if (uri != null) pendingImagesImportUri = uri
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        // A small nudge so it's obvious there's more content below.
        scrollState.animateScrollTo(120, animationSpec = tween(400))
        scrollState.animateScrollTo(0, animationSpec = tween(400))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard {
                Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
                Column(Modifier.selectableGroup().padding(top = 4.dp)) {
                    LanguageOption("system", settings.language, stringResource(R.string.system_default)) { viewModel.setLanguage(it) }
                    LanguageOption("es", settings.language, "Español") { viewModel.setLanguage(it) }
                    LanguageOption("en", settings.language, "English") { viewModel.setLanguage(it) }
                }
            }

            SettingsCard {
                Text(stringResource(R.string.master_volume), style = MaterialTheme.typography.titleMedium)
                Slider(value = settings.masterVolume, onValueChange = { viewModel.setMasterVolume(it) })
            }

            SettingsCard {
                Text(stringResource(R.string.gain_boost), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.gain_boost_hint), style = MaterialTheme.typography.bodySmall)
                Slider(value = settings.gainBoost, onValueChange = { viewModel.setGainBoost(it) })
            }

            SettingsCard {
                Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    AppThemeOption.entries.forEach { option ->
                        ThemeSwatch(
                            option = option,
                            selected = settings.theme == option.id,
                            onClick = { viewModel.setTheme(option.id) }
                        )
                    }
                }
            }

            SettingsCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.toy_fx), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Switch(checked = settings.toyFxEnabled, onCheckedChange = { viewModel.setToyFx(it) })
                }
                if (settings.toyFxEnabled) {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    FxSlider(stringResource(R.string.fx_bass_cut), settings.fxBassCut) { viewModel.setFxBassCut(it) }
                    FxSlider(stringResource(R.string.fx_mid_boost), settings.fxMidBoost) { viewModel.setFxMidBoost(it) }
                    FxSlider(stringResource(R.string.fx_treble_cut), settings.fxTrebleCut) { viewModel.setFxTrebleCut(it) }
                    FxSlider(stringResource(R.string.fx_drive), settings.fxDrive) { viewModel.setFxDrive(it) }
                }
            }

            SettingsCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.preview_sounds_setting), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.preview_sounds_setting_hint), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = settings.previewSoundsEnabled, onCheckedChange = { viewModel.setPreviewSoundsEnabled(it) })
                }
            }

            SettingsCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.haptic_feedback_setting), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.haptic_feedback_setting_hint), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = settings.hapticFeedbackEnabled, onCheckedChange = { viewModel.setHapticFeedbackEnabled(it) })
                }
            }

            SettingsCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.allow_long_sounds), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.allow_long_sounds_hint), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = settings.allowLongSounds, onCheckedChange = { viewModel.setAllowLongSounds(it) })
                }
                if (!settings.allowLongSounds) {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Text(stringResource(R.string.max_sound_duration), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    DurationSelector(
                        currentMs = settings.maxSoundDurationMs,
                        onSelect = { viewModel.setMaxSoundDurationMs(it) }
                    )
                }
            }

            SettingsCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.allow_simultaneous_sounds), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.allow_simultaneous_sounds_hint), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = settings.allowSimultaneousSounds, onCheckedChange = { viewModel.setAllowSimultaneousSounds(it) })
                }
            }

            SettingsCard {
                Text(stringResource(R.string.storage_permission_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.storage_permission_message), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                if (hasStorageAccess) {
                    Text(stringResource(R.string.storage_access_granted), color = MaterialTheme.colorScheme.primary)
                } else {
                    Button(onClick = { storagePermissionLauncher.launch(AppStorage.requestAllFilesAccessIntent(context)) }) {
                        Text(stringResource(R.string.grant_storage_access))
                    }
                }
            }

            SettingsCard {
                Text(stringResource(R.string.custom_sounds_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.custom_sounds_hint), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { exportSoundsZipLauncher.launch("soniloko_sonidos.zip") }) {
                        Text(stringResource(R.string.export_sounds_zip))
                    }
                    OutlinedButton(onClick = { importSoundsZipLauncher.launch(arrayOf("application/zip")) }) {
                        Text(stringResource(R.string.import_sounds_zip))
                    }
                }
            }

            SettingsCard {
                Text(stringResource(R.string.custom_images_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.custom_images_hint), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { exportImagesZipLauncher.launch("soniloko_imagenes.zip") }) {
                        Text(stringResource(R.string.export_images_zip))
                    }
                    OutlinedButton(onClick = { importImagesZipLauncher.launch(arrayOf("application/zip")) }) {
                        Text(stringResource(R.string.import_images_zip))
                    }
                }
            }

            SettingsCard {
                Text(stringResource(R.string.reclip_server_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.reclip_server_hint), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                // Local state for instant typing feedback — binding these fields directly to
                // the DataStore-backed `settings` flow causes a visible lag/cursor jump on each
                // keystroke, since the round trip through the store is async.
                var localUrl by remember { mutableStateOf(settings.reclipServerUrl) }
                var localUsername by remember { mutableStateOf(settings.reclipUsername) }
                var localPassword by remember { mutableStateOf(settings.reclipPassword) }
                OutlinedTextField(
                    value = localUrl,
                    onValueChange = { localUrl = it; viewModel.setReclipServerUrl(it) },
                    label = { Text(stringResource(R.string.reclip_server_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = localUsername,
                    onValueChange = { localUsername = it; viewModel.setReclipUsername(it) },
                    label = { Text(stringResource(R.string.reclip_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = localPassword,
                    onValueChange = { localPassword = it; viewModel.setReclipPassword(it) },
                    label = { Text(stringResource(R.string.reclip_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SettingsCard {
                Text(stringResource(R.string.check_updates_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.check_updates_hint), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Button(
                    enabled = !checkingUpdate,
                    onClick = {
                        checkingUpdate = true
                        scope.launch {
                            val result = UpdateChecker.checkForUpdate(context)
                            checkingUpdate = false
                            if (result != null) {
                                manualUpdateResult = result
                            } else {
                                Toast.makeText(context, context.getString(R.string.no_update_available), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    if (checkingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(stringResource(R.string.check_updates_button))
                    }
                }
            }

            SettingsCard {
                Text(stringResource(R.string.export_import_config_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.export_import_config_hint), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { exportLauncher.launch("soniloko_config.json") }) {
                        Text(stringResource(R.string.export))
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Text(stringResource(R.string.import_))
                    }
                }
            }

            SettingsCard {
                Button(
                    onClick = { showResetConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.factory_reset))
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.reset_confirm_title)) },
            text = { Text(stringResource(R.string.reset_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetFactory(); showResetConfirm = false }) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    manualUpdateResult?.let { update ->
        AlertDialog(
            onDismissRequest = { manualUpdateResult = null },
            title = { Text(stringResource(R.string.update_available_title)) },
            text = { Text(stringResource(R.string.update_available_message, update.versionName)) },
            confirmButton = {
                TextButton(onClick = {
                    UpdateChecker.downloadAndInstall(context, update)
                    manualUpdateResult = null
                }) {
                    Text(stringResource(R.string.update_download_install))
                }
            },
            dismissButton = {
                TextButton(onClick = { manualUpdateResult = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    pendingSoundsImportUri?.let { uri ->
        ImportChoiceDialog(
            onDismiss = { pendingSoundsImportUri = null },
            onAdd = {
                val count = SoundLibrary.importOwnSoundsZip(context, uri, replace = false)
                val message = if (count != null) context.getString(R.string.import_sounds_result, count) else context.getString(R.string.youtube_error)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                pendingSoundsImportUri = null
            },
            onReplace = {
                val count = SoundLibrary.importOwnSoundsZip(context, uri, replace = true)
                val message = if (count != null) context.getString(R.string.import_sounds_result, count) else context.getString(R.string.youtube_error)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                pendingSoundsImportUri = null
            }
        )
    }
    pendingImagesImportUri?.let { uri ->
        ImportChoiceDialog(
            onDismiss = { pendingImagesImportUri = null },
            onAdd = {
                val count = ImageLibrary.importOwnImagesZip(context, uri, replace = false)
                val message = if (count != null) context.getString(R.string.import_images_result, count) else context.getString(R.string.youtube_error)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                pendingImagesImportUri = null
            },
            onReplace = {
                val count = ImageLibrary.importOwnImagesZip(context, uri, replace = true)
                val message = if (count != null) context.getString(R.string.import_images_result, count) else context.getString(R.string.youtube_error)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                pendingImagesImportUri = null
            }
        )
    }
}

/** Asks whether a zip import should add to the current own sounds/images or replace them entirely. */
@Composable
private fun ImportChoiceDialog(onDismiss: () -> Unit, onAdd: () -> Unit, onReplace: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_zip_choice_title)) },
        text = { Text(stringResource(R.string.import_zip_choice_message)) },
        confirmButton = {
            TextButton(onClick = onReplace) { Text(stringResource(R.string.import_zip_replace)) }
        },
        dismissButton = {
            TextButton(onClick = onAdd) { Text(stringResource(R.string.import_zip_add)) }
        }
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun FxSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    Slider(value = value, onValueChange = onValueChange)
}

private val DURATION_PRESETS_MS = listOf(1000, 2000, 3000, 5000, 10000, 15000, 30000)

@Composable
private fun DurationSelector(currentMs: Int, onSelect: (Int) -> Unit) {
    val isCustom = currentMs !in DURATION_PRESETS_MS
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DURATION_PRESETS_MS.forEach { ms ->
            DurationChip(label = "${ms / 1000}s", selected = !isCustom && currentMs == ms) { onSelect(ms) }
        }
        DurationChip(label = stringResource(R.string.custom_duration), selected = isCustom) {
            if (!isCustom) onSelect(20000)
        }
    }
    if (isCustom) {
        Spacer(Modifier.height(8.dp))
        Text("${currentMs / 1000}s", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = currentMs.toFloat(),
            onValueChange = { onSelect(it.toInt()) },
            valueRange = 1000f..60000f
        )
    }
}

@Composable
private fun DurationChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun LanguageOption(value: String, current: String, label: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = current == value, onClick = { onSelect(value) }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = current == value, onClick = { onSelect(value) })
        Text(label)
    }
}

@Composable
private fun ThemeSwatch(option: AppThemeOption, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(option.primary)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            )
            .selectable(selected = selected, onClick = onClick)
    )
}

