package com.ngcliem.movieexplorer.di

import com.ngcliem.movieexplorer.BuildConfig
import com.ngcliem.movieexplorer.data.remote.ApiKeyInterceptor
import com.ngcliem.movieexplorer.data.remote.TmdbApi
import com.ngcliem.movieexplorer.data.remote.TmdbDns
import com.ngcliem.movieexplorer.data.settings.LanguageStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** Client without the TMDB API key, shared with Coil for loading images. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true // TMDB returns many fields we don't use
        coerceInputValues = true // null for a non-null field -> use the default value
    }

    @Provides
    @Singleton
    @BaseClient
    fun provideBaseOkHttpClient(): OkHttpClient {
        val bootstrap = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()
        return bootstrap.newBuilder()
            .dns(TmdbDns(bootstrap))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(@BaseClient base: OkHttpClient, languageStore: LanguageStore): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Only log in debug builds, so the API key never appears in release logs.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return base.newBuilder()
            .addInterceptor(ApiKeyInterceptor(BuildConfig.TMDB_API_KEY) { languageStore.language.value.tmdbCode })
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideTmdbApi(client: OkHttpClient, json: Json): TmdbApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.TMDB_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TmdbApi::class.java)
}
