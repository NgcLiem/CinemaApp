package com.ngcliem.movieexplorer.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.ngcliem.movieexplorer.data.firebase.FirestorePaths
import com.ngcliem.movieexplorer.data.firebase.SessionStore
import com.ngcliem.movieexplorer.data.firebase.firebaseCall
import com.ngcliem.movieexplorer.data.firebase.snapshots
import com.ngcliem.movieexplorer.domain.model.Booking
import com.ngcliem.movieexplorer.domain.model.BookingRequest
import com.ngcliem.movieexplorer.domain.model.MemberTier
import com.ngcliem.movieexplorer.domain.model.Review
import com.ngcliem.movieexplorer.domain.model.ScreenFormat
import com.ngcliem.movieexplorer.domain.model.SeatMap
import com.ngcliem.movieexplorer.domain.model.SpecialTag
import com.ngcliem.movieexplorer.domain.model.User
import com.ngcliem.movieexplorer.domain.model.discountedPrice
import com.ngcliem.movieexplorer.domain.model.pointsFor
import com.ngcliem.movieexplorer.domain.repository.AuthRepository
import com.ngcliem.movieexplorer.domain.repository.BookingRepository
import com.ngcliem.movieexplorer.domain.repository.ReviewRepository
import com.ngcliem.movieexplorer.domain.validation.AuthError
import com.ngcliem.movieexplorer.domain.validation.AuthException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Accounts use Firebase Authentication (Firebase stores and checks the password);
 * the name and phone number are kept in Firestore at users/{uid}.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val session: SessionStore,
) : AuthRepository {

    override val currentUser: Flow<User?> = session.userId.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(null)
        } else {
            FirestorePaths.user(db, uid).snapshots()
                .map { doc -> doc.toUser() ?: fallbackUser(uid) }
                // Profile not readable (e.g. offline on a fresh install): use the Auth data.
                .catch { emit(fallbackUser(uid)) }
        }
    }

    override fun isLoggedIn(): Boolean = auth.currentUser != null

    override suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
    ): Result<User> {
        val name = fullName.trim()
        val normalizedEmail = email.trim().lowercase()
        val user = User(
            id = "",
            fullName = name,
            email = normalizedEmail,
            phone = phone.trim().replace(" ", ""),
            createdAt = System.currentTimeMillis(),
        )
        return authCall {
            val firebaseUser = auth.createUserWithEmailAndPassword(normalizedEmail, password).await().user
                ?: throw AuthException(AuthError.NOT_LOGGED_IN)
            firebaseUser.updateProfile(userProfileChangeRequest { displayName = name }).await()
            // Not awaited: Firestore queues the write and shows it locally right away.
            FirestorePaths.user(db, firebaseUser.uid).set(
                mapOf(
                    "fullName" to user.fullName,
                    "email" to user.email,
                    "phone" to user.phone,
                    "createdAt" to user.createdAt,
                ),
            )
            user.copy(id = firebaseUser.uid)
        }
    }

    override suspend fun login(email: String, password: String): Result<User> = authCall {
        val firebaseUser = auth.signInWithEmailAndPassword(email.trim().lowercase(), password).await().user
            ?: throw AuthException(AuthError.WRONG_CREDENTIALS)
        fallbackUser(firebaseUser.uid)
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        val user = auth.currentUser
        val email = user?.email ?: return Result.failure(AuthException(AuthError.NOT_LOGGED_IN))
        if (currentPassword == newPassword) return Result.failure(AuthException(AuthError.SAME_PASSWORD))
        // Firebase asks for a recent sign-in before changing the password.
        val reauth = authCall(wrongPassword = AuthError.WRONG_CURRENT_PASSWORD) {
            user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
        }
        if (reauth.isFailure) return reauth.map { }
        return authCall { user.updatePassword(newPassword).await() }.map { }
    }

    override suspend fun logout() {
        auth.signOut()
    }

    private fun fallbackUser(uid: String): User {
        val firebaseUser = auth.currentUser
        return User(
            id = uid,
            fullName = firebaseUser?.displayName.orEmpty().ifBlank { firebaseUser?.email.orEmpty() },
            email = firebaseUser?.email.orEmpty(),
            phone = "",
            createdAt = firebaseUser?.metadata?.creationTimestamp ?: 0L,
        )
    }

    /** Runs an Auth call and turns Firebase errors into the app's own [AuthError]s. */
    private inline fun <T> authCall(
        wrongPassword: AuthError = AuthError.WRONG_CREDENTIALS,
        block: () -> T,
    ): Result<T> = firebaseCall(block).recoverCatching { e ->
        throw when (e) {
            is FirebaseAuthUserCollisionException -> AuthException(AuthError.EMAIL_TAKEN)
            is FirebaseAuthWeakPasswordException -> AuthException(AuthError.PASSWORD_WEAK)
            is FirebaseAuthInvalidUserException -> AuthException(AuthError.WRONG_CREDENTIALS)
            is FirebaseAuthInvalidCredentialsException -> AuthException(wrongPassword)
            else -> e
        }
    }
}

