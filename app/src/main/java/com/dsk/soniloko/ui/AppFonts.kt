package com.dsk.soniloko.ui

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

object AppFonts {
    private const val FA_ASSET_PATH = "fonts/fa-solid-900.otf"

    /** Returns null if the FontAwesome font hasn't been added to assets/fonts yet. */
    fun loadFontAwesome(context: Context): FontFamily? = runCatching {
        FontFamily(Font(FA_ASSET_PATH, context.assets))
    }.getOrNull()
}
