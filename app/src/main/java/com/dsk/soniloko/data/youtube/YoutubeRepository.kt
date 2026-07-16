package com.dsk.soniloko.data.youtube

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.File

data class YtSearchResult(
    val videoId: String,
    val title: String,
    val uploader: String,
    val durationSec: Long
)

/**
 * Client-side YouTube search/preview/download via NewPipeExtractor — no API key, no server,
 * same technique as DSK LoFi. Kept as plain suspend functions (no WebView/JS bridge needed
 * here since SoniLoko's UI is Compose-native).
 */
object YoutubeRepository {
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    @Volatile
    private var initialized = false

    private fun ensureInit() {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    NewPipe.init(YtDownloader.getInstance())
                    initialized = true
                }
            }
        }
    }

    private val yt get() = ServiceList.YouTube

    // ponytail: single-entry cache, only the last-resolved video matters (preview -> download of
    // the same result). Upgrade to a keyed cache if multiple in-flight lookups need to overlap.
    @Volatile
    private var cachedExtractor: Pair<String, StreamExtractor>? = null

    // Deliberately NOT StreamInfo.getInfo(): that also calls getRelatedItems()/getFrames()/
    // getSubtitlesDefault() etc., each of which can trigger its own extra network request for
    // data preview/download never uses. Fetching the extractor directly and reading only
    // audioStreams/videoStreams/name/uploaderName/length skips all of that.
    private fun fetchExtractor(videoId: String): StreamExtractor {
        cachedExtractor?.let { (id, ex) -> if (id == videoId) return ex }
        val extractor = yt.getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
        extractor.fetchPage()
        cachedExtractor = videoId to extractor
        return extractor
    }

    /**
     * Fetches up to [target] results, walking multiple result pages internally (the UI then
     * paginates client-side through this already-fetched batch — simpler and safer than trying
     * to keep a NewPipeExtractor session object alive across separate suspend calls).
     */
    suspend fun search(query: String, target: Int = 90): List<YtSearchResult> = withContext(Dispatchers.IO) {
        ensureInit()
        runCatching {
            val handler = yt.searchQHFactory.fromQuery(query, listOf(YoutubeSearchQueryHandlerFactory.VIDEOS), "")
            val extractor = yt.getSearchExtractor(handler)
            extractor.fetchPage()

            val out = mutableListOf<YtSearchResult>()
            val seen = HashSet<String>()
            var page: ListExtractor.InfoItemsPage<InfoItem>? = extractor.initialPage
            var guard = 0
            while (out.size < target && guard < 10) {
                val cur = page ?: break
                for (item in cur.items) {
                    if (out.size >= target) break
                    val s = item as? StreamInfoItem ?: continue
                    val url = s.url ?: continue
                    val id = runCatching { yt.streamLHFactory.getId(url) }.getOrNull() ?: continue
                    if (!seen.add(id)) continue
                    out.add(YtSearchResult(id, s.name ?: id, s.uploaderName ?: "", s.duration))
                }
                if (out.size >= target) break
                val np = cur.nextPage ?: break
                page = runCatching { extractor.getPage(np) }.getOrNull()
                guard++
            }
            out
        }.getOrDefault(emptyList())
    }

    /** Resolves a pasted YouTube link (full or shared/shortened form) straight to a result,
     * skipping search entirely — for when the user already knows the exact video. */
    suspend fun resolveByUrl(url: String): YtSearchResult? = withContext(Dispatchers.IO) {
        ensureInit()
        runCatching {
            val id = yt.streamLHFactory.getId(url)
            val extractor = fetchExtractor(id)
            YtSearchResult(id, extractor.name ?: id, extractor.uploaderName ?: "", extractor.length)
        }.getOrNull()
    }

    /** Best audio-only stream URL for direct streaming preview (URL expires after a few hours). */
    suspend fun resolvePreviewUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        ensureInit()
        runCatching {
            val extractor = fetchExtractor(videoId)
            extractor.audioStreams?.filter { !it.content.isNullOrBlank() }?.maxByOrNull { it.averageBitrate }?.content
                ?: extractor.videoStreams?.firstOrNull { !it.content.isNullOrBlank() }?.content
        }.getOrNull()
    }

    /**
     * Downloads the best available audio to [destFile]. Prefers m4a/AAC so the result is
     * compatible with the app's existing MediaExtractor/MediaMuxer trim tool. Reads through the
     * same disk cache the preview player uses — if the user already previewed this video, this
     * reuses those bytes instead of re-fetching them from the network.
     */
    @OptIn(UnstableApi::class)
    suspend fun downloadAudio(context: Context, videoId: String, destFile: File): Boolean = withContext(Dispatchers.IO) {
        ensureInit()
        runCatching {
            val extractor = fetchExtractor(videoId)
            val audios = extractor.audioStreams?.filter { !it.content.isNullOrBlank() }
            // Only m4a/AAC audio-only, or a combined stream (AAC audio track in an mp4 container),
            // are used — anything else (e.g. WebM/Opus audio-only) would still get saved with a
            // ".m4a" name below and later fail (or silently produce an invalid file) in
            // AudioTrim's MediaMuxer(MPEG_4), which only accepts AAC.
            val chosen = audios?.filter { it.format?.suffix == "m4a" }?.maxByOrNull { it.averageBitrate }
                ?: extractor.videoStreams?.firstOrNull { !it.content.isNullOrBlank() }
                ?: return@withContext false
            val streamUrl = chosen.content ?: return@withContext false

            val dataSource = CacheDataSource.Factory()
                .setCache(YtStreamCache.get(context))
                .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory().setUserAgent(USER_AGENT))
                .createDataSource()
            dataSource.open(DataSpec(Uri.parse(streamUrl)))
            try {
                destFile.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = dataSource.read(buffer, 0, buffer.size)
                        if (read == C.RESULT_END_OF_INPUT) break
                        out.write(buffer, 0, read)
                    }
                }
            } finally {
                dataSource.close()
            }
            true
        }.getOrDefault(false)
    }
}
