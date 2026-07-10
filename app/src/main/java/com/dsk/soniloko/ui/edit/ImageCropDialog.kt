package com.dsk.soniloko.ui.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dsk.soniloko.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

private const val OUTPUT_SIZE = 512
private val VIEWPORT_SIZE = 240.dp

@Composable
fun ImageCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropped: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var source by remember(imageUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUri) {
        source = withContext(Dispatchers.IO) { loadBitmap(context, imageUri) }
        if (source == null) onDismiss()
    }

    val bitmap = source ?: return
    val density = LocalDensity.current
    val viewportPx = with(density) { VIEWPORT_SIZE.toPx() }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    fun clampedOffset(candidate: Offset, currentScale: Float): Offset {
        val baseScale = max(viewportPx / bitmap.width, viewportPx / bitmap.height)
        val totalScale = baseScale * currentScale
        val displayedW = bitmap.width * totalScale
        val displayedH = bitmap.height * totalScale
        val maxX = max(0f, (displayedW - viewportPx) / 2f)
        val maxY = max(0f, (displayedH - viewportPx) / 2f)
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 4.dp) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.crop_image_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.crop_image_hint), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .size(VIEWPORT_SIZE)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black)
                        .pointerInput(bitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                                offset = clampedOffset(offset + pan, newScale)
                            }
                        }
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        onCropped(cropToBitmap(bitmap, viewportPx, scale, offset))
                    }) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

private fun loadBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.setTargetSampleSize(1)
            decoder.isMutableRequired = false
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}.getOrNull()

/** Maps the visible viewport region back to the source bitmap's pixel space and crops it. */
private fun cropToBitmap(source: Bitmap, viewportPx: Float, scale: Float, offset: Offset): Bitmap {
    val bw = source.width.toFloat()
    val bh = source.height.toFloat()
    val baseScale = max(viewportPx / bw, viewportPx / bh)
    val totalScale = baseScale * scale
    val displayedW = bw * totalScale
    val displayedH = bh * totalScale
    val topLeftX = (viewportPx - displayedW) / 2f + offset.x
    val topLeftY = (viewportPx - displayedH) / 2f + offset.y

    val srcLeft = ((-topLeftX) / totalScale).coerceIn(0f, bw)
    val srcTop = ((-topLeftY) / totalScale).coerceIn(0f, bh)
    val srcRight = ((viewportPx - topLeftX) / totalScale).coerceIn(0f, bw)
    val srcBottom = ((viewportPx - topLeftY) / totalScale).coerceIn(0f, bh)

    val srcX = srcLeft.toInt()
    val srcY = srcTop.toInt()
    val srcW = (srcRight - srcLeft).toInt().coerceIn(1, source.width - srcX)
    val srcH = (srcBottom - srcTop).toInt().coerceIn(1, source.height - srcY)

    val cropped = Bitmap.createBitmap(source, srcX, srcY, srcW, srcH)
    return Bitmap.createScaledBitmap(cropped, OUTPUT_SIZE, OUTPUT_SIZE, true)
}
