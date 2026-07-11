package com.dsk.soniloko.data.image

import com.dsk.soniloko.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

enum class ImageSearchType(val apiValue: String) {
    ILLUSTRATION("illustration"),
    PHOTO("photo"),
    VECTOR("vector"),
    ALL("all")
}

data class ImageSearchResult(
    val id: String,
    val thumbnailUrl: String,
    val fullUrl: String
)

/**
 * Client-side image search via the Pixabay API (pixabay.com) — free tier, requires an API key
 * (BuildConfig.PIXABAY_API_KEY, see app/build.gradle.kts), Creative-Commons-style content only.
 * Switched from Openverse: that API sits behind Cloudflare, which silently blocked OkHttp's
 * default User-Agent and returned no results.
 */
object ImageSearchRepository {
    private const val SEARCH_URL = "https://pixabay.com/api/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun search(
        query: String,
        imageType: ImageSearchType = ImageSearchType.ILLUSTRATION,
        pageSize: Int = 30,
        page: Int = 1
    ): List<ImageSearchResult> = withContext(Dispatchers.IO) {
        if (BuildConfig.PIXABAY_API_KEY.isBlank()) return@withContext emptyList()
        runCatching {
            val url = "$SEARCH_URL?key=${BuildConfig.PIXABAY_API_KEY}" +
                "&q=${URLEncoder.encode(query, "UTF-8")}" +
                "&image_type=${imageType.apiValue}" +
                "&safesearch=true" +
                "&per_page=$pageSize" +
                "&page=$page"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val hits = JSONObject(body).optJSONArray("hits") ?: return@withContext emptyList()
                (0 until hits.length()).mapNotNull { i ->
                    val o = hits.getJSONObject(i)
                    val thumb = o.optString("previewURL").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    // Prefer largeImageURL: webformatURL is sometimes a tighter/pre-cropped
                    // rendition, which left users unable to pan back to parts of the image that
                    // were never actually downloaded.
                    val full = o.optString("largeImageURL").takeIf { it.isNotBlank() }
                        ?: o.optString("webformatURL").takeIf { it.isNotBlank() }
                        ?: thumb
                    ImageSearchResult(id = o.optInt("id", 0).toString(), thumbnailUrl = thumb, fullUrl = full)
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun downloadBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.bytes()
            }
        }.getOrNull()
    }
}
