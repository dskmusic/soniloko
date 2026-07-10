package com.dsk.soniloko.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/** Shared by the recording, YouTube-download and "edit trim" flows — no re-encoding. */
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

    /** Cuts [sourceFile] down to [startMs]..[endMs]. Deletes [sourceFile]; returns the new file. */
    fun trim(sourceFile: File, startMs: Long, endMs: Long): File? = runCatching {
        val outFile = File(sourceFile.parentFile, "trimmed_${sourceFile.name}")
        val extractor = MediaExtractor()
        extractor.setDataSource(sourceFile.absolutePath)

        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = f
                break
            }
        }
        if (trackIndex < 0 || format == null) {
            extractor.release()
            return null
        }
        extractor.selectTrack(trackIndex)
        extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val outTrack = muxer.addTrack(format)
        muxer.start()

        val buffer = ByteBuffer.allocate(1 shl 20)
        val bufferInfo = MediaCodec.BufferInfo()
        val endUs = endMs * 1000

        while (true) {
            val sampleTime = extractor.sampleTime
            if (sampleTime < 0 || sampleTime > endUs) break
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            bufferInfo.offset = 0
            bufferInfo.size = size
            bufferInfo.presentationTimeUs = (sampleTime - startMs * 1000).coerceAtLeast(0)
            bufferInfo.flags = extractor.sampleFlags
            muxer.writeSampleData(outTrack, buffer, bufferInfo)
            extractor.advance()
        }

        muxer.stop()
        muxer.release()
        extractor.release()
        sourceFile.delete()
        outFile
    }.getOrNull()

    /** Trims an already-saved own sound in place, preserving its exact file name (so buttons
     * that already reference it keep working). */
    fun trimInPlace(file: File, startMs: Long, endMs: Long): Boolean {
        val originalName = file.name
        val trimmed = trim(file, startMs, endMs) ?: return false
        val restored = File(file.parentFile, originalName)
        return trimmed.renameTo(restored)
    }
}
