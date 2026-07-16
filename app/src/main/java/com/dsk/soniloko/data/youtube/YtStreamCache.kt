package com.dsk.soniloko.data.youtube

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Disk cache shared between YouTube preview (ExoPlayer) and download: previewing a video caches
 * its audio bytes, so downloading the same video right after reuses them instead of re-fetching
 * from the network. Bounded so repeated previews across many videos can't grow it unbounded.
 */
object YtStreamCache {
    private const val MAX_BYTES = 100L * 1024 * 1024

    @Volatile
    private var instance: SimpleCache? = null

    @OptIn(UnstableApi::class)
    fun get(context: Context): SimpleCache = instance ?: synchronized(this) {
        instance ?: SimpleCache(
            File(context.cacheDir, "yt_stream_cache"),
            LeastRecentlyUsedCacheEvictor(MAX_BYTES),
            StandaloneDatabaseProvider(context)
        ).also { instance = it }
    }
}
