package com.dsk.soniloko.util

import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import com.dsk.soniloko.data.AppStorage

/**
 * Same as [ActivityResultContracts.CreateDocument], but hints the system picker to open at the
 * SoniLoko folder by default. The user can still navigate anywhere else in the picker.
 */
class CreateDocumentWithHint(mimeType: String) : ActivityResultContracts.CreateDocument(mimeType) {
    override fun createIntent(context: Context, input: String): Intent =
        super.createIntent(context, input).putExtra(DocumentsContract.EXTRA_INITIAL_URI, AppStorage.soniLokoDocumentUriHint())
}

/** Same as [ActivityResultContracts.OpenDocument], hinting the SoniLoko folder as the default location. */
class OpenDocumentWithHint : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input).putExtra(DocumentsContract.EXTRA_INITIAL_URI, AppStorage.soniLokoDocumentUriHint())
}
