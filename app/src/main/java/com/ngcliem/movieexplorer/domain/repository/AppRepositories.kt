package com.ngcliem.movieexplorer.domain.repository

import com.ngcliem.movieexplorer.domain.model.Booking
import com.ngcliem.movieexplorer.domain.model.BookingRequest
import com.ngcliem.movieexplorer.domain.model.Review
import com.ngcliem.movieexplorer.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** Accounts are stored on the device (Room); the password is salted and hashed. */
interface AuthRepository {

    /** The signed-in user, or null when signed out. */
    val currentUser: Flow<User?>

    fun isLoggedIn(): Boolean

    suspend fun register(fullName: String, email: String, phone: String, password: String): Result<User>

    suspend fun login(email: String, password: String): Result<User>

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>

    suspend fun logout()
}

interface BookingRepository {

    /** All bookings of the signed-in user, newest first. */
    fun observeMyBookings(): Flow<List<Booking>>

    /** Seats already sold for a showing (real bookings + simulated customers). */
    suspend fun takenSeats(movieId: Int, cinema: String, date: LocalDate, time: String): Set<String>

    /** Applies the member discount, earns points and saves the booking. */
    suspend fun book(request: BookingRequest): Result<Booking>
}

/** The last queries typed in the search screen, newest first. */
interface SearchHistoryRepository {
    val history: StateFlow<List<String>>

    fun add(query: String)

    fun remove(query: String)

    fun clear()
}

/** Reviews written inside the app (stored locally). */
interface ReviewRepository {

    fun observeReviews(movieId: Int): Flow<List<Review>>

    suspend fun submitReview(movieId: Int, rating: Int, content: String): Result<Unit>

    suspend fun deleteMyReview(movieId: Int)
}
