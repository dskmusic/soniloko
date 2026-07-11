package com.dsk.soniloko.data.model

import java.util.Locale

/** A preset board layout read from assets/kits.json — applying one replaces all 12 buttons. */
data class SoundKit(
    val id: String,
    val namesByLang: Map<String, String>,
    val buttons: List<SoundButtonConfig>
) {
    fun displayName(): String {
        val lang = Locale.getDefault().language
        return namesByLang[lang] ?: namesByLang["en"] ?: namesByLang.values.firstOrNull() ?: id
    }
}

data class SoundButtonConfig(
    val id: Int,
    val iconName: String,
    val soundFile: String,
    val volume: Float = 1f,
    /** File name of a JPEG in SoniLoko/imagenes_propias (see ImageLibrary). Takes priority over
     * [customText] and the icon when set. */
    val customImageFile: String? = null,
    /** Custom label shown on the button. Takes priority over the icon, but not over [customImageFile]. */
    val customText: String? = null
)

data class AppSettings(
    val language: String = "system",
    val masterVolume: Float = 1f,
    val theme: String = "classic_red",
    val toyFxEnabled: Boolean = true,
    /** SoundBox Fx intensity knobs, each 0f (flat/off) to 1f (maximum). */
    val fxBassCut: Float = 1f,
    val fxMidBoost: Float = 1f,
    val fxTrebleCut: Float = 1f,
    val fxDrive: Float = 0.6f,
    /** Extra loudness boost independent of Fx, 0f (none) to 1f (max safe boost). */
    val gainBoost: Float = 0f,
    /** Whether tapping a sound in the sound picker plays it as a preview. */
    val previewSoundsEnabled: Boolean = true,
    /** When false, sounds longer than [maxSoundDurationMs] are cut off at that point. */
    val allowLongSounds: Boolean = true,
    val maxSoundDurationMs: Int = 5000,
    /** When false, starting a new sound stops whatever else is currently playing (same button repeated, or another button). */
    val allowSimultaneousSounds: Boolean = true,
    /** Short vibration on every button tap. Works even in silent/vibrate ringer mode. */
    val hapticFeedbackEnabled: Boolean = true,
    /** Optional self-hosted reclip server, tried before falling back to on-device extraction
     * when downloading a sound found via online search. Empty [reclipServerUrl] disables it. */
    val reclipServerUrl: String = "",
    val reclipUsername: String = "",
    val reclipPassword: String = ""
)
