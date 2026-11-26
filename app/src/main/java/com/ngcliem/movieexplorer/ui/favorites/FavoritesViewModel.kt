package com.ngcliem.movieexplorer.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngcliem.movieexplorer.domain.model.Movie
import com.ngcliem.movieexplorer.domain.usecase.ClearFavoritesUseCase
import com.ngcliem.movieexplorer.domain.usecase.ObserveFavoritesUseCase
import com.ngcliem.movieexplorer.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FavoriteSort { RECENT, RATING, TITLE }

sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState
    data class Success(val movies: List<Movie>, val sort: FavoriteSort) : FavoritesUiState
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    observeFavorites: ObserveFavoritesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val clearFavorites: ClearFavoritesUseCase,
) : ViewModel() {

    private val sort = MutableStateFlow(FavoriteSort.RECENT)

    /** The last removed movie, so the user can undo from the snackbar. */
    private val _lastRemoved = MutableStateFlow<Movie?>(null)
    val lastRemoved: StateFlow<Movie?> = _lastRemoved.asStateFlow()

    /** Updates automatically whenever a favorite is added or removed. */
    val uiState: StateFlow<FavoritesUiState> =
        combine(observeFavorites(), sort) { movies, order ->
            val sorted = when (order) {
                FavoriteSort.RECENT -> movies // already newest first
                FavoriteSort.RATING -> movies.sortedByDescending { it.voteAverage }
                FavoriteSort.TITLE -> movies.sortedBy { it.title.lowercase() }
            }
            FavoritesUiState.Success(sorted, order)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavoritesUiState.Loading,
        )

    fun onSortChange(newSort: FavoriteSort) {
        sort.value = newSort
    }

    fun remove(movie: Movie) {
        viewModelScope.launch {
            toggleFavorite(movie, isCurrentlyFavorite = true)
            _lastRemoved.value = movie
        }
    }

    fun undoRemove() {
        val movie = _lastRemoved.value ?: return
        _lastRemoved.value = null
        viewModelScope.launch { toggleFavorite(movie, isCurrentlyFavorite = false) }
    }

    fun removalShown() {
        _lastRemoved.value = null
    }

    fun clearAll() {
        viewModelScope.launch { clearFavorites() }
    }
}
