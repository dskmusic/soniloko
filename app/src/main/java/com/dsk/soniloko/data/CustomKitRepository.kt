package com.dsk.soniloko.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dsk.soniloko.data.model.SoundKit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.customKitsDataStore by preferencesDataStore(name = "soniloko_custom_kits")

/** User-created kits, kept separate from the read-only bundled kits in assets/kits.json. */
class CustomKitRepository(private val context: Context) {
    private val key = stringPreferencesKey("custom_kits_json")

    val customKits: Flow<List<SoundKit>> = context.customKitsDataStore.data.map { prefs ->
        prefs[key]?.let { JsonMapper.decodeKits(it) } ?: emptyList()
    }

    suspend fun addKit(kit: SoundKit) {
        saveAll(customKits.first() + kit)
    }

    suspend fun renameKit(id: String, newName: String) {
        val renamed = customKits.first().map {
            if (it.id == id) it.copy(namesByLang = mapOf("es" to newName, "en" to newName)) else it
        }
        saveAll(renamed)
    }

    suspend fun deleteKit(id: String) {
        saveAll(customKits.first().filterNot { it.id == id })
    }

    suspend fun replaceAll(kits: List<SoundKit>) {
        saveAll(kits)
    }

    suspend fun clearAll() {
        saveAll(emptyList())
    }

    private suspend fun saveAll(kits: List<SoundKit>) {
        context.customKitsDataStore.edit { it[key] = JsonMapper.encodeKits(kits) }
    }
}
