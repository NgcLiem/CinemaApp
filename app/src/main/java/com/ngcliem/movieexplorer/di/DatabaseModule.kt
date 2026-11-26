package com.ngcliem.movieexplorer.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.ngcliem.movieexplorer.data.local.AppDatabase
import com.ngcliem.movieexplorer.data.local.BookingDao
import com.ngcliem.movieexplorer.data.local.CachedMovieDao
import com.ngcliem.movieexplorer.data.local.FavoriteMovieDao
import com.ngcliem.movieexplorer.data.local.UserDao
import com.ngcliem.movieexplorer.data.local.UserReviewDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideFavoriteMovieDao(database: AppDatabase): FavoriteMovieDao =
        database.favoriteMovieDao()

    @Provides
    fun provideCachedMovieDao(database: AppDatabase): CachedMovieDao = database.cachedMovieDao()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideBookingDao(database: AppDatabase): BookingDao = database.bookingDao()

    @Provides
    fun provideUserReviewDao(database: AppDatabase): UserReviewDao = database.userReviewDao()

    /** Only stores the id of the signed-in user. */
    @Provides
    @Singleton
    fun provideSessionPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("session", Context.MODE_PRIVATE)
}
