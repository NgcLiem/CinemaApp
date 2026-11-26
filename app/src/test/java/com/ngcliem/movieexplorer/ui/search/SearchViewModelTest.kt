package com.ngcliem.movieexplorer.ui.search

import com.ngcliem.movieexplorer.domain.usecase.GetMovieSectionUseCase
import com.ngcliem.movieexplorer.domain.usecase.SearchMoviesUseCase
import com.ngcliem.movieexplorer.testutil.FakeSearchHistory
import com.ngcliem.movieexplorer.testutil.FakeMovieRepository
import com.ngcliem.movieexplorer.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeMovieRepository
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        repository = FakeMovieRepository()
        viewModel = SearchViewModel(
            searchMovies = SearchMoviesUseCase(repository),
            getMovieSection = GetMovieSectionUseCase(repository),
            searchHistory = FakeSearchHistory(),
        )
    }

    @Test
    fun `searches only once after the user stops typing`() = runTest {
        backgroundScope.launch { viewModel.results.collect {} }

        viewModel.onQueryChange("b")
        advanceTimeBy(100)
        viewModel.onQueryChange("ba")
        advanceTimeBy(100)
        viewModel.onQueryChange("bat")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MS + 1)

        assertEquals(listOf("bat"), repository.searchQueries)
    }

    @Test
    fun `blank query does not call the API and query is trimmed`() = runTest {
        backgroundScope.launch { viewModel.results.collect {} }

        viewModel.onQueryChange("    ")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MS + 1)
        assertTrue(repository.searchQueries.isEmpty())

        viewModel.onQueryChange("  dune  ")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MS + 1)
        assertEquals(listOf("dune"), repository.searchQueries)
        assertEquals("dune", viewModel.activeQuery.value)
    }

    @Test
    fun `clearing the query resets the text field`() = runTest {
        viewModel.onQueryChange("matrix")
        viewModel.onClearQuery()

        assertEquals("", viewModel.query.value)
    }
}
