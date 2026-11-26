package com.ngcliem.movieexplorer.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.domain.usecase.GetMovieDetailUseCase
import com.ngcliem.movieexplorer.domain.usecase.ObserveIsFavoriteUseCase
import com.ngcliem.movieexplorer.domain.usecase.ToggleFavoriteUseCase
import com.ngcliem.movieexplorer.testutil.FakeMovieRepository
import com.ngcliem.movieexplorer.testutil.MainDispatcherRule
import com.ngcliem.movieexplorer.testutil.testDetail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMovieRepository()

    private fun createViewModel(movieId: Int = 1) = DetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(DetailViewModel.MOVIE_ID_KEY to movieId)),
        getMovieDetail = GetMovieDetailUseCase(repository),
        observeIsFavorite = ObserveIsFavoriteUseCase(repository),
        toggleFavorite = ToggleFavoriteUseCase(repository),
    )

    /** uiState only updates while someone collects it (WhileSubscribed). */
    private fun TestScope.startCollecting(viewModel: DetailViewModel) {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    @Test
    fun `shows movie when loading succeeds`() = runTest {
        repository.detailResult = Result.success(testDetail(1))
        val viewModel = createViewModel()
        startCollecting(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(testDetail(1), state.movie)
        assertNull(state.errorMessage)
    }

    @Test
    fun `shows friendly error when offline`() = runTest {
        repository.detailResult = Result.failure(IOException("no network"))
        val viewModel = createViewModel()
        startCollecting(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.movie)
        assertEquals(R.string.error_no_internet, state.errorMessage?.resId)
    }

    @Test
    fun `retry loads the movie after an error`() = runTest {
        repository.detailResult = Result.failure(IOException())
        val viewModel = createViewModel()
        startCollecting(viewModel)
        advanceUntilIdle()

        repository.detailResult = Result.success(testDetail(1))
        viewModel.loadMovie()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.movie)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `toggle favorite adds then removes the movie`() = runTest {
        repository.detailResult = Result.success(testDetail(1))
        val viewModel = createViewModel()
        startCollecting(viewModel)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isFavorite)

        viewModel.onToggleFavorite()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isFavorite)

        viewModel.onToggleFavorite()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isFavorite)
    }
}
