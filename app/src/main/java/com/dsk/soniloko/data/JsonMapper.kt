package com.dsk.soniloko.data

import com.dsk.soniloko.data.model.AppSettings
import com.dsk.soniloko.data.model.SoundButtonConfig
import com.dsk.soniloko.data.model.SoundKit
import org.json.JSONArray
import org.json.JSONObject

object JsonMapper {
    private fun buttonsToJsonArray(buttons: List<SoundButtonConfig>): JSONArray =
        JSONArray().apply {
            buttons.forEach { b ->
                put(
                    JSONObject().apply {
                        put("id", b.id)
                        put("icon", b.iconName)
                        put("sound", b.soundFile)
                        put("volume", b.volume.toDouble())
                        put("image", b.customImageBase64 ?: JSONObject.NULL)
                        put("text", b.customText ?: JSONObject.NULL)
                    }
                )
            }
        }

    private fun jsonArrayToButtons(arr: JSONArray): List<SoundButtonConfig> =
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SoundButtonConfig(
                id = o.getInt("id"),
                iconName = o.getString("icon"),
                soundFile = o.getString("sound"),
                volume = o.optDouble("volume", 1.0).toFloat(),
                customImageBase64 = if (o.isNull("image")) null else o.optString("image").takeIf { it.isNotBlank() },
                customText = if (o.isNull("text")) null else o.optString("text").takeIf { it.isNotBlank() }
            )
        }

    private fun kitsToJsonArray(kits: List<SoundKit>): JSONArray =
        JSONArray().apply {
            kits.forEach { kit ->
                put(
                    JSONObject().apply {
                        put("id", kit.id)
                        put("name", JSONObject().apply { kit.namesByLang.forEach { (lang, name) -> put(lang, name) } })
                        put("buttons", buttonsToJsonArray(kit.buttons))
                    }
                )
            }
        }

    private fun jsonArrayToKits(arr: JSONArray): List<SoundKit> =
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val nameObj = o.getJSONObject("name")
            val names = mutableMapOf<String, String>()
            val keys = nameObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                names[key] = nameObj.getString(key)
            }
            SoundKit(
                id = o.getString("id"),
                namesByLang = names,
                buttons = jsonArrayToButtons(o.getJSONArray("buttons"))
            )
        }

    fun encodeBoard(buttons: List<SoundButtonConfig>): String =
        JSONObject().put("buttons", buttonsToJsonArray(buttons)).toString()

    fun decodeBoard(json: String): List<SoundButtonConfig> =
        jsonArrayToButtons(JSONObject(json).getJSONArray("buttons"))

    fun encodeKits(kits: List<SoundKit>): String =
        JSONObject().put("kits", kitsToJsonArray(kits)).toString()

    fun decodeKits(json: String): List<SoundKit> =
        jsonArrayToKits(JSONObject(json).getJSONArray("kits"))

    fun encodeFullConfig(
        settings: AppSettings,
        buttons: List<SoundButtonConfig>,
        customKits: List<SoundKit>
    ): String {
        val root = JSONObject()
        root.put(
            "settings",
            JSONObject().apply {
                put("language", settings.language)
                put("masterVolume", settings.masterVolume.toDouble())
                put("theme", settings.theme)
                put("toyFxEnabled", settings.toyFxEnabled)
                put("fxBassCut", settings.fxBassCut.toDouble())
                put("fxMidBoost", settings.fxMidBoost.toDouble())
                put("fxTrebleCut", settings.fxTrebleCut.toDouble())
                put("fxDrive", settings.fxDrive.toDouble())
                put("gainBoost", settings.gainBoost.toDouble())
                put("previewSoundsEnabled", settings.previewSoundsEnabled)
                put("allowLongSounds", settings.allowLongSounds)
                put("maxSoundDurationMs", settings.maxSoundDurationMs)
                put("allowSimultaneousSounds", settings.allowSimultaneousSounds)
                put("hapticFeedbackEnabled", settings.hapticFeedbackEnabled)
                // Deliberately excludes reclipUsername/reclipPassword — this file may get backed
                // up/shared, and credentials shouldn't ride along in a portable config export.
                put("reclipServerUrl", settings.reclipServerUrl)
            }
        )
        root.put("buttons", buttonsToJsonArray(buttons))
        root.put("customKits", kitsToJsonArray(customKits))
        return root.toString(2)
    }

    data class FullConfig(
        val settings: AppSettings,
        val buttons: List<SoundButtonConfig>,
        val customKits: List<SoundKit>
    )

    fun decodeFullConfig(json: String): FullConfig {
        val root = JSONObject(json)
        val s = root.optJSONObject("settings")
        val defaults = AppSettings()
        val settings = AppSettings(
            language = s?.optString("language", defaults.language) ?: defaults.language,
            masterVolume = s?.optDouble("masterVolume", defaults.masterVolume.toDouble())?.toFloat() ?: defaults.masterVolume,
            theme = s?.optString("theme", defaults.theme) ?: defaults.theme,
            toyFxEnabled = s?.optBoolean("toyFxEnabled", defaults.toyFxEnabled) ?: defaults.toyFxEnabled,
            fxBassCut = s?.optDouble("fxBassCut", defaults.fxBassCut.toDouble())?.toFloat() ?: defaults.fxBassCut,
            fxMidBoost = s?.optDouble("fxMidBoost", defaults.fxMidBoost.toDouble())?.toFloat() ?: defaults.fxMidBoost,
            fxTrebleCut = s?.optDouble("fxTrebleCut", defaults.fxTrebleCut.toDouble())?.toFloat() ?: defaults.fxTrebleCut,
            fxDrive = s?.optDouble("fxDrive", defaults.fxDrive.toDouble())?.toFloat() ?: defaults.fxDrive,
            gainBoost = s?.optDouble("gainBoost", defaults.gainBoost.toDouble())?.toFloat() ?: defaults.gainBoost,
            previewSoundsEnabled = s?.optBoolean("previewSoundsEnabled", defaults.previewSoundsEnabled) ?: defaults.previewSoundsEnabled,
            allowLongSounds = s?.optBoolean("allowLongSounds", defaults.allowLongSounds) ?: defaults.allowLongSounds,
            maxSoundDurationMs = s?.optInt("maxSoundDurationMs", defaults.maxSoundDurationMs) ?: defaults.maxSoundDurationMs,
            allowSimultaneousSounds = s?.optBoolean("allowSimultaneousSounds", defaults.allowSimultaneousSounds) ?: defaults.allowSimultaneousSounds,
            hapticFeedbackEnabled = s?.optBoolean("hapticFeedbackEnabled", defaults.hapticFeedbackEnabled) ?: defaults.hapticFeedbackEnabled,
            reclipServerUrl = s?.optString("reclipServerUrl", defaults.reclipServerUrl) ?: defaults.reclipServerUrl
        )
        val buttons = jsonArrayToButtons(root.getJSONArray("buttons"))
        val customKits = root.optJSONArray("customKits")?.let { jsonArrayToKits(it) } ?: emptyList()
        return FullConfig(settings, buttons, customKits)
    }
}
