package com.dsk.soniloko.ui.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dsk.soniloko.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val OUTPUT_SIZE = 512
private val VIEWPORT_SIZE = 240.dp
private const val BACKGROUND_COLOR_TOLERANCE = 42.0

@Composable
fun ImageCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropped: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var source by remember(imageUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUri) {
        val loaded = withContext(Dispatchers.IO) { loadBitmap(context, imageUri) }
        source = loaded?.let { withContext(Dispatchers.Default) { trimTransparentMargins(it) } }
        if (source == null) onDismiss()
    }

    val bitmap = source ?: return
    val density = LocalDensity.current
    val viewportPx = with(density) { VIEWPORT_SIZE.toPx() }

    // Lower zoom bound: lets the user pinch out far enough to see the whole source image
    // (letterboxed with transparent padding, see the checkerboard) instead of being locked at a
    // "cover" crop that can permanently hide part of a non-square image (e.g. a tall vector with
    // no way to pan far enough to reveal both ends at once).
    val minScale = remember(bitmap) { min(bitmap.width, bitmap.height).toFloat() / max(bitmap.width, bitmap.height) }
    // Defaults to a tight "cover" crop (fills the square, no transparent margin) like before;
    // `minScale` is still available as the pinch-out lower bound for anyone who wants to zoom out
    // and see the whole image. Keyed on `bitmap` so switching to a different image resets the
    // zoom/pan instead of carrying over whatever the previous image was left at.
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    var pickingColor by remember { mutableStateOf(false) }
    var backgroundColor by remember(imageUri) { mutableStateOf<Int?>(null) }
    var previewBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }

    // Chroma-keying the full-resolution source is a one-shot ~16M-pixel-worst-case loop, cheap
    // enough off the main thread — gives immediate visual feedback that the pick actually worked,
    // instead of only finding out after Guardar.
    LaunchedEffect(bitmap, backgroundColor) {
        previewBitmap = backgroundColor?.let { color ->
            withContext(Dispatchers.Default) { removeBackgroundColor(bitmap, color) }
        }
    }

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
                Text(
                    if (pickingColor) stringResource(R.string.picking_background_color_hint) else stringResource(R.string.crop_image_hint),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .size(VIEWPORT_SIZE)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(20.dp))
                        .drawBehind { drawCheckerboard(density) }
                        .pointerInput(bitmap, pickingColor) {
                            if (pickingColor) {
                                detectTapGestures { tap ->
                                    val (px, py) = viewportToSourcePixel(tap, bitmap, viewportPx, scale, offset)
                                    backgroundColor = bitmap.getPixel(px, py)
                                    pickingColor = false
                                }
                            } else {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(minScale, 5f)
                                    scale = newScale
                                    offset = clampedOffset(offset + pan, newScale)
                                }
                            }
                        }
                ) {
                    // Deliberately NOT using ContentScale.Crop here: Image applies that fit/clip
                    // itself at draw time, to its own fixed (viewportPx) bounds, BEFORE the
                    // graphicsLayer transform below ever runs. That meant every pan/zoom gesture
                    // was only ever moving/scaling the piece Crop had already thrown away the rest
                    // of — so dragging could never reveal anything beyond that first crop. Instead,
                    // size the Image ourselves to the exact "cover" dimensions (same baseScale math
                    // as viewportMapping/clampedOffset) and let ContentScale.FillBounds map the
                    // full bitmap onto that 1:1 — the graphicsLayer transform is then the only
                    // cropping that happens, and it operates on the complete image.
                    val baseScale = max(viewportPx / bitmap.width, viewportPx / bitmap.height)
                    val baseWidthDp = with(density) { (bitmap.width * baseScale).toDp() }
                    val baseHeightDp = with(density) { (bitmap.height * baseScale).toDp() }
                    Image(
                        bitmap = (previewBitmap ?: bitmap).asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .align(Alignment.Center)
                            // requiredSize, not size(): size() clamps to the incoming
                            // parent constraints (the 240dp viewport), which squashed the
                            // image back down to a square and stretched it to fill that —
                            // requiredSize ignores the parent's constraints, so the image
                            // is actually laid out at its real cover-fit dimensions, and
                            // the Box's own clip() is what crops the overflow to the
                            // viewport, not a squeeze of the image itself.
                            .requiredSize(baseWidthDp, baseHeightDp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { pickingColor = !pickingColor }) {
                        Text(
                            if (pickingColor) stringResource(R.string.cancel)
                            else stringResource(R.string.remove_background_color)
                        )
                    }
                    backgroundColor?.let { c ->
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                        TextButton(onClick = { backgroundColor = null }) {
                            Text(stringResource(R.string.clear_background_color))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val cropped = cropToBitmap(bitmap, viewportPx, scale, offset)
                        onCropped(backgroundColor?.let { removeBackgroundColor(cropped, it) } ?: cropped)
                    }) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

private val CHECKER_LIGHT = Color(0xFFDDDDDD)
private val CHECKER_DARK = Color(0xFFB0B0B0)
private val CHECKER_CELL = 12.dp

/** Renders a light/dark checkerboard (like GIMP/Photoshop) so real alpha transparency in the
 * source image is visually distinguishable from an opaque black/dark background. */
private fun DrawScope.drawCheckerboard(density: Density) {
    val cell = with(density) { CHECKER_CELL.toPx() }
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = 0f
        var col = 0
        while (x < size.width) {
            drawRect(
                color = if ((row + col) % 2 == 0) CHECKER_LIGHT else CHECKER_DARK,
                topLeft = Offset(x, y),
                size = Size(cell, cell)
            )
            x += cell
            col++
        }
        y += cell
        row++
    }
}

/** Many Pixabay vector renditions ship with real (baked-in) transparent margin around the
 * artwork's own bounding box — invisible in a browser preview, but wasted space once we cover-fit
 * the raw canvas into a square viewport, which crops more of the actual artwork than necessary.
 * Trims to the tight bounding box of non-transparent pixels (plus a small anti-alias margin) so
 * viewport math is based on the real content, not the padded canvas. */
private fun trimTransparentMargins(bitmap: Bitmap): Bitmap {
    if (!bitmap.hasAlpha()) return bitmap
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    var minX = w
    var minY = h
    var maxX = -1
    var maxY = -1
    for (y in 0 until h) {
        val rowStart = y * w
        for (x in 0 until w) {
            if ((pixels[rowStart + x] ushr 24) != 0) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }
    if (maxX < minX || maxY < minY) return bitmap

    val pad = 2
    val left = (minX - pad).coerceAtLeast(0)
    val top = (minY - pad).coerceAtLeast(0)
    val right = (maxX + pad).coerceAtMost(w - 1)
    val bottom = (maxY + pad).coerceAtMost(h - 1)
    if (left == 0 && top == 0 && right == w - 1 && bottom == h - 1) return bitmap

    return Bitmap.createBitmap(bitmap, left, top, right - left + 1, bottom - top + 1)
}

private fun loadBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.setTargetSampleSize(1)
            decoder.isMutableRequired = false
            // Default ImageDecoder allocator can produce a Config.HARDWARE bitmap, which crashes
            // (IllegalStateException) on any getPixel()/getPixels() call — needed by the
            // eyedropper's background-color pick and removal. Force a software-backed bitmap.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}.getOrNull()

/** topLeftX/topLeftY (viewport-space position of the source bitmap's top-left corner) and the
 * combined scale — shared by [cropToBitmap] and [viewportToSourcePixel] so both map between
 * viewport and source pixel space the same way. */
private fun viewportMapping(source: Bitmap, viewportPx: Float, scale: Float, offset: Offset): Triple<Float, Float, Float> {
    val bw = source.width.toFloat()
    val bh = source.height.toFloat()
    val baseScale = max(viewportPx / bw, viewportPx / bh)
    val totalScale = baseScale * scale
    val displayedW = bw * totalScale
    val displayedH = bh * totalScale
    val topLeftX = (viewportPx - displayedW) / 2f + offset.x
    val topLeftY = (viewportPx - displayedH) / 2f + offset.y
    return Triple(topLeftX, topLeftY, totalScale)
}

/** Maps a tap in the viewport (e.g. from the eyedropper) back to a pixel coordinate in [source]. */
private fun viewportToSourcePixel(tap: Offset, source: Bitmap, viewportPx: Float, scale: Float, offset: Offset): Pair<Int, Int> {
    val (topLeftX, topLeftY, totalScale) = viewportMapping(source, viewportPx, scale, offset)
    val px = ((tap.x - topLeftX) / totalScale).coerceIn(0f, source.width - 1f).toInt()
    val py = ((tap.y - topLeftY) / totalScale).coerceIn(0f, source.height - 1f).toInt()
    return px to py
}

/** Renders the exact visible viewport region into a square [OUTPUT_SIZE] canvas, matching what's
 * on screen (same [viewportMapping] transform) pixel for pixel. Drawing onto a blank transparent
 * canvas — rather than sub-bitmapping the source and force-scaling it to a square — means a
 * zoomed-out (non-covering) selection is padded with real transparency instead of being distorted
 * by stretching a non-square region into a square output. */
private fun cropToBitmap(source: Bitmap, viewportPx: Float, scale: Float, offset: Offset): Bitmap {
    val (topLeftX, topLeftY, totalScale) = viewportMapping(source, viewportPx, scale, offset)
    val outputScale = OUTPUT_SIZE / viewportPx

    val output = Bitmap.createBitmap(OUTPUT_SIZE, OUTPUT_SIZE, Bitmap.Config.ARGB_8888)
    val matrix = Matrix().apply {
        postScale(totalScale * outputScale, totalScale * outputScale)
        postTranslate(topLeftX * outputScale, topLeftY * outputScale)
    }
    Canvas(output).drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    return output
}

/** Chroma-keys [targetColor] out of [bitmap] (within [BACKGROUND_COLOR_TOLERANCE]), making
 * matching pixels fully transparent — the "eyedropper" background removal. */
private fun removeBackgroundColor(bitmap: Bitmap, targetColor: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    val tr = (targetColor shr 16) and 0xFF
    val tg = (targetColor shr 8) and 0xFF
    val tb = targetColor and 0xFF

    for (i in pixels.indices) {
        val p = pixels[i]
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        val dr = (r - tr).toDouble()
        val dg = (g - tg).toDouble()
        val db = (b - tb).toDouble()
        if (sqrt(dr * dr + dg * dg + db * db) <= BACKGROUND_COLOR_TOLERANCE) {
            pixels[i] = (r shl 16) or (g shl 8) or b
        }
    }

    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    // Bitmap.copy() preserves the source's opaque/has-alpha flag, not just its pixel config. Most
    // background images here start as opaque JPEGs, so without this the alpha=0 bytes just written
    // above get ignored wholesale at both render and save time (treated as fully opaque either way).
    result.setHasAlpha(true)
    result.setPixels(pixels, 0, w, 0, 0, w, h)
    return result
}
