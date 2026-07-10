package com.dsk.soniloko.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Shrinks [text] to fit the available space (down to [minFontSize]), falling back to an
 * ellipsis if it still doesn't fit at the smallest size — "adjust as much as possible, or trim
 * if needed".
 */
@Composable
fun AutoSizeText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 18.sp,
    minFontSize: TextUnit = 9.sp
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    var settled by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier,
        onTextLayout = { result ->
            if (settled) return@Text
            if ((result.didOverflowWidth || result.didOverflowHeight) && fontSize > minFontSize) {
                fontSize = (fontSize.value - 1).sp
            } else {
                settled = true
            }
        }
    )
}
