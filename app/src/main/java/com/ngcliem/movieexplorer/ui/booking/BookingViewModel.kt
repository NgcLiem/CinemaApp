package com.ngcliem.movieexplorer.ui.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngcliem.movieexplorer.BuildConfig
import com.ngcliem.movieexplorer.domain.model.Booking
import com.ngcliem.movieexplorer.domain.model.VietQr
import com.ngcliem.movieexplorer.domain.model.BookingRequest
import com.ngcliem.movieexplorer.domain.model.MemberTier
import com.ngcliem.movieexplorer.domain.model.MovieDetail
import com.ngcliem.movieexplorer.domain.model.ReleaseStatus
import com.ngcliem.movieexplorer.domain.model.ScreenFormat
import com.ngcliem.movieexplorer.domain.model.SeatMap
import com.ngcliem.movieexplorer.domain.model.computeStats
import com.ngcliem.movieexplorer.domain.model.discountedPrice
import com.ngcliem.movieexplorer.domain.model.releaseStatusOf
import com.ngcliem.movieexplorer.domain.repository.BookingRepository
import com.ngcliem.movieexplorer.domain.usecase.GetMovieDetailUseCase
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.domain.model.SpecialTag
import com.ngcliem.movieexplorer.ui.components.UiText
import com.ngcliem.movieexplorer.ui.components.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** A pending QR bank transfer for the current order. */
data class PaymentSession(
    val orderCode: String,
    val amount: Long,
    val qrPayload: String,
    val expiresAt: Long,
    /** False when no receiving bank account is configured: the QR code is for demo only. */
    val isRealTransfer: Boolean,
) {
    val transferNote: String get() = "MEIL $orderCode"
}

data class BookingUiState(
    val isLoading: Boolean = true,
    val errorMessage: UiText? = null,
    val movie: MovieDetail? = null,
    val cinema: String = SeatMap.cinemas.first(),
    val dates: List<LocalDate> = emptyList(),
    val date: LocalDate = LocalDate.now(),
    val availableTimes: List<String> = emptyList(),
    val time: String? = null,
    val format: ScreenFormat = ScreenFormat.TWO_D,
    val takenSeats: Set<String> = emptySet(),
    val selectedSeats: List<String> = emptyList(),
    val tier: MemberTier = MemberTier.MEMBER,
    val isSubmitting: Boolean = false,
    val message: UiText? = null,
    val completedBooking: Booking? = null,
    val payment: PaymentSession? = null,
    val isVerifyingPayment: Boolean = false,
) {
    val isEarlyScreening: Boolean
        get() = movie != null && releaseStatusOf(movie.releaseDate) == ReleaseStatus.UPCOMING

    val subtotal: Long get() = SeatMap.subtotal(selectedSeats, format)
    val total: Long get() = discountedPrice(subtotal, tier)
    val discount: Long get() = subtotal - total
    val specialTags: List<SpecialTag> get() = SeatMap.specialTags(selectedSeats, format, isEarlyScreening)
    val canSubmit: Boolean get() = time != null && selectedSeats.isNotEmpty() && !isSubmitting
}

