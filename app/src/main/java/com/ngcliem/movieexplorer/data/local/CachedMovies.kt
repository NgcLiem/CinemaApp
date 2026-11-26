package com.ngcliem.movieexplorer.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/** Last successful result of each home section, shown when the network fails. */
@Entity(tableName = "cached_movies", primaryKeys = ["category", "position"])
data class CachedMovieEntity(
    val category: String,
    val position: Int,
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String,
    val voteAverage: Double,
)

@Dao
interface CachedMovieDao {

    @Query("SELECT * FROM cached_movies WHERE category = :category ORDER BY position")
    suspend fun getSection(category: String): List<CachedMovieEntity>

    @Query("SELECT * FROM cached_movies WHERE id = :movieId LIMIT 1")
    suspend fun findMovie(movieId: Int): CachedMovieEntity?

    @Query("DELETE FROM cached_movies WHERE category = :category")
    suspend fun clearSection(category: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movies: List<CachedMovieEntity>)

    @Transaction
    suspend fun replaceSection(category: String, movies: List<CachedMovieEntity>) {
        clearSection(category)
        insertAll(movies)
    }
}
