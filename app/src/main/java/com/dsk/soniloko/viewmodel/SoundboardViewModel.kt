package com.dsk.soniloko.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dsk.soniloko.audio.HapticFeedback
import com.dsk.soniloko.audio.SoundEngine
import com.dsk.soniloko.data.BoardRepository
import com.dsk.soniloko.data.CustomKitRepository
import com.dsk.soniloko.data.JsonMapper
import com.dsk.soniloko.data.SettingsRepository
import com.dsk.soniloko.data.SoundLibrary
import com.dsk.soniloko.data.model.AppSettings
import com.dsk.soniloko.data.model.SoundButtonConfig
import com.dsk.soniloko.data.model.SoundKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class SoundboardViewModel(application: Application) : AndroidViewModel(application) {
    private val boardRepo = BoardRepository(application)
    private val settingsRepo = SettingsRepository(application)
    private val customKitRepo = CustomKitRepository(application)
    private val soundEngine = SoundEngine(application)

    val buttons: StateFlow<List<SoundButtonConfig>> =
        boardRepo.board.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val settings: StateFlow<AppSettings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** Preset board layouts read from assets/kits.json, shown in the overflow menu. */
    val kits: List<SoundKit> = boardRepo.loadKits()

    /** User-created kits, shown below the bundled ones with a separator. */
    val customKits: StateFlow<List<SoundKit>> =
        customKitRepo.customKits.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Id of the kit the current board was loaded from/saved as, for a persistent "current kit" label. */
    val currentKitId: StateFlow<String?> =
        boardRepo.currentKitId.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var editModeActive by mutableStateOf(false)
        private set

    var editingButton by mutableStateOf<SoundButtonConfig?>(null)
        private set

    /** Bumped on every play so the speaker grille can animate a reaction pulse. */
    var playPulse by mutableStateOf(0)
        private set

    // --- Game mode ("Simón dice") ---
    var gameModeActive by mutableStateOf(false)
        private set
    var gameLevel by mutableStateOf(0)
        private set
    var gameLives by mutableStateOf(3)
        private set
    var gameHighlightedButtonId by mutableStateOf<Int?>(null)
        private set
    var gameOver by mutableStateOf(false)
        private set
    var gameAcceptingInput by mutableStateOf(false)
        private set

    private var gameSequence: List<Int> = emptyList()
    private var gameUserProgress = 0
    private var gameJob: Job? = null

    init {
        viewModelScope.launch {
            settings.collect { s ->
                soundEngine.masterVolume = s.masterVolume
                soundEngine.applyEffects(s.toyFxEnabled, s.fxBassCut, s.fxMidBoost, s.fxTrebleCut, s.fxDrive, s.gainBoost)
                soundEngine.setPlaybackPolicy(s.allowLongSounds, s.maxSoundDurationMs, s.allowSimultaneousSounds)
            }
        }
        viewModelScope.launch {
            buttons.collect { list ->
                val files = list.map { it.soundFile }
                soundEngine.preload(files)
                withContext(Dispatchers.IO) { soundEngine.warmDurationCache(files) }
            }
        }
    }

    fun onButtonTap(button: SoundButtonConfig) {
        if (editModeActive) {
            editingButton = button
        } else {
            vibrateIfEnabled()
            soundEngine.play(button.soundFile, button.volume)
            playPulse++
        }
    }

    /** Panic button (tapping the speaker grille): stops every sound currently playing. */
    fun stopAllSounds() {
        soundEngine.stopAll()
    }

    private fun vibrateIfEnabled() {
        if (settings.value.hapticFeedbackEnabled) HapticFeedback.tap(getApplication())
    }

    /** Plays a sound as a preview from the sound picker, if the setting is enabled. */
    fun previewSound(fileName: String) {
        if (settings.value.previewSoundsEnabled) soundEngine.playPreview(fileName)
    }

    fun onButtonLongPress(button: SoundButtonConfig) {
        editingButton = button
    }

    fun startGameMode() {
        if (buttons.value.isEmpty()) return
        editModeActive = false
        gameModeActive = true
        gameOver = false
        gameLives = 3
        gameLevel = 0
        gameSequence = emptyList()
        nextGameRound()
    }

    fun exitGameMode() {
        gameJob?.cancel()
        gameModeActive = false
        gameOver = false
        gameHighlightedButtonId = null
    }

    fun onGameButtonTap(buttonId: Int) {
        if (!gameModeActive || !gameAcceptingInput || gameOver) return
        val expected = gameSequence.getOrNull(gameUserProgress) ?: return
        vibrateIfEnabled()
        buttons.value.firstOrNull { it.id == buttonId }?.let { soundEngine.play(it.soundFile, it.volume) }

        if (buttonId == expected) {
            gameHighlightedButtonId = buttonId
            viewModelScope.launch {
                delay(250)
                if (gameHighlightedButtonId == buttonId) gameHighlightedButtonId = null
            }
            gameUserProgress++
            if (gameUserProgress >= gameSequence.size) {
                gameAcceptingInput = false
                gameJob = viewModelScope.launch {
                    delay(700)
                    nextGameRound()
                }
            }
        } else {
            gameLives--
            if (gameLives <= 0) {
                gameOver = true
                gameAcceptingInput = false
            } else {
                gameUserProgress = 0
                gameAcceptingInput = false
                gameJob = viewModelScope.launch {
                    delay(700)
                    playGameSequence()
                }
            }
        }
    }

    private fun nextGameRound() {
        gameLevel++
        val nextId = buttons.value.map { it.id }.random()
        gameSequence = gameSequence + nextId
        gameUserProgress = 0
        playGameSequence()
    }

    private fun playGameSequence() {
        gameJob?.cancel()
        gameAcceptingInput = false
        gameJob = viewModelScope.launch {
            delay(500)
            for (id in gameSequence) {
                gameHighlightedButtonId = id
                buttons.value.firstOrNull { it.id == id }?.let { soundEngine.play(it.soundFile, it.volume) }
                delay(450)
                gameHighlightedButtonId = null
                delay(200)
            }
            gameAcceptingInput = true
        }
    }

    fun toggleEditMode() {
        editModeActive = !editModeActive
    }

    fun dismissEditDialog() {
        editingButton = null
    }

    fun saveButtonConfig(updated: SoundButtonConfig) {
        viewModelScope.launch {
            boardRepo.saveBoard(buttons.value.map { if (it.id == updated.id) updated else it })
        }
        editingButton = null
    }

    /** Renames an own sound file and updates any current board buttons that referenced the
     * old name, so they keep working. Returns the actual final name via [onResult]. */
    fun renameOwnSound(oldName: String, newBaseName: String, onResult: (String?) -> Unit) {
        val newName = SoundLibrary.renameOwnSound(oldName, newBaseName)
        if (newName != null && newName != oldName) {
            viewModelScope.launch {
                boardRepo.saveBoard(buttons.value.map { if (it.soundFile == oldName) it.copy(soundFile = newName) else it })
            }
        }
        onResult(newName)
    }

    fun applyKit(kit: SoundKit) {
        viewModelScope.launch { boardRepo.applyKit(kit) }
    }

    /** Saves the current board layout (icons, images, sounds, volumes) as a new custom kit. */
    fun saveCurrentAsKit(name: String) = createCustomKit(name, buttons.value)

    /** Saves an independently-built set of buttons (from the new-kit builder) as a new custom
     * kit, and immediately loads it onto the board so it shows up as the active kit. */
    fun createCustomKit(name: String, kitButtons: List<SoundButtonConfig>) {
        val kit = SoundKit(id = UUID.randomUUID().toString(), namesByLang = mapOf("es" to name, "en" to name), buttons = kitButtons)
        viewModelScope.launch {
            customKitRepo.addKit(kit)
            boardRepo.applyKit(kit)
        }
    }

    fun renameCustomKit(id: String, newName: String) {
        viewModelScope.launch { customKitRepo.renameKit(id, newName) }
    }

    /** Overwrites an already-created custom kit's name and buttons — used by the kit builder
     * when reopened to edit an existing kit (add more buttons, tweak existing ones, rename) —
     * and reloads it onto the board so the edits show up immediately. */
    fun updateCustomKit(id: String, name: String, kitButtons: List<SoundButtonConfig>) {
        val kit = SoundKit(id = id, namesByLang = mapOf("es" to name, "en" to name), buttons = kitButtons)
        viewModelScope.launch {
            customKitRepo.updateKit(id, name, kitButtons)
            boardRepo.applyKit(kit)
        }
    }

    /** Appends a new default button to the live board — the edit-mode "+" tile, so a kit with
     * room left (under the 12-button grid) can grow without going through the kit builder. */
    fun addBoardButton(availableSounds: List<String>) {
        val nextId = (buttons.value.maxOfOrNull { it.id } ?: 0) + 1
        val newButton = SoundButtonConfig(id = nextId, iconName = "music", soundFile = availableSounds.firstOrNull() ?: "", volume = 1f)
        viewModelScope.launch { boardRepo.saveBoard(buttons.value + newButton) }
    }

    /** Deletes a custom kit; if it was the active one, falls back to the first bundled kit
     * so the board never keeps showing a kit that no longer exists. */
    fun deleteCustomKit(id: String) {
        viewModelScope.launch {
            customKitRepo.deleteKit(id)
            if (currentKitId.value == id) {
                kits.firstOrNull()?.let { boardRepo.applyKit(it) }
            }
        }
    }

    fun setMasterVolume(v: Float) {
        viewModelScope.launch { settingsRepo.setMasterVolume(v) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { settingsRepo.setTheme(theme) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { settingsRepo.setLanguage(lang) }
    }

    fun setToyFx(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setToyFx(enabled) }
    }

    fun setFxBassCut(v: Float) {
        viewModelScope.launch { settingsRepo.setFxBassCut(v) }
    }

    fun setFxMidBoost(v: Float) {
        viewModelScope.launch { settingsRepo.setFxMidBoost(v) }
    }

    fun setFxTrebleCut(v: Float) {
        viewModelScope.launch { settingsRepo.setFxTrebleCut(v) }
    }

    fun setFxDrive(v: Float) {
        viewModelScope.launch { settingsRepo.setFxDrive(v) }
    }

    fun setGainBoost(v: Float) {
        viewModelScope.launch { settingsRepo.setGainBoost(v) }
    }

    fun setPreviewSoundsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setPreviewSoundsEnabled(enabled) }
    }

    fun setAllowLongSounds(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setAllowLongSounds(enabled) }
    }

    fun setMaxSoundDurationMs(ms: Int) {
        viewModelScope.launch { settingsRepo.setMaxSoundDurationMs(ms) }
    }

    fun setAllowSimultaneousSounds(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setAllowSimultaneousSounds(enabled) }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setHapticFeedbackEnabled(enabled) }
    }

    fun setReclipServerUrl(url: String) {
        viewModelScope.launch { settingsRepo.setReclipServerUrl(url) }
    }

    fun setReclipUsername(username: String) {
        viewModelScope.launch { settingsRepo.setReclipUsername(username) }
    }

    fun setReclipPassword(password: String) {
        viewModelScope.launch { settingsRepo.setReclipPassword(password) }
    }

    fun resetFactory() {
        viewModelScope.launch {
            settingsRepo.resetToDefaults()
            boardRepo.resetToDefault()
            customKitRepo.clearAll()
        }
    }

    fun exportConfig(): String = JsonMapper.encodeFullConfig(settings.value, buttons.value, customKits.value)

    fun importConfig(json: String) {
        viewModelScope.launch {
            val config = JsonMapper.decodeFullConfig(json)
            settingsRepo.setLanguage(config.settings.language)
            settingsRepo.setMasterVolume(config.settings.masterVolume)
            settingsRepo.setTheme(config.settings.theme)
            settingsRepo.setToyFx(config.settings.toyFxEnabled)
            settingsRepo.setFxBassCut(config.settings.fxBassCut)
            settingsRepo.setFxMidBoost(config.settings.fxMidBoost)
            settingsRepo.setFxTrebleCut(config.settings.fxTrebleCut)
            settingsRepo.setFxDrive(config.settings.fxDrive)
            settingsRepo.setGainBoost(config.settings.gainBoost)
            settingsRepo.setPreviewSoundsEnabled(config.settings.previewSoundsEnabled)
            settingsRepo.setAllowLongSounds(config.settings.allowLongSounds)
            settingsRepo.setMaxSoundDurationMs(config.settings.maxSoundDurationMs)
            settingsRepo.setAllowSimultaneousSounds(config.settings.allowSimultaneousSounds)
            settingsRepo.setHapticFeedbackEnabled(config.settings.hapticFeedbackEnabled)
            settingsRepo.setReclipServerUrl(config.settings.reclipServerUrl)
            boardRepo.saveBoard(config.buttons)
            customKitRepo.replaceAll(config.customKits)
        }
    }

    override fun onCleared() {
        soundEngine.release()
    }
}
