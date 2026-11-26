package com.ngcliem.movieexplorer.data.repository

import android.content.SharedPreferences
import android.database.sqlite.SQLiteConstraintException
import com.ngcliem.movieexplorer.data.auth.PasswordHasher
import com.ngcliem.movieexplorer.data.local.BookingDao
import com.ngcliem.movieexplorer.data.local.BookingEntity
import com.ngcliem.movieexplorer.data.local.UserDao
import com.ngcliem.movieexplorer.data.local.UserEntity
import com.ngcliem.movieexplorer.data.local.UserReviewDao
import com.ngcliem.movieexplorer.data.local.UserReviewEntity
import com.ngcliem.movieexplorer.domain.model.Booking
import com.ngcliem.movieexplorer.domain.model.BookingRequest
import com.ngcliem.movieexplorer.domain.model.MemberTier
import com.ngcliem.movieexplorer.domain.model.Review
import com.ngcliem.movieexplorer.domain.model.ScreenFormat
import com.ngcliem.movieexplorer.domain.model.SpecialTag
import com.ngcliem.movieexplorer.domain.model.SeatMap
import com.ngcliem.movieexplorer.domain.model.User
import com.ngcliem.movieexplorer.domain.model.discountedPrice
import com.ngcliem.movieexplorer.domain.model.pointsFor
import com.ngcliem.movieexplorer.domain.repository.AuthRepository
import com.ngcliem.movieexplorer.domain.repository.BookingRepository
import com.ngcliem.movieexplorer.domain.repository.ReviewRepository
import com.ngcliem.movieexplorer.domain.validation.AuthError
import com.ngcliem.movieexplorer.domain.validation.AuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Remembers which user is signed in, across app restarts. */
@Singleton
class SessionStore @Inject constructor(
    private val prefs: SharedPreferences,
) {
    private val _userId = MutableStateFlow(prefs.getLong(KEY_USER_ID, NO_USER).takeIf { it != NO_USER })
    val userId: StateFlow<Long?> = _userId.asStateFlow()

    fun signIn(id: Long) {
        prefs.edit().putLong(KEY_USER_ID, id).apply()
        _userId.value = id
    }

    fun signOut() {
        prefs.edit().remove(KEY_USER_ID).apply()
        _userId.value = null
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val NO_USER = -1L
    }
}

