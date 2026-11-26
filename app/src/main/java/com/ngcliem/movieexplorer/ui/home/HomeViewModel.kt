package com.ngcliem.movieexplorer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngcliem.movieexplorer.domain.model.Movie
import com.ngcliem.movieexplorer.domain.model.MovieCategory
import com.ngcliem.movieexplorer.domain.model.User
import com.ngcliem.movieexplorer.domain.repository.AuthRepository
import com.ngcliem.movieexplorer.domain.usecase.GetMovieSectionUseCase
import com.ngcliem.movieexplorer.data.settings.LanguageStore
import com.ngcliem.movieexplorer.ui.components.UiText
import com.ngcliem.movieexplorer.ui.components.toUiText
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.distinctUntilChanged
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectionState(
    val isLoading: Boolean = true,
    val movies: List<Movie> = emptyList(),
    val errorMessage: UiText? = null,
)

data class HomeUiState(
    val sections: Map<MovieCategory, SectionState> =
        MovieCategory.entries.associateWith { SectionState() },
) {
    fun section(category: MovieCategory) = sections.getValue(category)

    val isRefreshing: Boolean
        get() = sections.values.any { it.isLoading } && sections.values.any { it.movies.isNotEmpty() }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMovieSection: GetMovieSectionUseCase,
    authRepository: AuthRepository,
    languageStore: LanguageStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val user: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        refresh()
        // Reload titles and overviews in the new language after a language change.
        viewModelScope.launch {
            languageStore.language.drop(1).distinctUntilChanged().collect { refresh() }
        }
    }

    fun refresh() {
        MovieCategory.entries.forEach(::loadSection)
    }

    fun loadSection(category: MovieCategory) {
        viewModelScope.launch {
            updateSection(category) { it.copy(isLoading = true, errorMessage = null) }
            getMovieSection(category)
                .onSuccess { movies ->
                    updateSection(category) { SectionState(isLoading = false, movies = movies) }
                }
                .onFailure { error ->
                    updateSection(category) {
                        it.copy(isLoading = false, errorMessage = error.toUiText())
                    }
                }
        }
    }

    private fun updateSection(category: MovieCategory, transform: (SectionState) -> SectionState) {
        _uiState.update { state ->
            state.copy(sections = state.sections + (category to transform(state.section(category))))
        }
    }
}
