package com.ngcliem.movieexplorer.di

import com.ngcliem.movieexplorer.data.repository.AuthRepositoryImpl
import com.ngcliem.movieexplorer.data.repository.BookingRepositoryImpl
import com.ngcliem.movieexplorer.data.repository.MovieRepositoryImpl
import com.ngcliem.movieexplorer.data.repository.ReviewRepositoryImpl
import com.ngcliem.movieexplorer.domain.repository.AuthRepository
import com.ngcliem.movieexplorer.domain.repository.BookingRepository
import com.ngcliem.movieexplorer.domain.repository.MovieRepository
import com.ngcliem.movieexplorer.domain.repository.ReviewRepository
import com.ngcliem.movieexplorer.domain.repository.SearchHistoryRepository
import com.ngcliem.movieexplorer.data.settings.SearchHistoryStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Tells Hilt: "whenever someone asks for MovieRepository, give them MovieRepositoryImpl". */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindBookingRepository(impl: BookingRepositoryImpl): BookingRepository

    @Binds
    abstract fun bindSearchHistory(impl: SearchHistoryStore): SearchHistoryRepository

    @Binds
    abstract fun bindReviewRepository(impl: ReviewRepositoryImpl): ReviewRepository
}