private fun UserEntity.toDomain() = User(id, fullName, email, phone, createdAt)

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val session: SessionStore,
) : AuthRepository {

    override val currentUser: Flow<User?> = session.userId.flatMapLatest { id ->
        if (id == null) flowOf(null) else userDao.observeById(id).map { it?.toDomain() }
    }

    override fun isLoggedIn(): Boolean = session.userId.value != null

    override suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
    ): Result<User> = withContext(Dispatchers.Default) {
        val normalizedEmail = email.trim().lowercase()
        if (userDao.findByEmail(normalizedEmail) != null) {
            return@withContext Result.failure(AuthException(AuthError.EMAIL_TAKEN))
        }
        val salt = PasswordHasher.newSalt()
        val entity = UserEntity(
            fullName = fullName.trim(),
            email = normalizedEmail,
            phone = phone.trim().replace(" ", ""),
            passwordHash = PasswordHasher.hash(password, salt),
            salt = salt,
            createdAt = System.currentTimeMillis(),
        )
        try {
            val id = userDao.insert(entity)
            session.signIn(id)
            Result.success(entity.copy(id = id).toDomain())
        } catch (e: SQLiteConstraintException) {
            Result.failure(AuthException(AuthError.EMAIL_TAKEN))
        }
    }

    override suspend fun login(email: String, password: String): Result<User> =
        withContext(Dispatchers.Default) {
            val user = userDao.findByEmail(email.trim().lowercase())
            if (user == null || !PasswordHasher.verify(password, user.salt, user.passwordHash)) {
                Result.failure(AuthException(AuthError.WRONG_CREDENTIALS))
            } else {
                session.signIn(user.id)
                Result.success(user.toDomain())
            }
        }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            val id = session.userId.value
                ?: return@withContext Result.failure(AuthException(AuthError.NOT_LOGGED_IN))
            val user = userDao.getById(id)
                ?: return@withContext Result.failure(AuthException(AuthError.NOT_LOGGED_IN))
            when {
                !PasswordHasher.verify(currentPassword, user.salt, user.passwordHash) ->
                    Result.failure(AuthException(AuthError.WRONG_CURRENT_PASSWORD))
                currentPassword == newPassword ->
                    Result.failure(AuthException(AuthError.SAME_PASSWORD))
                else -> {
                    val salt = PasswordHasher.newSalt()
                    userDao.updatePassword(id, PasswordHasher.hash(newPassword, salt), salt)
                    Result.success(Unit)
                }
            }
        }

    override suspend fun logout() {
        session.signOut()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class BookingRepositoryImpl @Inject constructor(
    private val bookingDao: BookingDao,
    private val session: SessionStore,
) : BookingRepository {

    override fun observeMyBookings(): Flow<List<Booking>> = session.userId.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyList())
        } else {
            bookingDao.observeForUser(id).map { list -> list.map { it.toDomain() } }
        }
    }

    override suspend fun takenSeats(
        movieId: Int,
        cinema: String,
        date: LocalDate,
        time: String,
    ): Set<String> {
        val booked = bookingDao.seatsForShowing(movieId, cinema, date.toEpochDay(), time)
            .flatMap { it.split(",") }
            .filter { it.isNotBlank() }
        return SeatMap.simulatedTakenSeats(movieId, cinema, date, time) + booked
    }

    override suspend fun book(request: BookingRequest): Result<Booking> {
        val userId = session.userId.value
            ?: return Result.failure(AuthException(AuthError.NOT_LOGGED_IN))
        if (request.seats.isEmpty()) return Result.failure(AuthException(AuthError.NO_SEATS))

        val taken = takenSeats(request.movieId, request.cinema, request.showDate, request.showTime)
        if (request.seats.any { it in taken }) {
            return Result.failure(AuthException(AuthError.SEATS_TAKEN))
        }

        // The discount uses the tier the user had *before* this booking.
        val tier = MemberTier.forPoints(bookingDao.totalPoints(userId))
        val paid = discountedPrice(request.totalPrice, tier)
        val entity = BookingEntity(
            userId = userId,
            code = generateCode(),
            movieId = request.movieId,
            movieTitle = request.movieTitle,
            posterUrl = request.posterUrl,
            backdropUrl = request.backdropUrl,
            cinema = request.cinema,
            showDateEpochDay = request.showDate.toEpochDay(),
            showTime = request.showTime,
            format = request.format.name,
            seats = request.seats.joinToString(","),
            specialTags = request.specialTags.joinToString(",") { it.name },
            totalPrice = paid,
            points = pointsFor(paid),
            bookedAt = System.currentTimeMillis(),
        )
        val id = bookingDao.insert(entity)
        return Result.success(entity.copy(id = id).toDomain())
    }

    private fun generateCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return "MX" + (1..8).map { alphabet.random() }.joinToString("")
    }
}

fun BookingEntity.toDomain() = Booking(
    id = id,
    code = code,
    movieId = movieId,
    movieTitle = movieTitle,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    cinema = cinema,
    showDate = LocalDate.ofEpochDay(showDateEpochDay),
    showTime = showTime,
    format = runCatching { ScreenFormat.valueOf(format) }.getOrDefault(ScreenFormat.TWO_D),
    seats = seats.split(",").filter { it.isNotBlank() },
    specialTags = specialTags.split(",").mapNotNull { SpecialTag.fromStored(it.trim()) },
    totalPrice = totalPrice,
    points = points,
    bookedAt = bookedAt,
)

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val reviewDao: UserReviewDao,
    private val userDao: UserDao,
    private val session: SessionStore,
) : ReviewRepository {

    private val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE

    override fun observeReviews(movieId: Int): Flow<List<Review>> =
        combine(reviewDao.observeForMovie(movieId), session.userId) { list, userId ->
            list.map { entity ->
                Review(
                    id = "local-${entity.id}",
                    author = entity.authorName,
                    rating = entity.rating * 2.0,
                    content = entity.content,
                    createdAt = Instant.ofEpochMilli(entity.createdAt)
                        .atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormat),
                    isMine = entity.userId == userId,
                    isFromApp = true,
                )
            }.sortedByDescending { it.isMine }
        }

    override suspend fun submitReview(movieId: Int, rating: Int, content: String): Result<Unit> {
        val userId = session.userId.value
            ?: return Result.failure(AuthException(AuthError.NOT_LOGGED_IN))
        val user = userDao.getById(userId)
            ?: return Result.failure(AuthException(AuthError.NOT_LOGGED_IN))
        reviewDao.upsert(
            UserReviewEntity(
                movieId = movieId,
                userId = userId,
                authorName = user.fullName,
                rating = rating.coerceIn(1, 5),
                content = content.trim(),
                createdAt = System.currentTimeMillis(),
            ),
        )
        return Result.success(Unit)
    }

    override suspend fun deleteMyReview(movieId: Int) {
        val userId = session.userId.value ?: return
        reviewDao.delete(movieId, userId)
    }
}
