package com.ngcliem.movieexplorer.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ngcliem.movieexplorer.domain.model.Movie
import com.ngcliem.movieexplorer.domain.model.MovieCategory
import com.ngcliem.movieexplorer.domain.repository.SearchHistoryRepository
import com.ngcliem.movieexplorer.domain.usecase.GetMovieSectionUseCase
import com.ngcliem.movieexplorer.domain.usecase.SearchMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMovies: SearchMoviesUseCase,
    private val getMovieSection: GetMovieSectionUseCase,
    private val searchHistory: SearchHistoryRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")

    /** Exactly what the user typed, shown in the text field. */
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * The query that is actually searched: it only updates after the user stops
     * typing for [DEBOUNCE_MS], so we don't call the API on every key press.
     */
    val activeQuery: StateFlow<String> = _query
        .debounce(DEBOUNCE_MS)
        .map { it.trim() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    /** flatMapLatest cancels the previous search when a new query arrives. */
    val results: Flow<PagingData<Movie>> = activeQuery
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(PagingData.empty()) else searchMovies(query)
        }
        .cachedIn(viewModelScope)

    /** Recent searches, shown while the search field is empty. */
    val history: StateFlow<List<String>> = searchHistory.history

    private val _trending = MutableStateFlow<List<Movie>>(emptyList())

    /** Top popular movies, suggested while the search field is empty. */
    val trending: StateFlow<List<Movie>> = _trending.asStateFlow()

    init {
        loadTrending()
    }

    fun loadTrending() {
        viewModelScope.launch {
            getMovieSection(MovieCategory.POPULAR).onSuccess { _trending.value = it.take(TRENDING_COUNT) }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onClearQuery() {
        _query.value = ""
    }

    /** Keyboard "search" pressed or a result opened: remember the query. */
    fun onSubmit() {
        searchHistory.add(_query.value)
    }

    fun onHistoryClick(query: String) {
        _query.value = query
        searchHistory.add(query)
    }

    fun onRemoveHistory(query: String) = searchHistory.remove(query)

    fun onClearHistory() = searchHistory.clear()

    companion object {
        const val DEBOUNCE_MS = 400L
        private const val TRENDING_COUNT = 10
    }
}
