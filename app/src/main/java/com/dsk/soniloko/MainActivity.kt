package com.dsk.soniloko

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsk.soniloko.data.AppStorage
import com.dsk.soniloko.data.OnboardingPrefs
import com.dsk.soniloko.ui.AppFonts
import com.dsk.soniloko.ui.SoundboardScreen
import com.dsk.soniloko.ui.components.FirstRunPermissionsDialog
import com.dsk.soniloko.ui.settings.SettingsScreen
import com.dsk.soniloko.ui.theme.AppThemeOption
import com.dsk.soniloko.ui.theme.SoniLokoTheme
import com.dsk.soniloko.update.UpdateChecker
import com.dsk.soniloko.update.UpdateInfo
import com.dsk.soniloko.viewmodel.SoundboardViewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SoundboardViewModel = viewModel()
            val settings by viewModel.settings.collectAsState()

            LaunchedEffect(settings.language) {
                val locales = when (settings.language) {
                    "es" -> LocaleListCompat.forLanguageTags("es")
                    "en" -> LocaleListCompat.forLanguageTags("en")
                    else -> LocaleListCompat.getEmptyLocaleList()
                }
                if (AppCompatDelegate.getApplicationLocales() != locales) {
                    AppCompatDelegate.setApplicationLocales(locales)
                }
            }

            val faFontFamily = remember { AppFonts.loadFontAwesome(applicationContext) }
            var showSettings by remember { mutableStateOf(false) }
            var showExitConfirm by remember { mutableStateOf(false) }
            var showFirstRunDialog by remember { mutableStateOf(!OnboardingPrefs.hasRequestedPermissions(applicationContext)) }
            var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }

            LaunchedEffect(Unit) {
                availableUpdate = UpdateChecker.checkForUpdate(applicationContext)
            }

            val storagePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                OnboardingPrefs.markPermissionsRequested(applicationContext)
            }
            val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                // Proceed to the storage step regardless of whether mic access was granted.
                storagePermissionLauncher.launch(AppStorage.requestAllFilesAccessIntent(applicationContext))
            }

            BackHandler(enabled = showSettings) { showSettings = false }
            BackHandler(enabled = !showSettings && viewModel.editModeActive) { viewModel.toggleEditMode() }
            BackHandler(enabled = !showSettings && !viewModel.editModeActive) { showExitConfirm = true }

            SoniLokoTheme(themeOption = AppThemeOption.fromId(settings.theme)) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (showSettings) {
                        SettingsScreen(viewModel = viewModel, onBack = { showSettings = false })
                    } else {
                        SoundboardScreen(
                            viewModel = viewModel,
                            faFontFamily = faFontFamily,
                            onOpenSettings = { showSettings = true }
                        )
                    }
                }

                if (showExitConfirm) {
                    AlertDialog(
                        onDismissRequest = { showExitConfirm = false },
                        title = { Text(stringResource(R.string.exit_confirm_title)) },
                        text = { Text(stringResource(R.string.exit_confirm_message)) },
                        confirmButton = {
                            TextButton(onClick = { finish() }) { Text(stringResource(R.string.yes)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExitConfirm = false }) { Text(stringResource(R.string.cancel)) }
                        }
                    )
                }

                if (showFirstRunDialog) {
                    FirstRunPermissionsDialog(
                        onContinue = {
                            showFirstRunDialog = false
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onSkip = {
                            showFirstRunDialog = false
                            OnboardingPrefs.markPermissionsRequested(applicationContext)
                        }
                    )
                }

                availableUpdate?.let { update ->
                    AlertDialog(
                        onDismissRequest = { availableUpdate = null },
                        title = { Text(stringResource(R.string.update_available_title)) },
                        text = { Text(stringResource(R.string.update_available_message, update.versionName)) },
                        confirmButton = {
                            TextButton(onClick = {
                                UpdateChecker.downloadAndInstall(applicationContext, update)
                                availableUpdate = null
                            }) {
                                Text(stringResource(R.string.update_download_install))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { availableUpdate = null }) { Text(stringResource(R.string.first_run_skip)) }
                        }
                    )
                }
            }
        }
    }
}
