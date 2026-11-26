package com.ngcliem.movieexplorer.ui.favorites

import com.ngcliem.movieexplorer.ui.components.AppDialog
import com.ngcliem.movieexplorer.ui.components.DialogPrimaryButton
import com.ngcliem.movieexplorer.ui.components.DialogSecondaryButton
import com.ngcliem.movieexplorer.ui.components.DialogTone
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.domain.model.Movie
import com.ngcliem.movieexplorer.ui.components.EmptyState
import com.ngcliem.movieexplorer.ui.components.LoadingState
import com.ngcliem.movieexplorer.ui.components.MovieCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onMovieClick: (Int) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lastRemoved by viewModel.lastRemoved.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    val removedMessage = lastRemoved?.let { stringResource(R.string.removed_from_favorites, it.title) }
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(lastRemoved) {
        if (removedMessage == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(removedMessage, actionLabel = undoLabel, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove() else viewModel.removalShown()
    }

    val movies = (state as? FavoritesUiState.Success)?.movies.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.favorites_title))
                        if (movies.isNotEmpty()) {
                            Text(
                                stringResource(R.string.favorites_count, movies.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (movies.isNotEmpty()) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.action_clear_all))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        when (val current = state) {
            FavoritesUiState.Loading -> LoadingState(Modifier.padding(innerPadding))

            is FavoritesUiState.Success -> if (current.movies.isEmpty()) {
                EmptyState(
                    message = stringResource(R.string.favorites_empty),
                    icon = Icons.Outlined.FavoriteBorder,
                    modifier = Modifier.padding(innerPadding),
                )
            } else {
                Column(Modifier.padding(top = innerPadding.calculateTopPadding())) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FavoriteSort.entries.forEach { sort ->
                            FilterChip(
                                selected = sort == current.sort,
                                onClick = { viewModel.onSortChange(sort) },
                                label = {
                                    Text(
                                        stringResource(
                                            when (sort) {
                                                FavoriteSort.RECENT -> R.string.sort_recent
                                                FavoriteSort.RATING -> R.string.sort_rating
                                                FavoriteSort.TITLE -> R.string.sort_title
                                            },
                                        ),
                                    )
                                },
                            )
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(140.dp),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 8.dp,
                            bottom = innerPadding.calculateBottomPadding() + 12.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(current.movies, key = { it.id }) { movie ->
                            FavoriteCard(
                                movie = movie,
                                onClick = { onMovieClick(movie.id) },
                                onRemove = { viewModel.remove(movie) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AppDialog(
            onDismissRequest = { confirmClear = false },
            tone = DialogTone.DANGER,
            icon = Icons.Filled.DeleteSweep,
            title = stringResource(R.string.clear_favorites_title),
            confirmButton = {
                DialogPrimaryButton(stringResource(R.string.action_clear_all), onClick = { confirmClear = false; viewModel.clearAll() })
            },
            dismissButton = {
                DialogSecondaryButton(stringResource(R.string.action_cancel), onClick = { confirmClear = false })
            },
        ) {
            Text(
                stringResource(R.string.clear_favorites_message),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Movie card with a heart button to remove it from favorites. */
@Composable
private fun FavoriteCard(
    movie: Movie,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.6f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { if (pressed) onRemove() },
        label = "heartScale",
    )
    Box(modifier = modifier) {
        MovieCard(movie = movie, onClick = onClick)
        // A plain Box (not IconButton) so the round background matches the 36dp size exactly.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = !pressed, role = Role.Button) { pressed = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = stringResource(R.string.remove_favorite),
                tint = Color(0xFFFF4D67),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale },
            )
        }
    }
}