private fun DocumentSnapshot.toUser(): User? {
    if (!exists()) return null
    return User(
        id = id,
        fullName = getString("fullName").orEmpty(),
        email = getString("email").orEmpty(),
        phone = getString("phone").orEmpty(),
        createdAt = getLong("createdAt") ?: 0L,
    )
}

/**
 * Tickets live at users/{uid}/bookings. Seats sold for a showing are also written to
 * showings/{id}, in the same transaction, so two people can never buy the same seat.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class BookingRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val session: SessionStore,
) : BookingRepository {

    override fun observeMyBookings(): Flow<List<Booking>> = session.userId.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(emptyList())
        } else {
            FirestorePaths.bookings(db, uid)
                .orderBy("bookedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.documents.mapNotNull { it.toBooking() } }
                .catch { emit(emptyList()) }
        }
    }

    override suspend fun takenSeats(
        movieId: Int,
        cinema: String,
        date: LocalDate,
        time: String,
    ): Set<String> {
        val sold = firebaseCall {
            FirestorePaths.showing(db, movieId, cinema, date.toEpochDay(), time).get().await().seats()
        }.getOrDefault(emptyList())
        return SeatMap.simulatedTakenSeats(movieId, cinema, date, time) + sold
    }

    override suspend fun book(request: BookingRequest): Result<Booking> {
        val uid = session.userId.value
            ?: return Result.failure(AuthException(AuthError.NOT_LOGGED_IN))
        if (request.seats.isEmpty()) return Result.failure(AuthException(AuthError.NO_SEATS))
        val simulated = SeatMap.simulatedTakenSeats(request.movieId, request.cinema, request.showDate, request.showTime)
        if (request.seats.any { it in simulated }) {
            return Result.failure(AuthException(AuthError.SEATS_TAKEN))
        }

        return firebaseCall {
            // The discount uses the tier the user had *before* this booking.
            val totalPoints = FirestorePaths.bookings(db, uid).get().await().documents
                .sumOf { (it.getLong("points") ?: 0L).toInt() }
            val paid = discountedPrice(request.totalPrice, MemberTier.forPoints(totalPoints))
            val bookingRef = FirestorePaths.bookings(db, uid).document()
            val booking = Booking(
                id = bookingRef.id,
                code = generateCode(),
                movieId = request.movieId,
                movieTitle = request.movieTitle,
                posterUrl = request.posterUrl,
                backdropUrl = request.backdropUrl,
                cinema = request.cinema,
                showDate = request.showDate,
                showTime = request.showTime,
                format = request.format,
                seats = request.seats,
                specialTags = request.specialTags,
                totalPrice = paid,
                points = pointsFor(paid),
                bookedAt = System.currentTimeMillis(),
            )
            val showingRef = FirestorePaths.showing(
                db, request.movieId, request.cinema, request.showDate.toEpochDay(), request.showTime,
            )
            // Transactions need the server, so an offline booking fails instead of double-selling.
            val booked = db.runTransaction { tx ->
                val sold = tx.get(showingRef).seats()
                if (request.seats.any { it in sold }) return@runTransaction false
                tx.set(
                    showingRef,
                    mapOf("seats" to FieldValue.arrayUnion(*request.seats.toTypedArray())),
                    SetOptions.merge(),
                )
                tx.set(bookingRef, booking.toFirestore(uid))
                true
            }.await()
            if (!booked) throw AuthException(AuthError.SEATS_TAKEN)
            booking
        }
    }

    private fun generateCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return "MX" + (1..8).map { alphabet.random() }.joinToString("")
    }
}

private fun DocumentSnapshot.seats(): List<String> =
    (get("seats") as? List<*>)?.filterIsInstance<String>().orEmpty()

private fun Booking.toFirestore(uid: String): Map<String, Any?> = mapOf(
    "userId" to uid,
    "code" to code,
    "movieId" to movieId,
    "movieTitle" to movieTitle,
    "posterUrl" to posterUrl,
    "backdropUrl" to backdropUrl,
    "cinema" to cinema,
    "showDateEpochDay" to showDate.toEpochDay(),
    "showTime" to showTime,
    "format" to format.name,
    "seats" to seats,
    "specialTags" to specialTags.map { it.name },
    "totalPrice" to totalPrice,
    "points" to points,
    "bookedAt" to bookedAt,
)

private fun DocumentSnapshot.toBooking(): Booking? {
    val epochDay = getLong("showDateEpochDay") ?: return null
    return Booking(
        id = id,
        code = getString("code").orEmpty(),
        movieId = (getLong("movieId") ?: 0L).toInt(),
        movieTitle = getString("movieTitle").orEmpty(),
        posterUrl = getString("posterUrl"),
        backdropUrl = getString("backdropUrl"),
        cinema = getString("cinema").orEmpty(),
        showDate = LocalDate.ofEpochDay(epochDay),
        showTime = getString("showTime").orEmpty(),
        format = runCatching { ScreenFormat.valueOf(getString("format").orEmpty()) }
            .getOrDefault(ScreenFormat.TWO_D),
        seats = seats(),
        specialTags = (get("specialTags") as? List<*>).orEmpty()
            .mapNotNull { (it as? String)?.let(SpecialTag::fromStored) },
        totalPrice = getLong("totalPrice") ?: 0L,
        points = (getLong("points") ?: 0L).toInt(),
        bookedAt = getLong("bookedAt") ?: 0L,
    )
}

/**
 * Reviews written in the app, shared by every user: movies/{movieId}/reviews/{uid}.
 * Using the uid as the document id keeps one review per user per movie.
 */
