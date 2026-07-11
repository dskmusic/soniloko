package com.dsk.soniloko.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import java.io.File

/**
 * Manages the app's "SoniLoko" folder at the root of shared storage
 * (/storage/emulated/0/SoniLoko). Requires MANAGE_EXTERNAL_STORAGE — acceptable for this
 * personal, non-Play-Store app — so once granted, no folder picker is ever shown: the app
 * creates/reads the folder directly with plain file I/O.
 */
object AppStorage {
    private const val ROOT_FOLDER_NAME = "SoniLoko"
    private const val OWN_SOUNDS_FOLDER_NAME = "sonidos_propios"
    private const val OWN_IMAGES_FOLDER_NAME = "imagenes_propias"

    fun hasAllFilesAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true

    fun requestAllFilesAccessIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        }

    fun soniLokoFolder(): File? {
        if (!hasAllFilesAccess()) return null
        val root = File(Environment.getExternalStorageDirectory(), ROOT_FOLDER_NAME)
        if (!root.exists() && !root.mkdirs()) return null
        return root
    }

    fun ownSoundsFolder(): File? {
        val root = soniLokoFolder() ?: return null
        val own = File(root, OWN_SOUNDS_FOLDER_NAME)
        if (!own.exists() && !own.mkdirs()) return null
        return own
    }

    fun ownImagesFolder(): File? {
        val root = soniLokoFolder() ?: return null
        val own = File(root, OWN_IMAGES_FOLDER_NAME)
        if (!own.exists() && !own.mkdirs()) return null
        return own
    }

    /**
     * Best-effort hint URI so the system file picker opens at /SoniLoko by default. Uses the
     * standard AOSP external storage provider's document URI scheme for the primary volume —
     * not guaranteed on every OEM file picker, but the user can always navigate elsewhere anyway.
     */
    fun soniLokoDocumentUriHint(): Uri =
        DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:$ROOT_FOLDER_NAME")
}
