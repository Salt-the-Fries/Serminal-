package com.saltyfries.serminal

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File

/**
 * Owns the shared on-disk cache used by the animated terminal backdrop.
 *
 * Only the video layer reads from this cache. Compose blur and tint remain
 * separate visual layers, so terminal text is never blurred.
 */
internal object JellyfishPlayerFactory {
    private const val VIDEO_URL =
        "https://raw.githubusercontent.com/Salt-the-Fries/Serminal-/main/264272_medium.mp4"
    private const val CACHE_LIMIT_BYTES = 256L * 1024L * 1024L

    @Volatile
    private var mediaCache: SimpleCache? = null

    private fun getCache(context: Context): SimpleCache {
        mediaCache?.let { return it }
        return synchronized(this) {
            mediaCache ?: SimpleCache(
                File(context.cacheDir, "jellyfish-video"),
                LeastRecentlyUsedCacheEvictor(CACHE_LIMIT_BYTES),
                StandaloneDatabaseProvider(context)
            ).also { mediaCache = it }
        }
    }

    fun create(context: Context): ExoPlayer {
        val appContext = context.applicationContext
        val upstreamFactory = DefaultDataSource.Factory(appContext)
        val cachedDataSourceFactory = CacheDataSource.Factory()
            .setCache(getCache(appContext))
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        return ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(appContext)
                    .setDataSourceFactory(cachedDataSourceFactory)
            )
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(VIDEO_URL))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                playWhenReady = true
                prepare()
            }
    }
}
