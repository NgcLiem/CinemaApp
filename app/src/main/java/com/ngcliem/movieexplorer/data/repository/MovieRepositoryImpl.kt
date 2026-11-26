package com.ngcliem.movieexplorer.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.ngcliem.movieexplorer.data.local.CachedMovieDao
import com.ngcliem.movieexplorer.data.local.FavoriteMovieDao
import com.ngcliem.movieexplorer.data.mapper.toCached
import com.ngcliem.movieexplorer.data.mapper.toDetail
import com.ngcliem.movieexplorer.data.mapper.toDomain
import com.ngcliem.movieexplorer.data.mapper.toEntity
import com.ngcliem.movieexplorer.data.paging.MoviePagingSource
import com.ngcliem.movieexplorer.data.remote.TmdbApi
import com.ngcliem.movieexplorer.data.remote.dto.MovieListResponseDto
import com.ngcliem.movieexplorer.data.settings.AppLanguage
import com.ngcliem.movieexplorer.data.settings.LanguageStore
import com.ngcliem.movieexplorer.domain.model.Movie
import com.ngcliem.movieexplorer.domain.model.MovieCategory
import com.ngcliem.movieexplorer.domain.model.MovieDetail
import com.ngcliem.movieexplorer.domain.model.Review
import com.ngcliem.movieexplorer.domain.repository.MovieRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val api: TmdbApi,
    private val favoriteDao: FavoriteMovieDao,
    private val cachedMovieDao: CachedMovieDao,
    private val session: SessionStore,
    private val languageStore: LanguageStore,
) : MovieRepository {

    private val pagingConfig = PagingConfig(
        pageSize = PAGE_SIZE,
        enablePlaceholders = false,
    )

    /** Favorites belong to the signed-in user. */
    private val userId: Long get() = session.userId.value ?: NO_USER

    override fun getPopularMovies(): Flow<PagingData<Movie>> = getMovies(MovieCategory.POPULAR)

    override fun searchMovies(query: String): Flow<PagingData<Movie>> =
        Pager(pagingConfig) {
            MoviePagingSource { page -> api.searchMovies(query, page) }
        }.flow

    override fun getMovies(category: MovieCategory): Flow<PagingData<Movie>> =
        Pager(pagingConfig) {
            MoviePagingSource { page -> loadCategoryPage(category, page) }
        }.flow

    override suspend fun getMovieSection(category: MovieCategory): Result<List<Movie>> =
        try {
            val movies = loadCategoryPage(category, 1).results
                .map { it.toDomain() }
                .distinctBy { it.id }
            cachedMovieDao.replaceSection(
                category.name,
                movies.mapIndexed { index, movie -> movie.toCached(category.name, index) },
            )
            Result.success(movies)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Offline: show the last list we managed to load, if any.
            val cached = cachedMovieDao.getSection(category.name).map { it.toDomain() }
            if (cached.isNotEmpty()) Result.success(cached) else Result.failure(e)
        }

    override suspend fun getMovieReviews(movieId: Int): Result<List<Review>> =
        runCatchingNonCancel {
            api.getMovieReviews(movieId).results.map { it.toDomain() }
        }

    private suspend fun loadCategoryPage(category: MovieCategory, page: Int): MovieListResponseDto =
        when (category) {
            MovieCategory.NOW_PLAYING -> api.getNowPlayingMovies(page)
            MovieCategory.UPCOMING -> api.getUpcomingMovies(page)
            MovieCategory.POPULAR -> api.getPopularMovies(page)
        }

    private inline fun <T> runCatchingNonCancel(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun getMovieDetail(movieId: Int): Result<MovieDetail> =
        try {
            var detail = api.getMovieDetail(movieId).toDomain()
            // Many movies have no Vietnamese overview yet: use the English one instead.
            if (detail.overview.isBlank() && languageStore.language.value != AppLanguage.ENGLISH) {
                val english = runCatchingNonCancel {
                    api.getMovieDetail(movieId, AppLanguage.ENGLISH.tmdbCode).toDomain()
                }.getOrNull()
                if (english != null) {
                    detail = detail.copy(
                        overview = english.overview,
                        tagline = detail.tagline ?: english.tagline,
                    )
                }
            }
            Result.success(detail)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Offline fallback: a saved favorite, or a movie from a cached home section.
            val saved = favoriteDao.getById(userId, movieId)?.toDetail()
                ?: cachedMovieDao.findMovie(movieId)?.toDomain()?.toDetail()
            if (saved != null) Result.success(saved) else Result.failure(e)
        }

    override fun observeFavorites(): Flow<List<Movie>> =
        session.userId.flatMapLatest { id ->
            if (id == null) {
                flowOf(emptyList())
            } else {
                favoriteDao.observeAll(id).map { list -> list.map { it.toDomain() } }
            }
        }

    override fun observeIsFavorite(movieId: Int): Flow<Boolean> =
        session.userId.flatMapLatest { id ->
            if (id == null) flowOf(false) else favoriteDao.observeIsFavorite(id, movieId)
        }

    override suspend fun addFavorite(movie: Movie) {
        favoriteDao.insert(movie.toEntity(userId = userId))
    }

    override suspend fun removeFavorite(movieId: Int) {
        favoriteDao.deleteById(userId, movieId)
    }

    override suspend fun clearFavorites() {
        favoriteDao.deleteAll(userId)
    }

    private fun Movie.toDetail() = MovieDetail(
        id = id,
        title = title,
        tagline = null,
        overview = overview,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        releaseDate = releaseDate,
        runtimeMinutes = null,
        voteAverage = voteAverage,
        voteCount = 0,
        genres = emptyList(),
    )

    private companion object {
        const val PAGE_SIZE = 20 // TMDB always returns 20 movies per page
        const val NO_USER = 0L
    }
}
