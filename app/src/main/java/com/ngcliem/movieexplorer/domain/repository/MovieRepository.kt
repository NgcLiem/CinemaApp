package com.ngcliem.movieexplorer.domain.repository

import androidx.paging.PagingData
import com.ngcliem.movieexplorer.domain.model.Movie
import com.ngcliem.movieexplorer.domain.model.MovieCategory
import com.ngcliem.movieexplorer.domain.model.MovieDetail
import com.ngcliem.movieexplorer.domain.model.Review
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for movie data. The UI layer only knows this interface,
 * so the data source (network, database, fake for tests) can be swapped freely.
 */
interface MovieRepository {

    fun getPopularMovies(): Flow<PagingData<Movie>>

    fun searchMovies(query: String): Flow<PagingData<Movie>>

    /** Infinite list for the "see all" screen of a home section. */
    fun getMovies(category: MovieCategory): Flow<PagingData<Movie>>

    /**
     * First page only, for the horizontal rows and banners on the home screen.
     * Falls back to the last cached result when the network fails.
     */
    suspend fun getMovieSection(category: MovieCategory): Result<List<Movie>>

    suspend fun getMovieReviews(movieId: Int): Result<List<Review>>

    /** Loads from the network; falls back to the saved favorite when offline. */
    suspend fun getMovieDetail(movieId: Int): Result<MovieDetail>

    fun observeFavorites(): Flow<List<Movie>>

    fun observeIsFavorite(movieId: Int): Flow<Boolean>

    suspend fun addFavorite(movie: Movie)

    suspend fun removeFavorite(movieId: Int)

    suspend fun clearFavorites()
}
