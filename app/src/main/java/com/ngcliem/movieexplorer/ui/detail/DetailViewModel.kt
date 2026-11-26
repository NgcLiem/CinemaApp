package com.ngcliem.movieexplorer.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngcliem.movieexplorer.domain.model.MovieDetail
import com.ngcliem.movieexplorer.domain.usecase.GetMovieDetailUseCase
import com.ngcliem.movieexplorer.domain.usecase.ObserveIsFavoriteUseCase
import com.ngcliem.movieexplorer.domain.usecase.ToggleFavoriteUseCase
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

data class DetailUiState(
    val isLoading: Boolean = true,
    val movie: MovieDetail? = null,
    val errorMessage: UiText? = null,
    val isFavorite: Boolean = false,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMovieDetail: GetMovieDetailUseCase,
    observeIsFavorite: ObserveIsFavoriteUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {

    // Navigation stores the DetailRoute arguments in SavedStateHandle by property name.
    private val movieId: Int = checkNotNull(savedStateHandle.get<Int>(MOVIE_ID_KEY)) {
        "DetailViewModel needs a movieId argument"
    }

    private val loadState = MutableStateFlow(DetailUiState())

    /** Combines the loaded detail with the favorite status, which updates live from Room. */
    val uiState: StateFlow<DetailUiState> =
        combine(loadState, observeIsFavorite(movieId)) { state, isFavorite ->
            state.copy(isFavorite = isFavorite)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState(),
        )

    init {
        loadMovie()
    }

    fun loadMovie() {
        viewModelScope.launch {
            loadState.update { it.copy(isLoading = true, errorMessage = null) }
            getMovieDetail(movieId)
                .onSuccess { movie ->
                    loadState.update { it.copy(isLoading = false, movie = movie) }
                }
                .onFailure { error ->
                    loadState.update {
                        it.copy(isLoading = false, errorMessage = error.toUiText())
                    }
                }
        }
    }

    fun onToggleFavorite() {
        val state = uiState.value
        val movie = state.movie ?: return
        viewModelScope.launch {
            toggleFavorite(movie.toMovie(), isCurrentlyFavorite = state.isFavorite)
        }
    }

    companion object {
        /** Must match the property name in DetailRoute. */
        const val MOVIE_ID_KEY = "movieId"
    }
}
