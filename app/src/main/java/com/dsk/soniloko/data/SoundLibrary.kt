package com.dsk.soniloko.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Bundled sounds live in assets/sounds. Sounds recorded or imported by the user live in
 * SoniLoko/sonidos_propios (see [AppStorage]) — real files, no app-private copies.
 */
object SoundLibrary {
    private const val ASSETS_SOUNDS_DIR = "sounds"
    private const val HIDDEN_PREFS = "soniloko_hidden_sounds"
    private const val HIDDEN_KEY = "hidden"
    private val supportedExtensions = listOf(".mp3", ".wav", ".ogg", ".opus", ".m4a")

    fun listAvailableSounds(context: Context): List<String> {
        val bundled = context.assets.list(ASSETS_SOUNDS_DIR)?.filter { isAudioFile(it) } ?: emptyList()
        return (bundled + listOwnSounds(context)).distinct().sorted()
    }

    fun listOwnSounds(context: Context): List<String> {
        val folder = AppStorage.ownSoundsFolder() ?: return emptyList()
        val hidden = hiddenSounds(context)
        return folder.listFiles()
            ?.filter { it.isFile && isAudioFile(it.name) && it.name !in hidden }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    private fun hiddenSounds(context: Context): Set<String> =
        context.getSharedPreferences(HIDDEN_PREFS, Context.MODE_PRIVATE).getStringSet(HIDDEN_KEY, emptySet()) ?: emptySet()

    /** Removes a sound from the picker list without touching the physical file. */
    fun removeFromList(context: Context, fileName: String) {
        val prefs = context.getSharedPreferences(HIDDEN_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(HIDDEN_KEY, hiddenSounds(context) + fileName).apply()
    }

    fun isOwnSound(fileName: String): Boolean =
        AppStorage.ownSoundsFolder()?.let { File(it, fileName).isFile } ?: false

    fun resolveOwnSoundFile(fileName: String): File? {
        val folder = AppStorage.ownSoundsFolder() ?: return null
        val file = File(folder, fileName)
        return if (file.isFile) file else null
    }

    /** Copies a device-picked audio file into SoniLoko/sonidos_propios. Returns the saved file name. */
    fun importSound(context: Context, uri: Uri): String? = runCatching {
        val folder = AppStorage.ownSoundsFolder() ?: return null
        val displayName = queryDisplayName(context, uri) ?: "sound_${UUID.randomUUID()}.m4a"
        val target = uniqueTarget(folder, sanitizeFileName(displayName))
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target.name
    }.getOrNull()

    /** Copies a local temp recording into SoniLoko/sonidos_propios under [desiredName]. */
    fun saveRecording(tempFile: File, desiredName: String): String? = runCatching {
        val folder = AppStorage.ownSoundsFolder() ?: return null
        val target = uniqueTarget(folder, "${sanitizeFileName(desiredName)}.m4a")
        tempFile.copyTo(target, overwrite = false)
        tempFile.delete()
        target.name
    }.getOrNull()

    fun deleteOwnSound(fileName: String): Boolean {
        val folder = AppStorage.ownSoundsFolder() ?: return false
        return File(folder, fileName).delete()
    }

    /** Renames an own sound, keeping its extension. Returns the actual final file name (made
     * unique if needed), or null on failure. */
    fun renameOwnSound(oldName: String, newBaseName: String): String? {
        val folder = AppStorage.ownSoundsFolder() ?: return null
        val source = File(folder, oldName)
        if (!source.isFile) return null
        val ext = oldName.substringAfterLast('.', "")
        val cleanBase = sanitizeFileName(newBaseName).ifBlank { return null }
        val desiredName = if (ext.isNotEmpty()) "$cleanBase.$ext" else cleanBase
        if (desiredName == oldName) return oldName
        val target = uniqueTarget(folder, desiredName)
        return if (source.renameTo(target)) target.name else null
    }

    /** Zips every own sound file into [destUri]. Returns how many files were written, or null on failure. */
    fun exportOwnSoundsZip(context: Context, destUri: Uri): Int? = runCatching {
        val folder = AppStorage.ownSoundsFolder() ?: return null
        val files = folder.listFiles()?.filter { it.isFile && isAudioFile(it.name) } ?: emptyList()
        context.contentResolver.openOutputStream(destUri)?.use { out ->
            ZipOutputStream(out).use { zip ->
                files.forEach { file ->
                    zip.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: return null
        files.size
    }.getOrNull()

    /** Extracts every audio entry from [srcUri] into sonidos_propios. When [replace] is true,
     * existing own sounds are deleted first instead of adding alongside them. Returns how many
     * were imported, or null on failure. */
    fun importOwnSoundsZip(context: Context, srcUri: Uri, replace: Boolean): Int? = runCatching {
        val folder = AppStorage.ownSoundsFolder() ?: return null
        if (replace) folder.listFiles()?.forEach { if (it.isFile) it.delete() }
        var count = 0
        context.contentResolver.openInputStream(srcUri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val entryName = entry.name.substringAfterLast('/')
                    if (!entry.isDirectory && isAudioFile(entryName)) {
                        val target = uniqueTarget(folder, sanitizeFileName(entryName))
                        target.outputStream().use { out -> zip.copyTo(out) }
                        count++
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: return null
        count
    }.getOrNull()

    private fun isAudioFile(name: String) = supportedExtensions.any { name.endsWith(it, ignoreCase = true) }

    private fun sanitizeFileName(name: String): String = name.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun uniqueTarget(folder: File, name: String): File {
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var candidate = File(folder, name)
        var counter = 1
        while (candidate.exists()) {
            candidate = File(folder, if (ext.isNotEmpty()) "${base}_$counter.$ext" else "${base}_$counter")
            counter++
        }
        return candidate
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }.getOrNull()
}
