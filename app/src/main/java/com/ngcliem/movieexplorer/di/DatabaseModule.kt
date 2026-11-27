package com.ngcliem.movieexplorer.di

import android.content.Context
import androidx.room.Room
import com.ngcliem.movieexplorer.data.local.AppDatabase
import com.ngcliem.movieexplorer.data.local.CachedMovieDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** The database only holds a cache, so older versions are simply recreated. */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCachedMovieDao(database: AppDatabase): CachedMovieDao = database.cachedMovieDao()
}
