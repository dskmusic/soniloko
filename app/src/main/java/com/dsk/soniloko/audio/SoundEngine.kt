package com.dsk.soniloko.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Handler
import android.os.Looper
import com.dsk.soniloko.data.SoundLibrary
import java.util.concurrent.ConcurrentHashMap

class SoundEngine(private val context: Context) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = mutableMapOf<String, Int>()
    private val loadedIds = mutableSetOf<Int>()
    private val pendingVolume = mutableMapOf<Int, Float>()
    // Written from a background thread (warmDurationCache) and read/written from the main
    // thread (play taps) concurrently — needs to be thread-safe.
    private val durationCache = ConcurrentHashMap<String, Long>()
    private val activeStreamIds = mutableSetOf<Int>()
    private var longSoundPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private val handler = Handler(Looper.getMainLooper())

    var masterVolume: Float = 1f
    private var allowLongSounds = true
    private var maxDurationMs = 5000
    private var allowSimultaneousSounds = true

    init {
        // SoundPool.load() is async: a sound isn't playable the instant load() returns.
        // Without this, the very first tap on a freshly-loaded sound is silently dropped.
        soundPool.setOnLoadCompleteListener { pool, sampleId, status ->
            if (status == 0) {
                loadedIds.add(sampleId)
                pendingVolume.remove(sampleId)?.let { vol -> pool.play(sampleId, vol, vol, 1, 0, 1f) }
            }
        }
    }

    /** Loads (without playing) every given sound file so taps aren't the first thing to trigger loading. */
    fun preload(fileNames: List<String>) {
        fileNames.distinct().forEach { fileName ->
            if (!soundIds.containsKey(fileName)) loadSound(fileName)
        }
    }

    /** Warms [durationCache] for every given file — call off the main thread. Without this,
     * the first tap on each sound pays for a synchronous MediaMetadataRetriever read (used to
     * decide SoundPool vs MediaPlayer) right before it plays, which is felt as tap-to-sound lag. */
    fun warmDurationCache(fileNames: List<String>) {
        fileNames.distinct().forEach { durationOf(it) }
    }

    /** Playback policy, all user-tunable from Settings. */
    fun setPlaybackPolicy(allowLongSounds: Boolean, maxDurationMs: Int, allowSimultaneousSounds: Boolean) {
        this.allowLongSounds = allowLongSounds
        this.maxDurationMs = maxDurationMs
        this.allowSimultaneousSounds = allowSimultaneousSounds
    }

    fun play(fileName: String, buttonVolume: Float) {
        val vol = (buttonVolume * masterVolume).coerceIn(0f, 1f)
        if (!allowSimultaneousSounds) stopAllActive()

        playInternal(fileName, vol)
    }

    /** Always stops whatever is currently playing first — used for auditioning sounds in
     * pickers, where overlapping previews are never wanted regardless of the simultaneous
     * sounds setting (that setting is about actual button taps, not previews). */
    fun playPreview(fileName: String) {
        stopAllActive()
        playInternal(fileName, masterVolume.coerceIn(0f, 1f))
    }

    private fun playInternal(fileName: String, vol: Float) {
        val duration = durationOf(fileName)
        if (allowLongSounds && duration != null && duration > LONG_SOUND_THRESHOLD_MS) {
            // SoundPool decodes the whole file to memory up front and isn't meant for long
            // clips; MediaPlayer streams it instead so it reliably plays start to finish.
            playViaMediaPlayer(fileName, vol)
        } else {
            val capMs = if (!allowLongSounds && duration != null && duration > maxDurationMs) maxDurationMs else null
            playViaSoundPool(fileName, vol, capMs)
        }
    }

    private fun playViaSoundPool(fileName: String, vol: Float, capMs: Int?) {
        val id = soundIds[fileName] ?: loadSound(fileName) ?: return
        if (id in loadedIds) {
            val streamId = soundPool.play(id, vol, vol, 1, 0, 1f)
            activeStreamIds.add(streamId)
            if (capMs != null) {
                handler.postDelayed({
                    runCatching { soundPool.stop(streamId) }
                    activeStreamIds.remove(streamId)
                }, capMs.toLong())
            }
        } else {
            // Still decoding — play it as soon as setOnLoadCompleteListener fires instead of
            // dropping it. (The duration cap isn't applied on this exact first play; it applies
            // normally from the next play onward once the sound is cached.)
            pendingVolume[id] = vol
        }
    }

    private fun playViaMediaPlayer(fileName: String, vol: Float) {
        longSoundPlayer?.runCatching { stop(); release() }
        longSoundPlayer = null
        runCatching {
            val mp = MediaPlayer()
            val ownFile = SoundLibrary.resolveOwnSoundFile(fileName)
            if (ownFile != null) {
                mp.setDataSource(ownFile.absolutePath)
            } else {
                context.assets.openFd("sounds/$fileName").use { afd ->
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
            }
            mp.setVolume(vol, vol)
            mp.setOnCompletionListener { player ->
                player.release()
                if (longSoundPlayer === player) longSoundPlayer = null
            }
            mp.prepare() // synchronous: fine here since long sounds aren't latency-critical
            mp.start()
            longSoundPlayer = mp
        }
    }

    /** Panic-stops every sound currently playing (SoundPool streams and the long-sound player). */
    fun stopAll() = stopAllActive()

    private fun stopAllActive() {
        activeStreamIds.forEach { runCatching { soundPool.stop(it) } }
        activeStreamIds.clear()
        longSoundPlayer?.runCatching { stop(); release() }
        longSoundPlayer = null
    }

    private fun durationOf(fileName: String): Long? {
        durationCache[fileName]?.let { return it }
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                val ownFile = SoundLibrary.resolveOwnSoundFile(fileName)
                if (ownFile != null) {
                    retriever.setDataSource(ownFile.absolutePath)
                } else {
                    context.assets.openFd("sounds/$fileName").use { afd ->
                        retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    }
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                retriever.release()
            }
        }.getOrNull()?.also { durationCache[fileName] = it }
    }

    private fun loadSound(fileName: String): Int? = runCatching {
        val id = SoundLibrary.resolveOwnSoundFile(fileName)?.let { file ->
            soundPool.load(file.absolutePath, 1)
        } ?: context.assets.openFd("sounds/$fileName").use { afd -> soundPool.load(afd, 1) }
        soundIds[fileName] = id
        id
    }.getOrNull()

    /**
     * Toy speaker effect + optional extra loudness, both user-tunable from Settings:
     * - [fxEnabled]: cuts bass, boosts mids, rolls off highs — the tinny, boxy sound of a cheap
     *   plastic speaker. [bassCut]/[midBoost]/[trebleCut] (0f..1f) scale each band's intensity.
     * - [drive] (0f..1f, only applied when Fx is on) and [gainBoost] (0f..1f, always applied)
     *   both add loudness via a single LoudnessEnhancer — which is a dynamics-processing effect,
     *   not a plain volume multiplier, so it limits peaks as it boosts (an "invisible" limiter,
     *   no separate normalizer needed). Combined gain is hard-capped at [SAFETY_CEILING_MB]
     *   regardless of slider settings, so it can't be pushed to speaker-damaging levels. Applied
     *   to the global output mix (session 0), so it covers both SoundPool and MediaPlayer audio.
     */
    fun applyEffects(fxEnabled: Boolean, bassCut: Float, midBoost: Float, trebleCut: Float, drive: Float, gainBoost: Float) {
        if (fxEnabled) {
            applyEqualizer(bassCut, midBoost, trebleCut)
        } else {
            equalizer?.enabled = false
        }
        applyLoudnessEnhancer(if (fxEnabled) drive else 0f, gainBoost)
    }

    private fun applyEqualizer(bassCut: Float, midBoost: Float, trebleCut: Float) {
        val eq = equalizer ?: runCatching { Equalizer(0, 0) }.getOrNull()?.also { equalizer = it } ?: return
        runCatching {
            val range = eq.bandLevelRange
            val minLevel = range[0]
            val maxLevel = range[1]
            val bands = eq.numberOfBands
            for (band in 0 until bands) {
                val centerHz = eq.getCenterFreq(band.toShort()) / 1000
                val level = when {
                    centerHz < 400 -> (minLevel * bassCut.coerceIn(0f, 1f)).toInt()
                    centerHz < 6000 -> (maxLevel * 0.9f * midBoost.coerceIn(0f, 1f)).toInt()
                    else -> (minLevel * 0.4f * trebleCut.coerceIn(0f, 1f)).toInt()
                }
                eq.setBandLevel(band.toShort(), level.toShort())
            }
            eq.enabled = true
        }
    }

    private fun applyLoudnessEnhancer(drive: Float, gainBoost: Float) {
        val enhancer = loudnessEnhancer
            ?: runCatching { LoudnessEnhancer(0) }.getOrNull()?.also { loudnessEnhancer = it }
            ?: return
        runCatching {
            val driveMb = drive.coerceIn(0f, 1f) * MAX_DRIVE_MB
            val gainMb = gainBoost.coerceIn(0f, 1f) * MAX_GAIN_BOOST_MB
            val totalMb = (driveMb + gainMb).coerceAtMost(SAFETY_CEILING_MB)
            if (totalMb <= 0f) {
                enhancer.enabled = false
            } else {
                enhancer.setTargetGain(totalMb.toInt())
                enhancer.enabled = true
            }
        }
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        equalizer?.release()
        loudnessEnhancer?.release()
        longSoundPlayer?.runCatching { stop(); release() }
        soundPool.release()
    }

    private companion object {
        const val LONG_SOUND_THRESHOLD_MS = 6000L
        const val MAX_DRIVE_MB = 900f // +9 dB at fxDrive = 1f
        const val MAX_GAIN_BOOST_MB = 900f // +9 dB at gainBoost = 1f
        const val SAFETY_CEILING_MB = 1500f // hard ceiling regardless of combined sliders: +15 dB
    }
}
