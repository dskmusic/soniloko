package com.dsk.soniloko.data.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.File
import java.util.concurrent.TimeUnit

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

    /** Best audio-only stream URL for direct streaming preview (URL expires after a few hours). */
    suspend fun resolvePreviewUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        ensureInit()
        runCatching {
            val info = StreamInfo.getInfo(yt, "https://www.youtube.com/watch?v=$videoId")
            info.audioStreams?.filter { !it.content.isNullOrBlank() }?.maxByOrNull { it.averageBitrate }?.content
                ?: info.videoStreams?.firstOrNull { !it.content.isNullOrBlank() }?.content
        }.getOrNull()
    }

    /**
     * Downloads the best available audio to [destFile]. Prefers m4a/AAC so the result is
     * compatible with the app's existing MediaExtractor/MediaMuxer trim tool.
     */
    suspend fun downloadAudio(videoId: String, destFile: File): Boolean = withContext(Dispatchers.IO) {
        ensureInit()
        runCatching {
            val info = StreamInfo.getInfo(yt, "https://www.youtube.com/watch?v=$videoId")
            val audios = info.audioStreams?.filter { !it.content.isNullOrBlank() }
            val chosen = audios?.filter { it.format?.suffix == "m4a" }?.maxByOrNull { it.averageBitrate }
                ?: audios?.maxByOrNull { it.averageBitrate }
                ?: info.videoStreams?.firstOrNull { !it.content.isNullOrBlank() }
                ?: return@withContext false
            val streamUrl = chosen.content ?: return@withContext false

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(streamUrl).header("User-Agent", USER_AGENT).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false
                destFile.outputStream().use { out -> body.byteStream().copyTo(out) }
            }
            true
        }.getOrDefault(false)
    }
}
