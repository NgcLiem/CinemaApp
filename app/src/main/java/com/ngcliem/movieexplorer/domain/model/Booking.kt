package com.ngcliem.movieexplorer.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Seat categories in the cinema hall, with their base price in VND. */
enum class SeatType(val price: Long, val isSpecial: Boolean) {
    STANDARD(75_000, false),
    VIP(110_000, true),
    SWEETBOX(125_000, true),
}

/** Why a ticket counts as a "special ticket". Stored in the database by name. */
enum class SpecialTag(private val legacyLabel: String) {
    EARLY_SCREENING("Suất chiếu sớm"),
    IMAX("IMAX"),
    VIP_SEAT("Ghế VIP"),
    SWEETBOX("Sweetbox");

    companion object {
        /** Accepts the enum name, or the Vietnamese label stored by version 2 of the app. */
        fun fromStored(value: String): SpecialTag? =
            entries.firstOrNull { it.name == value || it.legacyLabel == value }
    }
}

/** Projection format; the surcharge is added to every seat. */
enum class ScreenFormat(val label: String, val surcharge: Long, val isSpecial: Boolean) {
    TWO_D("2D", 0, false),
    THREE_D("3D", 30_000, false),
    IMAX("IMAX", 60_000, true),
}

/** A booked ticket order. One booking can hold several seats. */
data class Booking(
    val id: Long,
    val code: String,
    val movieId: Int,
    val movieTitle: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val cinema: String,
    val showDate: LocalDate,
    val showTime: String,
    val format: ScreenFormat,
    val seats: List<String>,
    /** Why this is a special ticket (IMAX, VIP seats, early screening...). Empty for normal tickets. */
    val specialTags: List<SpecialTag>,
    val totalPrice: Long,
    val points: Int,
    val bookedAt: Long,
) {
    val isSpecial: Boolean get() = specialTags.isNotEmpty()

    val showDateTime: LocalDateTime
        get() = LocalDateTime.of(showDate, runCatching { LocalTime.parse(showTime) }.getOrDefault(LocalTime.MIDNIGHT))

    fun isUpcoming(now: LocalDateTime = LocalDateTime.now()): Boolean = showDateTime.isAfter(now)
}

/** What the user picked on the booking screen. */
data class BookingRequest(
    val movieId: Int,
    val movieTitle: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val cinema: String,
    val showDate: LocalDate,
    val showTime: String,
    val format: ScreenFormat,
    val seats: List<String>,
    val specialTags: List<SpecialTag>,
    val totalPrice: Long,
)
