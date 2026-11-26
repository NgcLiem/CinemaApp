package com.ngcliem.movieexplorer.testutil

import androidx.paging.PagingData
import com.ngcliem.movieexplorer.domain.model.Movie
import com.ngcliem.movieexplorer.domain.model.MovieCategory
import com.ngcliem.movieexplorer.domain.model.MovieDetail
import com.ngcliem.movieexplorer.domain.model.Review
import com.ngcliem.movieexplorer.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException

/** In-memory repository for tests: no network, no database, fully controllable. */
class FakeMovieRepository : MovieRepository {

    /** Every query passed to [searchMovies], in order. */
    val searchQueries = mutableListOf<String>()

    /** What [getMovieDetail] returns. Change it inside a test to simulate errors. */
    var detailResult: Result<MovieDetail> = Result.failure(IOException("offline"))

    private val favorites = MutableStateFlow<List<Movie>>(emptyList())

    override fun getPopularMovies(): Flow<PagingData<Movie>> =
        flowOf(PagingData.from(listOf(testMovie(1), testMovie(2))))

    override fun searchMovies(query: String): Flow<PagingData<Movie>> {
        searchQueries += query
        return flowOf(PagingData.from(listOf(testMovie(3))))
    }

    override fun getMovies(category: MovieCategory): Flow<PagingData<Movie>> =
        flowOf(PagingData.from(listOf(testMovie(4))))

    override suspend fun getMovieSection(category: MovieCategory): Result<List<Movie>> =
        Result.success(listOf(testMovie(5), testMovie(6)))

    override suspend fun getMovieReviews(movieId: Int): Result<List<Review>> =
        Result.success(emptyList())

    override suspend fun getMovieDetail(movieId: Int): Result<MovieDetail> = detailResult

    override fun observeFavorites(): Flow<List<Movie>> = favorites

    override fun observeIsFavorite(movieId: Int): Flow<Boolean> =
        favorites.map { list -> list.any { it.id == movieId } }

    override suspend fun addFavorite(movie: Movie) {
        favorites.update { list -> list.filterNot { it.id == movie.id } + movie }
    }

    override suspend fun removeFavorite(movieId: Int) {
        favorites.update { list -> list.filterNot { it.id == movieId } }
    }

    override suspend fun clearFavorites() {
        favorites.value = emptyList()
    }
}

fun testMovie(id: Int) = Movie(
    id = id,
    title = "Movie $id",
    overview = "Overview $id",
    posterUrl = null,
    backdropUrl = null,
    releaseDate = "2024-01-01",
    voteAverage = 7.5,
)

fun testDetail(id: Int) = MovieDetail(
    id = id,
    title = "Movie $id",
    tagline = "Tagline",
    overview = "Overview $id",
    posterUrl = null,
    backdropUrl = null,
    releaseDate = "2024-01-01",
    runtimeMinutes = 120,
    voteAverage = 7.5,
    voteCount = 1000,
    genres = listOf("Action", "Drama"),
)
