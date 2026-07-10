package com.dsk.soniloko.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dsk.soniloko.data.model.SoundButtonConfig
import com.dsk.soniloko.data.model.SoundKit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.boardDataStore by preferencesDataStore(name = "soniloko_board")

class BoardRepository(private val context: Context) {
    private val boardKey = stringPreferencesKey("board_json")

    val board: Flow<List<SoundButtonConfig>> = context.boardDataStore.data.map { prefs ->
        prefs[boardKey]?.let { JsonMapper.decodeBoard(it) } ?: loadDefaultFromAssets()
    }

    /** The "classic" kit in kits.json doubles as the factory-default board — one source of truth. */
    private fun loadDefaultFromAssets(): List<SoundButtonConfig> {
        val kits = loadKits()
        return kits.firstOrNull { it.id == "classic" }?.buttons ?: kits.firstOrNull()?.buttons ?: emptyList()
    }

    suspend fun saveBoard(buttons: List<SoundButtonConfig>) {
        context.boardDataStore.edit { it[boardKey] = JsonMapper.encodeBoard(buttons) }
    }

    suspend fun resetToDefault() {
        saveBoard(loadDefaultFromAssets())
    }

    /** Preset board layouts defined in assets/kits.json — add or reorder kits by editing that file. */
    fun loadKits(): List<SoundKit> = runCatching {
        JsonMapper.decodeKits(context.assets.open("kits.json").bufferedReader().use { it.readText() })
    }.getOrDefault(emptyList())
}
