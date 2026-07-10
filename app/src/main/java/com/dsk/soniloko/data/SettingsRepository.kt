package com.dsk.soniloko.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dsk.soniloko.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "soniloko_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val MASTER_VOLUME = floatPreferencesKey("master_volume")
        val THEME = stringPreferencesKey("theme")
        val TOY_FX = booleanPreferencesKey("toy_fx_enabled")
        val FX_BASS_CUT = floatPreferencesKey("fx_bass_cut")
        val FX_MID_BOOST = floatPreferencesKey("fx_mid_boost")
        val FX_TREBLE_CUT = floatPreferencesKey("fx_treble_cut")
        val FX_DRIVE = floatPreferencesKey("fx_drive")
        val GAIN_BOOST = floatPreferencesKey("gain_boost")
        val PREVIEW_SOUNDS = booleanPreferencesKey("preview_sounds_enabled")
        val ALLOW_LONG_SOUNDS = booleanPreferencesKey("allow_long_sounds")
        val MAX_SOUND_DURATION_MS = intPreferencesKey("max_sound_duration_ms")
        val ALLOW_SIMULTANEOUS_SOUNDS = booleanPreferencesKey("allow_simultaneous_sounds")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback_enabled")
        val RECLIP_SERVER_URL = stringPreferencesKey("reclip_server_url")
        val RECLIP_USERNAME = stringPreferencesKey("reclip_username")
        val RECLIP_PASSWORD = stringPreferencesKey("reclip_password")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            language = prefs[Keys.LANGUAGE] ?: defaults.language,
            masterVolume = prefs[Keys.MASTER_VOLUME] ?: defaults.masterVolume,
            theme = prefs[Keys.THEME] ?: defaults.theme,
            toyFxEnabled = prefs[Keys.TOY_FX] ?: defaults.toyFxEnabled,
            fxBassCut = prefs[Keys.FX_BASS_CUT] ?: defaults.fxBassCut,
            fxMidBoost = prefs[Keys.FX_MID_BOOST] ?: defaults.fxMidBoost,
            fxTrebleCut = prefs[Keys.FX_TREBLE_CUT] ?: defaults.fxTrebleCut,
            fxDrive = prefs[Keys.FX_DRIVE] ?: defaults.fxDrive,
            gainBoost = prefs[Keys.GAIN_BOOST] ?: defaults.gainBoost,
            previewSoundsEnabled = prefs[Keys.PREVIEW_SOUNDS] ?: defaults.previewSoundsEnabled,
            allowLongSounds = prefs[Keys.ALLOW_LONG_SOUNDS] ?: defaults.allowLongSounds,
            maxSoundDurationMs = prefs[Keys.MAX_SOUND_DURATION_MS] ?: defaults.maxSoundDurationMs,
            allowSimultaneousSounds = prefs[Keys.ALLOW_SIMULTANEOUS_SOUNDS] ?: defaults.allowSimultaneousSounds,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: defaults.hapticFeedbackEnabled,
            reclipServerUrl = prefs[Keys.RECLIP_SERVER_URL] ?: defaults.reclipServerUrl,
            reclipUsername = prefs[Keys.RECLIP_USERNAME] ?: defaults.reclipUsername,
            reclipPassword = prefs[Keys.RECLIP_PASSWORD] ?: defaults.reclipPassword
        )
    }

    suspend fun setLanguage(lang: String) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = lang }
    }

    suspend fun setMasterVolume(v: Float) {
        context.settingsDataStore.edit { it[Keys.MASTER_VOLUME] = v }
    }

    suspend fun setTheme(theme: String) {
        context.settingsDataStore.edit { it[Keys.THEME] = theme }
    }

    suspend fun setToyFx(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.TOY_FX] = enabled }
    }

    suspend fun setFxBassCut(v: Float) {
        context.settingsDataStore.edit { it[Keys.FX_BASS_CUT] = v }
    }

    suspend fun setFxMidBoost(v: Float) {
        context.settingsDataStore.edit { it[Keys.FX_MID_BOOST] = v }
    }

    suspend fun setFxTrebleCut(v: Float) {
        context.settingsDataStore.edit { it[Keys.FX_TREBLE_CUT] = v }
    }

    suspend fun setFxDrive(v: Float) {
        context.settingsDataStore.edit { it[Keys.FX_DRIVE] = v }
    }

    suspend fun setGainBoost(v: Float) {
        context.settingsDataStore.edit { it[Keys.GAIN_BOOST] = v }
    }

    suspend fun setPreviewSoundsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.PREVIEW_SOUNDS] = enabled }
    }

    suspend fun setAllowLongSounds(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ALLOW_LONG_SOUNDS] = enabled }
    }

    suspend fun setMaxSoundDurationMs(ms: Int) {
        context.settingsDataStore.edit { it[Keys.MAX_SOUND_DURATION_MS] = ms }
    }

    suspend fun setAllowSimultaneousSounds(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ALLOW_SIMULTANEOUS_SOUNDS] = enabled }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setReclipServerUrl(url: String) {
        context.settingsDataStore.edit { it[Keys.RECLIP_SERVER_URL] = url }
    }

    suspend fun setReclipUsername(username: String) {
        context.settingsDataStore.edit { it[Keys.RECLIP_USERNAME] = username }
    }

    suspend fun setReclipPassword(password: String) {
        context.settingsDataStore.edit { it[Keys.RECLIP_PASSWORD] = password }
    }

    suspend fun resetToDefaults() {
        context.settingsDataStore.edit { it.clear() }
    }
}
