package com.ngcliem.movieexplorer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.ngcliem.movieexplorer.di.BaseClient
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

/** Entry point for Hilt: generates the dependency graph for the whole app. */
@HiltAndroidApp
class MovieExplorerApplication : Application(), ImageLoaderFactory {

    @Inject
    @BaseClient
    lateinit var okHttpClient: OkHttpClient

    /** Coil uses the same client, so images also get the DNS fallback. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { okHttpClient }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
}
