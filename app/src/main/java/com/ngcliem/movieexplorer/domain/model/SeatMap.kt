package com.ngcliem.movieexplorer.domain.model

import java.time.LocalDate
import kotlin.random.Random

/** Layout of the (fictional) cinema hall used by the booking screen. */
object SeatMap {
    val rows = listOf('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H')
    const val COLUMNS = 10
    const val MAX_SEATS_PER_ORDER = 8

    val cinemas = listOf(
        "Meil Cinema Nguyễn Du (Q.1)",
        "Meil Cinema Landmark (Bình Thạnh)",
        "Meil Cinema Hà Đông (Hà Nội)",
    )

    val showtimes = listOf("09:30", "12:15", "14:45", "17:30", "20:00", "22:30")

    fun seatId(row: Char, column: Int) = "$row$column"

    fun typeOf(seatId: String): SeatType = when (seatId.first()) {
        'F', 'G' -> SeatType.VIP
        'H' -> SeatType.SWEETBOX
        else -> SeatType.STANDARD
    }

    /** Sweetbox seats are sold in pairs: H1+H2, H3+H4... */
    fun partnerOf(seatId: String): String? {
        if (typeOf(seatId) != SeatType.SWEETBOX) return null
        val column = seatId.drop(1).toIntOrNull() ?: return null
        val partner = if (column % 2 == 1) column + 1 else column - 1
        return seatId(seatId.first(), partner)
    }

    /**
     * Seats already sold to other (simulated) customers. Deterministic, so the same
     * showing always looks the same.
     */
    fun simulatedTakenSeats(movieId: Int, cinema: String, date: LocalDate, time: String): Set<String> {
        val random = Random("$movieId|$cinema|$date|$time".hashCode())
        val taken = mutableSetOf<String>()
        rows.forEach { row ->
            for (column in 1..COLUMNS) {
                val id = seatId(row, column)
                if (id in taken) continue
                if (random.nextFloat() < 0.18f) {
                    taken += id
                    partnerOf(id)?.let { taken += it }
                }
            }
        }
        return taken
    }

    fun subtotal(seats: Collection<String>, format: ScreenFormat): Long =
        seats.sumOf { typeOf(it).price + format.surcharge }

    /** Reasons a ticket counts as a "special ticket". */
    fun specialTags(
        seats: Collection<String>,
        format: ScreenFormat,
        isEarlyScreening: Boolean,
    ): List<SpecialTag> = buildList {
        if (isEarlyScreening) add(SpecialTag.EARLY_SCREENING)
        if (format == ScreenFormat.IMAX) add(SpecialTag.IMAX)
        if (seats.any { typeOf(it) == SeatType.VIP }) add(SpecialTag.VIP_SEAT)
        if (seats.any { typeOf(it) == SeatType.SWEETBOX }) add(SpecialTag.SWEETBOX)
    }
}
