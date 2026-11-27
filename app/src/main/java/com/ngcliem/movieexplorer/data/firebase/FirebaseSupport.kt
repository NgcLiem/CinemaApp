package com.ngcliem.movieexplorer.data.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The uid of the signed-in Firebase user, as a StateFlow so repositories can read it
 * synchronously and react when the user signs in or out. Firebase keeps the session
 * across app restarts by itself.
 */
@Singleton
class SessionStore @Inject constructor(auth: FirebaseAuth) {

    private val _userId = MutableStateFlow(auth.currentUser?.uid)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    init {
        auth.addAuthStateListener { _userId.value = it.currentUser?.uid }
    }
}

/** Where each kind of data lives in Firestore. */
object FirestorePaths {
    const val USERS = "users"
    const val BOOKINGS = "bookings"
    const val FAVORITES = "favorites"
    const val MOVIES = "movies"
    const val REVIEWS = "reviews"
    const val SHOWINGS = "showings"

    fun user(db: FirebaseFirestore, uid: String) = db.collection(USERS).document(uid)
    fun bookings(db: FirebaseFirestore, uid: String) = user(db, uid).collection(BOOKINGS)
    fun favorites(db: FirebaseFirestore, uid: String) = user(db, uid).collection(FAVORITES)
    fun movie(db: FirebaseFirestore, movieId: Int) = db.collection(MOVIES).document(movieId.toString())
    fun reviews(db: FirebaseFirestore, movieId: Int) = movie(db, movieId).collection(REVIEWS)

    /** One document per showing holds the seats sold for it. */
    fun showing(db: FirebaseFirestore, movieId: Int, cinema: String, epochDay: Long, time: String): DocumentReference {
        // Document ids cannot contain '/', so keep only safe characters.
        val safeCinema = cinema.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-')
        val safeTime = time.replace(":", "")
        return db.collection(SHOWINGS).document("${movieId}_${safeCinema}_${epochDay}_$safeTime")
    }
}

/** Live results of a query; emits again every time the data changes (also offline). */
fun Query.snapshots(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error.toAppException())
        } else if (snapshot != null) {
            trySend(snapshot)
        }
    }
    awaitClose { registration.remove() }
}

fun DocumentReference.snapshots(): Flow<DocumentSnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error.toAppException())
        } else if (snapshot != null) {
            trySend(snapshot)
        }
    }
    awaitClose { registration.remove() }
}

/**
 * Network problems become [IOException], so the UI shows its usual
 * "no internet" message without knowing about Firebase.
 */
fun Throwable.toAppException(): Throwable = when {
    this is FirebaseNetworkException -> IOException(message, this)
    this is FirebaseFirestoreException && (
        code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
        ) -> IOException(message, this)
    else -> this
}

/** Runs a Firebase call and returns its failure (mapped by [toAppException]) as a [Result]. */
inline fun <T> firebaseCall(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toAppException())
    }
