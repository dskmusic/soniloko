package com.dsk.soniloko.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dsk.soniloko.data.model.SoundButtonConfig
import com.dsk.soniloko.data.model.SoundKit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.boardDataStore by preferencesDataStore(name = "soniloko_board")

class BoardRepository(private val context: Context) {
    private val boardKey = stringPreferencesKey("board_json")
    private val currentKitIdKey = stringPreferencesKey("current_kit_id")
    private val kitOverridesKey = stringPreferencesKey("kit_overrides_json")

    val board: Flow<List<SoundButtonConfig>> = context.boardDataStore.data.map { prefs ->
        prefs[boardKey]?.let { JsonMapper.decodeBoard(it) } ?: loadDefaultFromAssets()
    }

    /** The "classic" kit in kits.json doubles as the factory-default board — one source of truth. */
    private fun loadDefaultFromAssets(): List<SoundButtonConfig> {
        val kits = loadKits()
        return kits.firstOrNull { it.id == "classic" }?.buttons ?: kits.firstOrNull()?.buttons ?: emptyList()
    }

    private fun defaultKitId(): String? =
        loadKits().let { kits -> kits.firstOrNull { it.id == "classic" }?.id ?: kits.firstOrNull()?.id }

    private fun readOverrides(raw: String?): JSONObject =
        raw?.let { runCatching { JSONObject(it) }.getOrNull() } ?: JSONObject()

    /** Persists [buttons] as the live board and, tagged to whichever kit is currently active
     * (falling back to the default kit if the app was never explicitly switched), as that kit's
     * own override — so switching away and back restores the customization instead of reloading
     * the kit's pristine template from assets/kits.json. */
    suspend fun saveBoard(buttons: List<SoundButtonConfig>) {
        context.boardDataStore.edit { prefs ->
            prefs[boardKey] = JsonMapper.encodeBoard(buttons)
            val kitId = prefs[currentKitIdKey] ?: defaultKitId()
            if (kitId != null) {
                val overrides = readOverrides(prefs[kitOverridesKey])
                overrides.put(kitId, JsonMapper.encodeBoard(buttons))
                prefs[kitOverridesKey] = overrides.toString()
            }
        }
    }

    /** Switches the live board to [kit]: restores its saved override if the user previously
     * customized it, otherwise falls back to the kit's bundled/template buttons. */
    suspend fun applyKit(kit: SoundKit) {
        context.boardDataStore.edit { prefs ->
            val overrides = readOverrides(prefs[kitOverridesKey])
            val overrideJson = overrides.optString(kit.id, "")
            val buttons = if (overrideJson.isNotBlank()) JsonMapper.decodeBoard(overrideJson) else kit.buttons
            prefs[boardKey] = JsonMapper.encodeBoard(buttons)
            prefs[currentKitIdKey] = kit.id
        }
    }

    suspend fun resetToDefault() {
        context.boardDataStore.edit { prefs ->
            prefs[boardKey] = JsonMapper.encodeBoard(loadDefaultFromAssets())
            prefs.remove(currentKitIdKey)
            prefs.remove(kitOverridesKey)
        }
    }

    /** Preset board layouts defined in assets/kits.json — add or reorder kits by editing that file. */
    fun loadKits(): List<SoundKit> = runCatching {
        JsonMapper.decodeKits(context.assets.open("kits.json").bufferedReader().use { it.readText() })
    }.getOrDefault(emptyList())
}
