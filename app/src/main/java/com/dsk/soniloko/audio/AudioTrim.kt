package com.dsk.soniloko.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Shared by the recording, YouTube-download and "edit trim" flows. Trims via Media3 Transformer
 * rather than a hand-rolled MediaExtractor/MediaMuxer remux: YouTube's downloaded audio is a
 * fragmented/DASH-sourced stream whose sample table a plain remux can't reliably seek/copy —
 * it either throws (caught, silently falling back to the untrimmed file) or writes a container
 * no real audio editor accepts. Transformer decodes/re-encodes as needed and handles both cases.
 */
object AudioTrim {

    fun loadDurationMs(file: File): Int = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull() ?: 0
        } finally {
            retriever.release()
        }
    }.getOrDefault(0)

    /** Cuts [sourceFile] down to [startMs]..[endMs]. Deletes [sourceFile] on success; returns the
     * new file, or null on failure (in which case [sourceFile] is left untouched). */
    suspend fun trim(context: Context, sourceFile: File, startMs: Long, endMs: Long): File? {
        val outFile = File(sourceFile.parentFile, "trimmed_${sourceFile.name}")
        outFile.delete()
        val ok = runCatching { export(context, sourceFile, outFile, startMs, endMs) }.getOrDefault(false)
        if (!ok) {
            outFile.delete()
            return null
        }
        sourceFile.delete()
        return outFile
    }

    /** Trims an already-saved own sound in place, preserving its exact file name (so buttons
     * that already reference it keep working). */
    suspend fun trimInPlace(context: Context, file: File, startMs: Long, endMs: Long): Boolean {
        val originalName = file.name
        val trimmed = trim(context, file, startMs, endMs) ?: return false
        val restored = File(file.parentFile, originalName)
        return trimmed.renameTo(restored)
    }

    // Transformer must be built and driven on a thread with a Looper — Dispatchers.Main
    // guarantees that regardless of which dispatcher the caller is on.
    private suspend fun export(
        context: Context,
        sourceFile: File,
        outFile: File,
        startMs: Long,
        endMs: Long
    ): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.fromFile(sourceFile))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
                .build()
            val editedItem = EditedMediaItem.Builder(mediaItem).setRemoveVideo(true).build()

            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (cont.isActive) cont.resume(true)
                    }
                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        if (cont.isActive) cont.resume(false)
                    }
                })
                .build()
            cont.invokeOnCancellation { transformer.cancel() }
            transformer.start(editedItem, outFile.absolutePath)
        }
    }
}