@HiltViewModel
class BookingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMovieDetail: GetMovieDetailUseCase,
    private val bookingRepository: BookingRepository,
) : ViewModel() {

    private val movieId: Int = checkNotNull(savedStateHandle.get<Int>(MOVIE_ID_KEY))

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    init {
        val today = LocalDate.now()
        val dates = (0L until 7L).map { today.plusDays(it) }
        _uiState.update { it.copy(dates = dates) }
        selectDate(dates.first())
        loadMovie()
        viewModelScope.launch {
            bookingRepository.observeMyBookings().collect { bookings ->
                _uiState.update { it.copy(tier = computeStats(bookings).tier) }
            }
        }
    }

    fun loadMovie() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getMovieDetail(movieId)
                .onSuccess { movie -> _uiState.update { it.copy(isLoading = false, movie = movie) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.toUiText()) } }
        }
    }

    fun selectCinema(cinema: String) {
        _uiState.update { it.copy(cinema = cinema, selectedSeats = emptyList()) }
        refreshTakenSeats()
    }

    fun selectDate(date: LocalDate) {
        val now = LocalTime.now()
        val times = SeatMap.showtimes.filter { date != LocalDate.now() || LocalTime.parse(it).isAfter(now) }
        _uiState.update {
            it.copy(
                date = date,
                availableTimes = times,
                time = it.time?.takeIf { t -> t in times } ?: times.firstOrNull(),
                selectedSeats = emptyList(),
            )
        }
        refreshTakenSeats()
    }

    fun selectTime(time: String) {
        _uiState.update { it.copy(time = time, selectedSeats = emptyList()) }
        refreshTakenSeats()
    }

    fun selectFormat(format: ScreenFormat) = _uiState.update { it.copy(format = format) }

    fun toggleSeat(seat: String) {
        val state = _uiState.value
        if (seat in state.takenSeats) return
        val group = listOfNotNull(seat, SeatMap.partnerOf(seat))
        _uiState.update {
            if (seat in it.selectedSeats) {
                it.copy(selectedSeats = it.selectedSeats - group.toSet())
            } else if (it.selectedSeats.size + group.size > SeatMap.MAX_SEATS_PER_ORDER) {
                it.copy(message = UiText(R.string.max_seats, listOf(SeatMap.MAX_SEATS_PER_ORDER)))
            } else {
                it.copy(selectedSeats = (it.selectedSeats + group).distinct())
            }
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    /** Opens a QR payment for the current order total (valid for [PAYMENT_TIMEOUT_MS]). */
    fun startQrPayment() {
        val state = _uiState.value
        if (!state.canSubmit) return
        val orderCode = (1..6).map { ORDER_ALPHABET.random() }.joinToString("")
        val session = PaymentSession(
            orderCode = orderCode,
            amount = state.total,
            qrPayload = "",
            expiresAt = System.currentTimeMillis() + PAYMENT_TIMEOUT_MS,
            isRealTransfer = BuildConfig.VIETQR_BANK_BIN.isNotBlank() && BuildConfig.VIETQR_ACCOUNT_NO.isNotBlank(),
        )
        val payload = if (session.isRealTransfer) {
            VietQr.payload(BuildConfig.VIETQR_BANK_BIN, BuildConfig.VIETQR_ACCOUNT_NO, session.amount, session.transferNote)
        } else {
            "MEILCINEMA:PAY?order=$orderCode&amount=${session.amount}"
        }
        _uiState.update { it.copy(payment = session.copy(qrPayload = payload)) }
    }

    fun cancelPayment() = _uiState.update { it.copy(payment = null, isVerifyingPayment = false) }

    /**
     * Called when the customer says the transfer is done. The app has no payment backend,
     * so this simulates the bank confirmation before issuing the tickets.
     */
    fun confirmPaid() {
        val payment = _uiState.value.payment ?: return
        if (_uiState.value.isVerifyingPayment) return
        if (System.currentTimeMillis() > payment.expiresAt) return
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifyingPayment = true) }
            delay(VERIFY_DELAY_MS)
            _uiState.update { it.copy(isVerifyingPayment = false, payment = null) }
            confirm()
        }
    }

    fun confirm() {
        val state = _uiState.value
        val movie = state.movie ?: return
        val time = state.time ?: return
        if (!state.canSubmit) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            bookingRepository.book(
                BookingRequest(
                    movieId = movie.id,
                    movieTitle = movie.title,
                    posterUrl = movie.posterUrl,
                    backdropUrl = movie.backdropUrl,
                    cinema = state.cinema,
                    showDate = state.date,
                    showTime = time,
                    format = state.format,
                    seats = state.selectedSeats.sortedWith(compareBy({ it.first() }, { it.drop(1).toInt() })),
                    specialTags = state.specialTags,
                    totalPrice = state.subtotal,
                ),
            ).onSuccess { booking ->
                _uiState.update { it.copy(isSubmitting = false, completedBooking = booking) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSubmitting = false, message = e.toUiText()) }
                refreshTakenSeats()
            }
        }
    }

    private fun refreshTakenSeats() {
        val state = _uiState.value
        val time = state.time ?: return
        viewModelScope.launch {
            val taken = bookingRepository.takenSeats(movieId, state.cinema, state.date, time)
            _uiState.update { it.copy(takenSeats = taken, selectedSeats = it.selectedSeats - taken) }
        }
    }

    companion object {
        /** Must match the property name in BookingRoute. */
        const val MOVIE_ID_KEY = "movieId"

        const val PAYMENT_TIMEOUT_MS = 10 * 60 * 1000L
        private const val VERIFY_DELAY_MS = 1800L
        private const val ORDER_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
