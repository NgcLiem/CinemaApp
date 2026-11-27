package com.ngcliem.movieexplorer.domain

import com.ngcliem.movieexplorer.domain.model.Booking
import com.ngcliem.movieexplorer.domain.model.MemberTier
import com.ngcliem.movieexplorer.domain.model.ReleaseStatus
import com.ngcliem.movieexplorer.domain.model.ScreenFormat
import com.ngcliem.movieexplorer.domain.model.SeatMap
import com.ngcliem.movieexplorer.domain.model.SeatType
import com.ngcliem.movieexplorer.domain.model.SpecialTag
import com.ngcliem.movieexplorer.domain.validation.AuthError
import com.ngcliem.movieexplorer.domain.model.computeStats
import com.ngcliem.movieexplorer.domain.model.discountedPrice
import com.ngcliem.movieexplorer.domain.model.pointsFor
import com.ngcliem.movieexplorer.domain.model.releaseStatusOf
import com.ngcliem.movieexplorer.domain.validation.AuthValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AccountLogicTest {

    // --- Validation -------------------------------------------------------

    @Test
    fun `email validation`() {
        assertNull(AuthValidator.validateEmail("an.nguyen@example.com"))
        assertNotNull(AuthValidator.validateEmail(""))
        assertNotNull(AuthValidator.validateEmail("not-an-email"))
    }

    @Test
    fun `password needs letters and digits`() {
        assertNull(AuthValidator.validatePassword("abc123"))
        assertNotNull(AuthValidator.validatePassword("abc"))
        assertNotNull(AuthValidator.validatePassword("abcdefgh"))
        assertNotNull(AuthValidator.validatePassword("12345678"))
    }

    @Test
    fun `vietnamese phone numbers`() {
        assertNull(AuthValidator.validatePhone("0912345678"))
        assertNull(AuthValidator.validatePhone("+84912345678"))
        assertNotNull(AuthValidator.validatePhone("12345"))
    }

    @Test
    fun `validator explains what is wrong`() {
        assertEquals(AuthError.PASSWORD_TOO_SHORT, AuthValidator.validatePassword("a1"))
        assertEquals(AuthError.PASSWORD_WEAK, AuthValidator.validatePassword("abcdefgh"))
        assertEquals(AuthError.CONFIRM_MISMATCH, AuthValidator.validateConfirm("abc123", "abc12"))
    }

    @Test
    fun `special tags saved by the old version are still read`() {
        assertEquals(SpecialTag.VIP_SEAT, SpecialTag.fromStored("Ghế VIP"))
        assertEquals(SpecialTag.EARLY_SCREENING, SpecialTag.fromStored("EARLY_SCREENING"))
        assertNull(SpecialTag.fromStored("unknown"))
    }

    @Test
    fun `confirm password must match`() {
        assertNull(AuthValidator.validateConfirm("abc123", "abc123"))
        assertNotNull(AuthValidator.validateConfirm("abc123", "abc124"))
    }

    // --- Membership -------------------------------------------------------

    @Test
    fun `tier depends on points`() {
        assertEquals(MemberTier.MEMBER, MemberTier.forPoints(0))
        assertEquals(MemberTier.SILVER, MemberTier.forPoints(1_000))
        assertEquals(MemberTier.GOLD, MemberTier.forPoints(5_999))
        assertEquals(MemberTier.DIAMOND, MemberTier.forPoints(10_000))
    }

    @Test
    fun `discount and points`() {
        assertEquals(200_000L, discountedPrice(200_000, MemberTier.MEMBER))
        assertEquals(180_000L, discountedPrice(200_000, MemberTier.GOLD))
        assertEquals(150, pointsFor(150_999))
    }

    @Test
    fun `stats count tickets, watched movies and spending`() {
        val now = LocalDateTime.of(2026, 11, 24, 12, 0)
        val millis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val past = booking(1, movieId = 10, date = LocalDate.of(2026, 11, 1), seats = listOf("A1", "A2"), bookedAt = millis, tags = emptyList())
        val future = booking(2, movieId = 11, date = LocalDate.of(2026, 11, 30), seats = listOf("F1"), bookedAt = millis, tags = listOf(SpecialTag.VIP_SEAT))

        val stats = computeStats(listOf(past, future), now)

        assertEquals(2, stats.totalBookings)
        assertEquals(3, stats.totalTickets)
        assertEquals(1, stats.moviesWatched)
        assertEquals(1, stats.upcomingTickets)
        assertEquals(1, stats.specialTickets)
        assertEquals(past.totalPrice + future.totalPrice, stats.totalSpent)
        assertEquals(6, stats.monthly.size)
        assertEquals(3, stats.monthly.last().second)
    }

    // --- Seats ------------------------------------------------------------

    @Test
    fun `seat types and sweetbox pairs`() {
        assertEquals(SeatType.STANDARD, SeatMap.typeOf("A1"))
        assertEquals(SeatType.VIP, SeatMap.typeOf("G4"))
        assertEquals(SeatType.SWEETBOX, SeatMap.typeOf("H3"))
        assertEquals("H4", SeatMap.partnerOf("H3"))
        assertEquals("H3", SeatMap.partnerOf("H4"))
        assertNull(SeatMap.partnerOf("A1"))
    }

    @Test
    fun `price and special tags`() {
        val seats = listOf("A1", "F1")
        assertEquals(75_000L + 110_000L + 2 * 60_000L, SeatMap.subtotal(seats, ScreenFormat.IMAX))
        assertEquals(
            listOf(SpecialTag.EARLY_SCREENING, SpecialTag.IMAX, SpecialTag.VIP_SEAT),
            SeatMap.specialTags(seats, ScreenFormat.IMAX, true),
        )
        assertTrue(SeatMap.specialTags(listOf("A1"), ScreenFormat.TWO_D, false).isEmpty())
    }

    @Test
    fun `simulated taken seats are stable and keep sweetbox pairs together`() {
        val date = LocalDate.of(2026, 12, 1)
        val a = SeatMap.simulatedTakenSeats(1, "X", date, "20:00")
        val b = SeatMap.simulatedTakenSeats(1, "X", date, "20:00")
        assertEquals(a, b)
        a.filter { SeatMap.typeOf(it) == SeatType.SWEETBOX }.forEach { seat ->
            assertTrue(SeatMap.partnerOf(seat) in a)
        }
    }

    @Test
    fun `release status`() {
        val today = LocalDate.of(2026, 11, 24)
        assertEquals(ReleaseStatus.UPCOMING, releaseStatusOf("2026-12-20", today))
        assertEquals(ReleaseStatus.NOW_SHOWING, releaseStatusOf("2026-11-24", today))
        assertEquals(ReleaseStatus.RELEASED, releaseStatusOf("2008-04-30", today))
        assertEquals(ReleaseStatus.UNKNOWN, releaseStatusOf("", today))
    }

    private fun booking(
        id: Long,
        movieId: Int,
        date: LocalDate,
        seats: List<String>,
        bookedAt: Long,
        tags: List<SpecialTag>,
    ) = Booking(
        id = id.toString(),
        code = "MX$id",
        movieId = movieId,
        movieTitle = "Movie $movieId",
        posterUrl = null,
        backdropUrl = null,
        cinema = "C",
        showDate = date,
        showTime = "20:00",
        format = ScreenFormat.TWO_D,
        seats = seats,
        specialTags = tags,
        totalPrice = seats.size * 75_000L,
        points = seats.size * 75,
        bookedAt = bookedAt,
    )
}
