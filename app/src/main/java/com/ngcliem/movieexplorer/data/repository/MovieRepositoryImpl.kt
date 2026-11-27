package com.ngcliem.movieexplorer.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.ngcliem.movieexplorer.data.firebase.FirestorePaths
import com.ngcliem.movieexplorer.data.firebase.SessionStore
import com.ngcliem.movieexplorer.data.firebase.snapshots
import com.ngcliem.movieexplorer.data.local.CachedMovieDao
import com.ngcliem.movieexplorer.data.mapper.hasNonLatinLetters
import com.ngcliem.movieexplorer.data.mapper.toCached
import com.ngcliem.movieexplorer.data.mapper.toDomain
import com.ngcliem.movieexplorer.data.mapper.toFavoriteData
import com.ngcliem.movieexplorer.data.mapper.toFavoriteMovie
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val api: TmdbApi,
    private val db: FirebaseFirestore,
    private val cachedMovieDao: CachedMovieDao,
    private val session: SessionStore,
    private val languageStore: LanguageStore,
) : MovieRepository {

    private val pagingConfig = PagingConfig(
        pageSize = PAGE_SIZE,
        enablePlaceholders = false,
    )

    /** English titles fetched for movies whose title is in a non-Latin script, by movie id. */
    private val readableTitles = ConcurrentHashMap<Int, String>()

    /** Favorites belong to the signed-in user: users/{uid}/favorites/{movieId}. */
    private fun favorites() = session.userId.value?.let { FirestorePaths.favorites(db, it) }

    override fun getPopularMovies(): Flow<PagingData<Movie>> = getMovies(MovieCategory.POPULAR)

    override fun searchMovies(query: String): Flow<PagingData<Movie>> =
        Pager(pagingConfig) {
            MoviePagingSource(loadPage = { page -> api.searchMovies(query, page) }, transform = ::withReadableTitles)
        }.flow

    override fun getMovies(category: MovieCategory): Flow<PagingData<Movie>> =
        Pager(pagingConfig) {
            MoviePagingSource(loadPage = { page -> loadCategoryPage(category, page) }, transform = ::withReadableTitles)
        }.flow

    override suspend fun getMovieSection(category: MovieCategory): Result<List<Movie>> =
        try {
            val movies = withReadableTitles(
                loadCategoryPage(category, 1).results
                    .map { it.toDomain() }
                    .distinctBy { it.id },
            )
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

    /**
     * Replaces titles written in a non-Latin script (shown by TMDB when there is no
     * Vietnamese title) with the English title, which every user can read.
     */
    private suspend fun withReadableTitles(movies: List<Movie>): List<Movie> {
        // In English the list already holds the English title when one exists.
        if (languageStore.language.value == AppLanguage.ENGLISH) return movies
        if (movies.none { it.title.hasNonLatinLetters() }) return movies
        return coroutineScope {
            movies.map { movie ->
                async {
                    if (!movie.title.hasNonLatinLetters()) return@async movie
                    val title = readableTitles[movie.id] ?: runCatchingNonCancel {
                        api.getMovieDetail(movie.id, AppLanguage.ENGLISH.tmdbCode).toDomain().title
                    }.getOrNull()?.let { english ->
                        readableTitle(movie.title, english).also { readableTitles[movie.id] = it }
                    }
                    if (title != null) movie.copy(title = title) else movie
                }
            }.awaitAll()
        }
    }

    /** Prefers [english] when [title] cannot be read by Latin-alphabet users. */
    private fun readableTitle(title: String, english: String): String =
        if (title.hasNonLatinLetters() && english.isNotBlank() && !english.hasNonLatinLetters()) english else title

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
            // Many movies have no Vietnamese overview yet, and some only have a title in their
            // original script (Telugu, Japanese…): use the English text instead.
            val needsEnglish = detail.overview.isBlank() || detail.title.hasNonLatinLetters()
            if (needsEnglish && languageStore.language.value != AppLanguage.ENGLISH) {
                val english = runCatchingNonCancel {
                    api.getMovieDetail(movieId, AppLanguage.ENGLISH.tmdbCode).toDomain()
                }.getOrNull()
                if (english != null) {
                    detail = detail.copy(
                        title = readableTitle(detail.title, english.title),
                        overview = detail.overview.ifBlank { english.overview },
                        tagline = detail.tagline ?: english.tagline,
                    )
                }
            }
            Result.success(detail)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Offline fallback: a saved favorite, or a movie from a cached home section.
            val saved = savedFavorite(movieId)?.toDetail()
                ?: cachedMovieDao.findMovie(movieId)?.toDomain()?.toDetail()
            if (saved != null) Result.success(saved) else Result.failure(e)
        }

    /** Reads a favorite from Firestore's offline cache (used when TMDB is unreachable). */
    private suspend fun savedFavorite(movieId: Int): Movie? =
        runCatchingNonCancel {
            favorites()?.document(movieId.toString())?.get(Source.CACHE)?.await()?.data?.toFavoriteMovie()
        }.getOrNull()

    override fun observeFavorites(): Flow<List<Movie>> =
        session.userId.flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                FirestorePaths.favorites(db, uid)
                    .orderBy("addedAt", Query.Direction.DESCENDING)
                    .snapshots()
                    .map { snapshot -> snapshot.documents.mapNotNull { it.data?.toFavoriteMovie() } }
                    .catch { emit(emptyList()) }
            }
        }

    override fun observeIsFavorite(movieId: Int): Flow<Boolean> =
        session.userId.flatMapLatest { uid ->
            if (uid == null) {
                flowOf(false)
            } else {
                FirestorePaths.favorites(db, uid).document(movieId.toString())
                    .snapshots()
                    .map { it.exists() }
                    .catch { emit(false) }
            }
        }

    // Writes are not awaited: Firestore applies them locally at once (also offline)
    // and sends them to the server when it can.
    override suspend fun addFavorite(movie: Movie) {
        favorites()?.document(movie.id.toString())?.set(movie.toFavoriteData())
    }

    override suspend fun removeFavorite(movieId: Int) {
        favorites()?.document(movieId.toString())?.delete()
    }

    override suspend fun clearFavorites() {
        val collection = favorites() ?: return
        runCatchingNonCancel {
            val docs = collection.get().await().documents // server, or the cache when offline
            db.batch().apply { docs.forEach { delete(it.reference) } }.commit()
        }
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
    }
}
