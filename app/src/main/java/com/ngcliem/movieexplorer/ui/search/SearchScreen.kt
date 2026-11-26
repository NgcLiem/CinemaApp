package com.ngcliem.movieexplorer.ui.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.domain.model.Movie
import com.ngcliem.movieexplorer.ui.components.EmptyState
import com.ngcliem.movieexplorer.ui.components.PagedMovieGrid
import com.ngcliem.movieexplorer.ui.components.PosterImage
import com.ngcliem.movieexplorer.ui.components.RatingRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMovieClick: (Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val activeQuery by viewModel.activeQuery.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val trending by viewModel.trending.collectAsStateWithLifecycle()
    val results = viewModel.results.collectAsLazyPagingItems()
    val keyboard = LocalSoftwareKeyboardController.current

    val openResult: (Int) -> Unit = { id ->
        viewModel.onSubmit()
        onMovieClick(id)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.search_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = viewModel::onClearQuery) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    viewModel.onSubmit()
                    keyboard?.hide()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            val bottomPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
            AnimatedContent(
                targetState = query.isBlank() || activeQuery.isBlank(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "searchMode",
            ) { showSuggestions ->
                if (showSuggestions) {
                    Suggestions(
                        history = history,
                        trending = trending,
                        contentPadding = bottomPadding,
                        onHistoryClick = viewModel::onHistoryClick,
                        onRemoveHistory = viewModel::onRemoveHistory,
                        onClearHistory = viewModel::onClearHistory,
                        onMovieClick = onMovieClick,
                    )
                } else {
                    Column {
                        Text(
                            text = stringResource(R.string.search_results_for, activeQuery),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        PagedMovieGrid(
                            movies = results,
                            onMovieClick = openResult,
                            emptyMessage = stringResource(R.string.search_no_results, activeQuery),
                            contentPadding = bottomPadding,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun Suggestions(
    history: List<String>,
    trending: List<Movie>,
    contentPadding: PaddingValues,
    onHistoryClick: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onMovieClick: (Int) -> Unit,
) {
    if (history.isEmpty() && trending.isEmpty()) {
        EmptyState(
            message = stringResource(R.string.search_empty_prompt),
            icon = Icons.Filled.Search,
            modifier = Modifier.padding(contentPadding),
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (history.isNotEmpty()) {
            item(key = "historyHeader") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.search_recent),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onClearHistory) { Text(stringResource(R.string.action_clear_all)) }
                }
            }
            item(key = "historyChips") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.forEach { item ->
                        InputChip(
                            selected = false,
                            onClick = { onHistoryClick(item) },
                            label = { Text(item, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.remove_item),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onRemoveHistory(item) },
                                )
                            },
                        )
                    }
                }
            }
        }
        if (trending.isNotEmpty()) {
            item(key = "trendingHeader") {
                Text(
                    stringResource(R.string.search_trending),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            itemsIndexed(trending, key = { _, movie -> movie.id }) { index, movie ->
                TrendingRow(rank = index + 1, movie = movie, onClick = { onMovieClick(movie.id) })
            }
        }
    }
}

@Composable
private fun TrendingRow(rank: Int, movie: Movie, onClick: () -> Unit) {
    val rankBrush = when (rank) {
        1 -> Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8F00)))
        2 -> Brush.verticalGradient(listOf(Color(0xFFE0E0E0), Color(0xFF9E9E9E)))
        3 -> Brush.verticalGradient(listOf(Color(0xFFFFAB91), Color(0xFFBF360C)))
        else -> Brush.verticalGradient(listOf(Color(0xFF7986CB), Color(0xFF3949AB)))
    }
    Card(onClick = onClick, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$rank",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(rankBrush)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Spacer(Modifier.width(12.dp))
            PosterImage(
                url = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier
                    .width(48.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(movie.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                RatingRow(rating = movie.voteAverage, year = movie.releaseDate.take(4))
            }
        }
    }
}
