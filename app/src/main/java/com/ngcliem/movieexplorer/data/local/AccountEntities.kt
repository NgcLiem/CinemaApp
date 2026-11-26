package com.ngcliem.movieexplorer.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    /** Always stored lower-case. */
    val email: String,
    val phone: String,
    val passwordHash: String,
    val salt: String,
    val createdAt: Long,
)

@Entity(tableName = "bookings", indices = [Index(value = ["userId"])])
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val code: String,
    val movieId: Int,
    val movieTitle: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val cinema: String,
    val showDateEpochDay: Long,
    val showTime: String,
    /** [com.ngcliem.movieexplorer.domain.model.ScreenFormat] name. */
    val format: String,
    /** Comma separated, e.g. "F5,F6". */
    val seats: String,
    /** Comma separated, empty for normal tickets. */
    val specialTags: String,
    val totalPrice: Long,
    val points: Int,
    val bookedAt: Long,
)

@Entity(
    tableName = "user_reviews",
    indices = [Index(value = ["movieId", "userId"], unique = true)],
)
data class UserReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val movieId: Int,
    val userId: Long,
    val authorName: String,
    /** 1..5 stars. */
    val rating: Int,
    val content: String,
    val createdAt: Long,
)

@Dao
interface UserDao {

    /** Throws SQLiteConstraintException when the email is already registered. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query("UPDATE users SET passwordHash = :hash, salt = :salt WHERE id = :id")
    suspend fun updatePassword(id: Long, hash: String, salt: String)
}

@Dao
interface BookingDao {

    @Insert
    suspend fun insert(booking: BookingEntity): Long

    @Query("SELECT * FROM bookings WHERE userId = :userId ORDER BY bookedAt DESC")
    fun observeForUser(userId: Long): Flow<List<BookingEntity>>

    @Query("SELECT COALESCE(SUM(points), 0) FROM bookings WHERE userId = :userId")
    suspend fun totalPoints(userId: Long): Int

    @Query(
        "SELECT seats FROM bookings WHERE movieId = :movieId AND cinema = :cinema " +
            "AND showDateEpochDay = :epochDay AND showTime = :time",
    )
    suspend fun seatsForShowing(movieId: Int, cinema: String, epochDay: Long, time: String): List<String>
}

@Dao
interface UserReviewDao {

    @Query("SELECT * FROM user_reviews WHERE movieId = :movieId ORDER BY createdAt DESC")
    fun observeForMovie(movieId: Int): Flow<List<UserReviewEntity>>

    /** REPLACE on the (movieId, userId) unique index = one review per user per movie. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: UserReviewEntity)

    @Query("DELETE FROM user_reviews WHERE movieId = :movieId AND userId = :userId")
    suspend fun delete(movieId: Int, userId: Long)
}
