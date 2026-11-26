package com.ngcliem.movieexplorer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import com.ngcliem.movieexplorer.domain.model.Movie

private val GridMinCellWidth = 140.dp
private val GridSpacing = 12.dp

/**
 * Grid for a paged list (Home, Search). Handles the first load, errors,
 * empty results and "loading more" at the bottom.
 */
@Composable
fun PagedMovieGrid(
    movies: LazyPagingItems<Movie>,
    onMovieClick: (Int) -> Unit,
    emptyMessage: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val refresh = movies.loadState.refresh
    when {
        refresh is LoadState.Loading && movies.itemCount == 0 ->
            LoadingState(modifier.padding(contentPadding))

        refresh is LoadState.Error && movies.itemCount == 0 ->
            ErrorState(
                message = refresh.error.toUiText().asString(),
                onRetry = movies::retry,
                modifier = modifier.padding(contentPadding),
            )

        refresh is LoadState.NotLoading && movies.itemCount == 0 ->
            EmptyState(message = emptyMessage, modifier = modifier.padding(contentPadding))

        else -> MovieGridLayout(modifier = modifier, contentPadding = contentPadding) {
            // No item keys on purpose: TMDB pages can contain the same movie twice,
            // and duplicate keys would crash a lazy grid.
            items(
                count = movies.itemCount,
                contentType = movies.itemContentType { "movie" },
            ) { index ->
                movies[index]?.let { movie ->
                    MovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
                }
            }

            when (movies.loadState.append) {
                is LoadState.Loading -> fullWidthItem { AppendLoadingItem() }
                is LoadState.Error -> fullWidthItem { AppendErrorItem(onRetry = movies::retry) }
                is LoadState.NotLoading -> Unit
            }
        }
    }
}

/** Grid for a plain list (Favorites). */
@Composable
fun MovieGrid(
    movies: List<Movie>,
    onMovieClick: (Int) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    MovieGridLayout(modifier = modifier, contentPadding = contentPadding) {
        items(movies, key = { it.id }) { movie ->
            MovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
        }
    }
}

@Composable
private fun MovieGridLayout(
    modifier: Modifier,
    contentPadding: PaddingValues,
    content: LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(GridMinCellWidth),
        modifier = modifier,
        contentPadding = contentPadding.withExtra(GridSpacing),
        horizontalArrangement = Arrangement.spacedBy(GridSpacing),
        verticalArrangement = Arrangement.spacedBy(GridSpacing),
        content = content,
    )
}

private fun LazyGridScope.fullWidthItem(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

/** Adds the same extra padding on every side of the Scaffold padding. */
@Composable
private fun PaddingValues.withExtra(extra: Dp): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(direction) + extra,
        top = calculateTopPadding() + extra,
        end = calculateEndPadding(direction) + extra,
        bottom = calculateBottomPadding() + extra,
    )
}
