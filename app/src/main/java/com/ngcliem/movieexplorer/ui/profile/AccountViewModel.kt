package com.ngcliem.movieexplorer.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngcliem.movieexplorer.data.settings.AppLanguage
import com.ngcliem.movieexplorer.data.settings.LanguageStore
import com.ngcliem.movieexplorer.domain.model.Booking
import com.ngcliem.movieexplorer.domain.model.BookingStats
import com.ngcliem.movieexplorer.domain.model.User
import com.ngcliem.movieexplorer.domain.model.computeStats
import com.ngcliem.movieexplorer.domain.repository.AuthRepository
import com.ngcliem.movieexplorer.domain.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val bookings: List<Booking> = emptyList(),
    val stats: BookingStats = BookingStats(),
)

/** Shared by the profile, tickets and membership screens. */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    bookingRepository: BookingRepository,
    private val languageStore: LanguageStore,
) : ViewModel() {

    val uiState: StateFlow<AccountUiState> = combine(
        authRepository.currentUser,
        bookingRepository.observeMyBookings(),
    ) { user, bookings ->
        AccountUiState(
            isLoading = false,
            user = user,
            bookings = bookings,
            stats = computeStats(bookings),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState())

    val language: StateFlow<AppLanguage> = languageStore.language

    fun setLanguage(language: AppLanguage) = languageStore.setLanguage(language)

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
