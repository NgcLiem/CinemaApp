package com.ngcliem.movieexplorer.domain.usecase

import androidx.paging.PagingData
import com.ngcliem.movieexplorer.domain.model.Movie
import com.ngcliem.movieexplorer.domain.model.MovieCategory
import com.ngcliem.movieexplorer.domain.model.MovieDetail
import com.ngcliem.movieexplorer.domain.model.Review
import com.ngcliem.movieexplorer.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Each use case represents one action the user can take.
// ViewModels depend on use cases instead of the whole repository.

class GetPopularMoviesUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    operator fun invoke(): Flow<PagingData<Movie>> = repository.getPopularMovies()
}

class SearchMoviesUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    operator fun invoke(query: String): Flow<PagingData<Movie>> =
        repository.searchMovies(query.trim())
}

class GetMovieDetailUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(movieId: Int): Result<MovieDetail> =
        repository.getMovieDetail(movieId)
}

class ObserveFavoritesUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    operator fun invoke(): Flow<List<Movie>> = repository.observeFavorites()
}

class ObserveIsFavoriteUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    operator fun invoke(movieId: Int): Flow<Boolean> = repository.observeIsFavorite(movieId)
}

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(movie: Movie, isCurrentlyFavorite: Boolean) {
        if (isCurrentlyFavorite) {
            repository.removeFavorite(movie.id)
        } else {
            repository.addFavorite(movie)
        }
    }
}

class GetMoviesByCategoryUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    operator fun invoke(category: MovieCategory): Flow<PagingData<Movie>> =
        repository.getMovies(category)
}

class GetMovieSectionUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(category: MovieCategory): Result<List<Movie>> =
        repository.getMovieSection(category)
}

class GetMovieReviewsUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(movieId: Int): Result<List<Review>> =
        repository.getMovieReviews(movieId)
}

class ClearFavoritesUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke() = repository.clearFavorites()
}
