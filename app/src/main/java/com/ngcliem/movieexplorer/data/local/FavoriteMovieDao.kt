package com.ngcliem.movieexplorer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteMovieDao {

    /** Emits again automatically every time the table changes. */
    @Query("SELECT * FROM favorite_movies WHERE userId = :userId ORDER BY addedAt DESC")
    fun observeAll(userId: Long): Flow<List<FavoriteMovieEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_movies WHERE userId = :userId AND id = :movieId)")
    fun observeIsFavorite(userId: Long, movieId: Int): Flow<Boolean>

    @Query("SELECT * FROM favorite_movies WHERE userId = :userId AND id = :movieId")
    suspend fun getById(userId: Long, movieId: Int): FavoriteMovieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movie: FavoriteMovieEntity)

    @Query("DELETE FROM favorite_movies WHERE userId = :userId AND id = :movieId")
    suspend fun deleteById(userId: Long, movieId: Int)

    @Query("DELETE FROM favorite_movies WHERE userId = :userId")
    suspend fun deleteAll(userId: Long)
}
