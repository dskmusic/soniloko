package com.dsk.soniloko.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private const val DSK_URL = "https://www.dskmusic.com/dsk_dev_redirect.php"

@Composable
fun Footer(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val onBackground = MaterialTheme.colorScheme.onBackground
    val linkColor = MaterialTheme.colorScheme.primary
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = onBackground)) {
            append("Made with ❤️ by ")
        }
        pushStringAnnotation(tag = "DSK", annotation = DSK_URL)
        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
            append("DSK")
        }
        pop()
    }
    ClickableText(
        text = text,
        style = TextStyle(textAlign = TextAlign.Center),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        onClick = { offset ->
            text.getStringAnnotations("DSK", offset, offset).firstOrNull()?.let {
                uriHandler.openUri(it.item)
            }
        }
    )
}
