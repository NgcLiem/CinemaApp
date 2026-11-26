package com.ngcliem.movieexplorer.domain.model

import java.time.LocalDate

/** The movie lists shown on the home screen. */
enum class MovieCategory {
    NOW_PLAYING,
    UPCOMING,
    POPULAR,
}

/** A review, either from TMDB or written by a user of this app. */
data class Review(
    val id: String,
    val author: String,
    /** Score out of 10, or null when the author did not give one. */
    val rating: Double?,
    val content: String,
    /** yyyy-MM-dd, may be blank. */
    val createdAt: String,
    val avatarUrl: String? = null,
    val isMine: Boolean = false,
    val isFromApp: Boolean = false,
)

/**
 * UPCOMING: not released yet (only early screenings). NOW_SHOWING: released in the
 * last [NOW_SHOWING_DAYS] days. RELEASED: an older movie (shown again on request).
 */
enum class ReleaseStatus { NOW_SHOWING, UPCOMING, RELEASED, UNKNOWN }

const val NOW_SHOWING_DAYS = 90L

fun releaseStatusOf(releaseDate: String, today: LocalDate = LocalDate.now()): ReleaseStatus {
    val date = parseDateOrNull(releaseDate) ?: return ReleaseStatus.UNKNOWN
    return when {
        date.isAfter(today) -> ReleaseStatus.UPCOMING
        date.isBefore(today.minusDays(NOW_SHOWING_DAYS)) -> ReleaseStatus.RELEASED
        else -> ReleaseStatus.NOW_SHOWING
    }
}

fun parseDateOrNull(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
