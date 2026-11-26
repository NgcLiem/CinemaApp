package com.ngcliem.movieexplorer.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.ui.components.asString
import com.ngcliem.movieexplorer.ui.components.titleRes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ngcliem.movieexplorer.domain.model.Movie
import com.ngcliem.movieexplorer.domain.model.MovieCategory
import com.ngcliem.movieexplorer.ui.components.HeroBanner
import com.ngcliem.movieexplorer.ui.components.PosterImage
import com.ngcliem.movieexplorer.ui.components.PromoBannerCarousel
import com.ngcliem.movieexplorer.ui.components.RatingRow
import com.ngcliem.movieexplorer.ui.components.formatApiDate
import com.ngcliem.movieexplorer.ui.components.shimmer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMovieClick: (Int) -> Unit,
    onBookClick: (Int) -> Unit,
    onSeeAll: (MovieCategory) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF7B0D1E), Color(0xFF3A0A2E)),
                    ),
                ),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Movie,
                            contentDescription = null,
                            tint = Color(0xFFFFC94D),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Meil Cinema", fontWeight = FontWeight.ExtraBold)
                            user?.let {
                                Text(
                                    text = stringResource(R.string.home_greeting, it.fullName.substringAfterLast(' ')),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.75f),
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "hero") {
                    val nowPlaying = state.section(MovieCategory.NOW_PLAYING)
                    AnimatedContent(
                        targetState = nowPlaying.movies.isEmpty(),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "hero",
                    ) { empty ->
                        if (empty) {
                            Box(
                                Modifier
                                    .padding(horizontal = 20.dp)
                                    .fillMaxWidth()
                                    .height(230.dp)
                                    .shimmer(RoundedCornerShape(20.dp)),
                            )
                        } else {
                            HeroBanner(
                                movies = nowPlaying.movies.filter { it.backdropUrl != null }.take(6)
                                    .ifEmpty { nowPlaying.movies.take(6) },
                                onMovieClick = onMovieClick,
                                onBookClick = onBookClick,
                            )
                        }
                    }
                }

                MovieCategory.entries.forEachIndexed { index, category ->
                    item(key = category.name) {
                        MovieSection(
                            category = category,
                            state = state.section(category),
                            onMovieClick = onMovieClick,
                            onSeeAll = { onSeeAll(category) },
                            onRetry = { viewModel.loadSection(category) },
                        )
                    }
                    // Promotions between "now playing" and "upcoming".
                    if (index == 0) {
                        item(key = "promo") { PromoBannerCarousel() }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieSection(
    category: MovieCategory,
    state: SectionState,
    onMovieClick: (Int) -> Unit,
    onSeeAll: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(width = 4.dp, height = 20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE50914)),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(category.titleRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSeeAll) {
                Text(stringResource(R.string.action_see_all))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }

        AnimatedContent(
            targetState = when {
                state.movies.isNotEmpty() -> 2
                state.errorMessage != null -> 1
                else -> 0
            },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "section-${category.name}",
        ) { mode ->
            when (mode) {
                0 -> LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false,
                ) {
                    items(4) {
                        Box(
                            Modifier
                                .width(130.dp)
                                .aspectRatio(2f / 3f)
                                .shimmer(),
                        )
                    }
                }

                1 -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = state.errorMessage?.asString().orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                }

                else -> LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.movies, key = { it.id }) { movie ->
                        SectionMovieCard(
                            movie = movie,
                            showReleaseDate = category == MovieCategory.UPCOMING,
                            onClick = { onMovieClick(movie.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionMovieCard(
    movie: Movie,
    showReleaseDate: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(130.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Box {
            PosterImage(
                url = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
            )
            if (showReleaseDate && movie.releaseDate.isNotBlank()) {
                Text(
                    text = formatApiDate(movie.releaseDate),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC1565C0))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            RatingRow(rating = movie.voteAverage, year = movie.releaseDate.take(4))
        }
    }
}
