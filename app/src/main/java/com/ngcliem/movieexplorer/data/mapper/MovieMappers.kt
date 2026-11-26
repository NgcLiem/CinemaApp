package com.ngcliem.movieexplorer.data.mapper

import com.ngcliem.movieexplorer.data.local.CachedMovieEntity
import com.ngcliem.movieexplorer.data.local.FavoriteMovieEntity
import com.ngcliem.movieexplorer.data.remote.dto.MovieDetailDto
import com.ngcliem.movieexplorer.data.remote.dto.MovieDto
import com.ngcliem.movieexplorer.data.remote.dto.ReviewDto
import com.ngcliem.movieexplorer.domain.model.Movie
import com.ngcliem.movieexplorer.domain.model.MovieDetail
import com.ngcliem.movieexplorer.domain.model.Review

// Mappers convert between the layers, so a change in the API JSON
// only needs a change here, not in the UI.

private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
private const val POSTER_SIZE = "w500"
private const val BACKDROP_SIZE = "w780"

/** TMDB returns only a path like "/abc.jpg"; this builds the full image URL. */
internal fun String?.toImageUrl(size: String): String? =
    this?.takeIf { it.isNotBlank() }?.let { "$IMAGE_BASE_URL$size$it" }

fun MovieDto.toDomain(): Movie = Movie(
    id = id,
    title = title,
    overview = overview,
    posterUrl = posterPath.toImageUrl(POSTER_SIZE),
    backdropUrl = backdropPath.toImageUrl(BACKDROP_SIZE),
    releaseDate = releaseDate.orEmpty(),
    voteAverage = voteAverage,
)

fun MovieDetailDto.toDomain(): MovieDetail = MovieDetail(
    id = id,
    title = title,
    tagline = tagline?.takeIf { it.isNotBlank() },
    overview = overview,
    posterUrl = posterPath.toImageUrl(POSTER_SIZE),
    backdropUrl = backdropPath.toImageUrl(BACKDROP_SIZE),
    releaseDate = releaseDate.orEmpty(),
    runtimeMinutes = runtime?.takeIf { it > 0 },
    voteAverage = voteAverage,
    voteCount = voteCount,
    genres = genres.map { it.name },
)

fun FavoriteMovieEntity.toDomain(): Movie = Movie(
    id = id,
    title = title,
    overview = overview,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
)

/** Used when the device is offline: shows the data saved with the favorite. */
fun FavoriteMovieEntity.toDetail(): MovieDetail = MovieDetail(
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

fun Movie.toEntity(
    userId: Long = 0,
    addedAt: Long = System.currentTimeMillis(),
): FavoriteMovieEntity =
    FavoriteMovieEntity(
        userId = userId,
        id = id,
        title = title,
        overview = overview,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        addedAt = addedAt,
    )

fun ReviewDto.toDomain(): Review = Review(
    id = id,
    author = authorDetails?.name?.takeIf { it.isNotBlank() } ?: author,
    rating = authorDetails?.rating,
    content = content.trim(),
    createdAt = createdAt.orEmpty().take(10),
    // TMDB sometimes stores a full Gravatar URL here, prefixed with "/".
    avatarUrl = authorDetails?.avatarPath?.let { path ->
        if (path.startsWith("/http")) path.drop(1) else path.toImageUrl("w185")
    },
)

fun Movie.toCached(category: String, position: Int) = CachedMovieEntity(
    category = category,
    position = position,
    id = id,
    title = title,
    overview = overview,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
)

fun CachedMovieEntity.toDomain() = Movie(
    id = id,
    title = title,
    overview = overview,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
)
