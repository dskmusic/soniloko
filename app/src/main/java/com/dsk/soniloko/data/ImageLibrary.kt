package com.dsk.soniloko.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Custom button images live in SoniLoko/imagenes_propias as real JPEG/PNG files (see
 * [AppStorage]), mirroring how user sounds are stored — referenced by file name from
 * [SoundButtonConfig], never held inline as base64 in the day-to-day board/kits JSON.
 */
object ImageLibrary {
    private val supportedExtensions = listOf(".jpg", ".jpeg", ".png")

    fun resolveImageFile(fileName: String): File? {
        val folder = AppStorage.ownImagesFolder() ?: return null
        val file = File(folder, fileName)
        return if (file.isFile) file else null
    }

    /** Compresses and saves a cropped bitmap in imagenes_propias. Images with an alpha channel
     * (e.g. illustrations/vectors downloaded with a transparent background) are saved as PNG to
     * keep the transparency; everything else as JPEG. Returns the file name. */
    fun saveImage(bitmap: Bitmap): String? = runCatching {
        val folder = AppStorage.ownImagesFolder() ?: return null
        val keepTransparency = bitmap.hasAlpha()
        val ext = if (keepTransparency) "png" else "jpg"
        val format = if (keepTransparency) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val target = uniqueTarget(folder, "img_${UUID.randomUUID()}.$ext")
        target.outputStream().use { out -> bitmap.compress(format, 90, out) }
        target.name
    }.getOrNull()

    fun deleteImage(fileName: String): Boolean {
        val folder = AppStorage.ownImagesFolder() ?: return false
        return File(folder, fileName).delete()
    }

    /** Reads an own image file back out as base64 — used only to embed images in the portable
     * full-config JSON export, so a restored config is self-contained on another device. */
    fun encodeFileAsBase64(fileName: String): String? = resolveImageFile(fileName)?.let { file ->
        runCatching { Base64.encodeToString(file.readBytes(), Base64.NO_WRAP) }.getOrNull()
    }

    /** Writes base64 image data (from an imported portable config) out as a new file. Returns the file name. */
    fun saveBase64AsNewFile(base64: String): String? = runCatching {
        val folder = AppStorage.ownImagesFolder() ?: return null
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val target = uniqueTarget(folder, "img_${UUID.randomUUID()}.jpg")
        target.writeBytes(bytes)
        target.name
    }.getOrNull()

    /** Zips every own image file into [destUri]. Returns how many files were written, or null on failure. */
    fun exportOwnImagesZip(context: Context, destUri: Uri): Int? = runCatching {
        val folder = AppStorage.ownImagesFolder() ?: return null
        val files = folder.listFiles()?.filter { it.isFile && isImageFile(it.name) } ?: emptyList()
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

    /** Extracts every image entry from [srcUri] into imagenes_propias. When [replace] is true,
     * existing images are deleted first instead of adding alongside them. Returns how many were
     * imported, or null on failure. */
    fun importOwnImagesZip(context: Context, srcUri: Uri, replace: Boolean): Int? = runCatching {
        val folder = AppStorage.ownImagesFolder() ?: return null
        if (replace) folder.listFiles()?.forEach { if (it.isFile) it.delete() }
        var count = 0
        context.contentResolver.openInputStream(srcUri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val entryName = entry.name.substringAfterLast('/')
                    if (!entry.isDirectory && isImageFile(entryName)) {
                        val target = uniqueTarget(folder, entryName)
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

    private fun isImageFile(name: String) = supportedExtensions.any { name.endsWith(it, ignoreCase = true) }

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
}
