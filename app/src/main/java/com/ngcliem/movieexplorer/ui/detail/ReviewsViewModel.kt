package com.ngcliem.movieexplorer.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngcliem.movieexplorer.domain.model.Review
import com.ngcliem.movieexplorer.domain.repository.AuthRepository
import com.ngcliem.movieexplorer.domain.repository.ReviewRepository
import com.ngcliem.movieexplorer.domain.usecase.GetMovieReviewsUseCase
import com.ngcliem.movieexplorer.ui.components.UiText
import com.ngcliem.movieexplorer.ui.components.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewsUiState(
    val isLoadingTmdb: Boolean = true,
    val tmdbError: UiText? = null,
    val appReviews: List<Review> = emptyList(),
    val tmdbReviews: List<Review> = emptyList(),
    val isLoggedIn: Boolean = false,
) {
    val myReview: Review? get() = appReviews.firstOrNull { it.isMine }

    val allReviews: List<Review> get() = appReviews + tmdbReviews

    /** Average of the in-app ratings, out of 5 stars. */
    val appAverageStars: Double?
        get() = appReviews.mapNotNull { it.rating }.takeIf { it.isNotEmpty() }?.average()?.div(2)

    /** Number of in-app reviews per star (index 0 = 1 star). */
    val starHistogram: List<Int>
        get() = (1..5).map { star -> appReviews.count { (it.rating ?: 0.0) / 2 == star.toDouble() } }
}

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMovieReviews: GetMovieReviewsUseCase,
    private val reviewRepository: ReviewRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val movieId: Int = checkNotNull(savedStateHandle.get<Int>(DetailViewModel.MOVIE_ID_KEY))

    private val remote = MutableStateFlow(ReviewsUiState())

    val uiState: StateFlow<ReviewsUiState> = combine(
        remote,
        reviewRepository.observeReviews(movieId),
        authRepository.currentUser,
    ) { state, local, user ->
        state.copy(appReviews = local, isLoggedIn = user != null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewsUiState())

    init {
        loadTmdbReviews()
    }

    fun loadTmdbReviews() {
        viewModelScope.launch {
            remote.update { it.copy(isLoadingTmdb = true, tmdbError = null) }
            getMovieReviews(movieId)
                .onSuccess { reviews -> remote.update { it.copy(isLoadingTmdb = false, tmdbReviews = reviews) } }
                .onFailure { e -> remote.update { it.copy(isLoadingTmdb = false, tmdbError = e.toUiText()) } }
        }
    }

    fun submit(stars: Int, content: String) {
        viewModelScope.launch { reviewRepository.submitReview(movieId, stars, content) }
    }

    fun deleteMine() {
        viewModelScope.launch { reviewRepository.deleteMyReview(movieId) }
    }
}
