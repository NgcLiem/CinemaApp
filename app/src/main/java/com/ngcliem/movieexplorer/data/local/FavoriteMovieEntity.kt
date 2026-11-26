package com.ngcliem.movieexplorer.data.local

import androidx.room.Entity

/** A favorite movie of one user, saved on the device so it is available offline. */
@Entity(tableName = "favorite_movies", primaryKeys = ["userId", "id"])
data class FavoriteMovieEntity(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val addedAt: Long,
    val userId: Long = 0,
)