@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val session: SessionStore,
) : ReviewRepository {

    private val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE

    override fun observeReviews(movieId: Int): Flow<List<Review>> =
        combine(
            FirestorePaths.reviews(db, movieId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { it.documents }
                .catch { emit(emptyList()) },
            session.userId,
        ) { docs, uid ->
            docs.map { doc ->
                val createdAt = doc.getLong("createdAt") ?: 0L
                Review(
                    id = "app-${doc.id}",
                    author = doc.getString("authorName").orEmpty(),
                    rating = (doc.getLong("rating") ?: 0L) * 2.0,
                    content = doc.getString("content").orEmpty(),
                    createdAt = Instant.ofEpochMilli(createdAt)
                        .atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormat),
                    isMine = doc.id == uid,
                    isFromApp = true,
                )
            }.sortedByDescending { it.isMine }
        }

    override suspend fun submitReview(movieId: Int, rating: Int, content: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(AuthException(AuthError.NOT_LOGGED_IN))
        val now = System.currentTimeMillis()
        // Not awaited: the write is queued (also offline) and the list updates at once.
        db.batch()
            .set(
                FirestorePaths.reviews(db, movieId).document(user.uid),
                mapOf(
                    "movieId" to movieId,
                    "userId" to user.uid,
                    "authorName" to user.displayName.orEmpty().ifBlank { user.email.orEmpty() },
                    "rating" to rating.coerceIn(1, 5),
                    "content" to content.trim(),
                    "createdAt" to now,
                ),
            )
            // Also create movies/{movieId}; without it the Firebase console hides "movies".
            .set(
                FirestorePaths.movie(db, movieId),
                mapOf("movieId" to movieId, "lastReviewAt" to now),
                SetOptions.merge(),
            )
            .commit()
        return Result.success(Unit)
    }

    override suspend fun deleteMyReview(movieId: Int) {
        val uid = session.userId.value ?: return
        FirestorePaths.reviews(db, movieId).document(uid).delete()
    }
}
