package com.ngcliem.movieexplorer.ui.movielist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.ui.components.titleRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import com.ngcliem.movieexplorer.domain.model.Movie
import com.ngcliem.movieexplorer.domain.model.MovieCategory
import com.ngcliem.movieexplorer.domain.usecase.GetMoviesByCategoryUseCase
import com.ngcliem.movieexplorer.ui.components.PagedMovieGrid
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class MovieListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getMovies: GetMoviesByCategoryUseCase,
) : ViewModel() {

    val category: MovieCategory = savedStateHandle.get<String>(CATEGORY_KEY)
        ?.let { runCatching { MovieCategory.valueOf(it) }.getOrNull() }
        ?: MovieCategory.POPULAR

    val movies: Flow<PagingData<Movie>> = getMovies(category).cachedIn(viewModelScope)

    companion object {
        /** Must match the property name in MovieListRoute. */
        const val CATEGORY_KEY = "category"
    }
}

/** "See all" screen for one home section, with infinite scrolling. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(
    onMovieClick: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: MovieListViewModel = hiltViewModel(),
) {
    val movies = viewModel.movies.collectAsLazyPagingItems()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(viewModel.category.titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        PagedMovieGrid(
            movies = movies,
            onMovieClick = onMovieClick,
            emptyMessage = stringResource(R.string.home_empty_list),
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        )
    }
}
