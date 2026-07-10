package com.dsk.soniloko.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

/** Decodes an audio file into a small array of peak-amplitude buckets, for drawing a waveform. */
object AudioWaveform {

    /** [durationMs] must match what the trim slider uses, so buckets line up with the selection. */
    fun extract(file: File, durationMs: Int, buckets: Int = 120): FloatArray {
        val result = FloatArray(buckets)
        if (durationMs <= 0) return result
        val totalUs = durationMs.toLong() * 1000

        runCatching {
            val extractor = MediaExtractor()
            extractor.setDataSource(file.absolutePath)

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
                return@runCatching
            }
            extractor.selectTrack(trackIndex)

            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEOS = false
            var sawOutputEOS = false
            val timeoutUs = 10_000L
            var guard = 0

            while (!sawOutputEOS && guard < 500_000) {
                guard++
                if (!sawInputEOS) {
                    val inIndex = codec.dequeueInputBuffer(timeoutUs)
                    if (inIndex >= 0) {
                        val inBuf = codec.getInputBuffer(inIndex)
                        val sampleSize = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEOS = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (outIndex >= 0) {
                    if (bufferInfo.size > 0) {
                        codec.getOutputBuffer(outIndex)?.let { buf ->
                            buf.order(ByteOrder.LITTLE_ENDIAN)
                            buf.position(bufferInfo.offset)
                            buf.limit(bufferInfo.offset + bufferInfo.size)
                            val shortBuf = buf.asShortBuffer()
                            val n = shortBuf.remaining()
                            if (n > 0) {
                                var peak = 0f
                                val step = max(1, n / 200)
                                var i = 0
                                while (i < n) {
                                    val v = abs(shortBuf.get(i).toInt()) / 32768f
                                    if (v > peak) peak = v
                                    i += step
                                }
                                val bucket = ((bufferInfo.presentationTimeUs.toFloat() / totalUs) * buckets)
                                    .toInt().coerceIn(0, buckets - 1)
                                if (peak > result[bucket]) result[bucket] = peak
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawOutputEOS = true
                } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER && sawInputEOS) {
                    sawOutputEOS = true
                }
            }

            codec.stop()
            codec.release()
            extractor.release()
        }

        val maxVal = result.maxOrNull() ?: 0f
        if (maxVal > 0.01f) {
            for (i in result.indices) result[i] = (result[i] / maxVal).coerceIn(0f, 1f)
        }
        return result
    }
}
