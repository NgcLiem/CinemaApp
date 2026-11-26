package com.ngcliem.movieexplorer.domain.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

/**
 * Membership program. Every 1.000đ paid earns 1 point; the tier depends on the
 * total points earned since registration and gives a discount on new bookings.
 */
enum class MemberTier(
    val minPoints: Int,
    val discountPercent: Int,
    val colorHex: Long,
) {
    MEMBER(minPoints = 0, discountPercent = 0, colorHex = 0xFF8D99AE),
    SILVER(minPoints = 1_000, discountPercent = 5, colorHex = 0xFFB0BEC5),
    GOLD(minPoints = 3_000, discountPercent = 10, colorHex = 0xFFFFC94D),
    DIAMOND(minPoints = 6_000, discountPercent = 15, colorHex = 0xFF7FDBFF);

    val next: MemberTier? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun forPoints(points: Int): MemberTier = entries.last { points >= it.minPoints }
    }
}

fun pointsFor(amountPaid: Long): Int = (amountPaid / 1_000).toInt()

/** Price of a booking after the membership discount. */
fun discountedPrice(subtotal: Long, tier: MemberTier): Long =
    subtotal - subtotal * tier.discountPercent / 100

/** Summary of everything the user has booked since registering. */
data class BookingStats(
    val totalBookings: Int = 0,
    val totalTickets: Int = 0,
    val moviesWatched: Int = 0,
    val upcomingTickets: Int = 0,
    val specialTickets: Int = 0,
    val totalSpent: Long = 0,
    val totalPoints: Int = 0,
    val firstBookingAt: Long? = null,
    /** Tickets per month for the last 6 months, oldest first. */
    val monthly: List<Pair<YearMonth, Int>> = emptyList(),
) {
    val tier: MemberTier get() = MemberTier.forPoints(totalPoints)

    /** 0..1 progress from the current tier to the next one (1 at the top tier). */
    val tierProgress: Float
        get() {
            val next = tier.next ?: return 1f
            return ((totalPoints - tier.minPoints).toFloat() / (next.minPoints - tier.minPoints))
                .coerceIn(0f, 1f)
        }

    val pointsToNextTier: Int? get() = tier.next?.let { it.minPoints - totalPoints }
}

fun computeStats(
    bookings: List<Booking>,
    now: LocalDateTime = LocalDateTime.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): BookingStats {
    val currentMonth = YearMonth.from(now)
    val months = (5 downTo 0).map { currentMonth.minusMonths(it.toLong()) }
    val ticketsByMonth = bookings.groupBy {
        YearMonth.from(Instant.ofEpochMilli(it.bookedAt).atZone(zone))
    }.mapValues { (_, list) -> list.sumOf { it.seats.size } }

    return BookingStats(
        totalBookings = bookings.size,
        totalTickets = bookings.sumOf { it.seats.size },
        moviesWatched = bookings.filterNot { it.isUpcoming(now) }.map { it.movieId }.distinct().size,
        upcomingTickets = bookings.filter { it.isUpcoming(now) }.sumOf { it.seats.size },
        specialTickets = bookings.filter { it.isSpecial }.sumOf { it.seats.size },
        totalSpent = bookings.sumOf { it.totalPrice },
        totalPoints = bookings.sumOf { it.points },
        firstBookingAt = bookings.minOfOrNull { it.bookedAt },
        monthly = months.map { it to (ticketsByMonth[it] ?: 0) },
    )
}
