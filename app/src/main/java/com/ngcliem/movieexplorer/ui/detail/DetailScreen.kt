package com.ngcliem.movieexplorer.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.ui.components.asString
import com.ngcliem.movieexplorer.ui.components.formatVnd
import com.ngcliem.movieexplorer.domain.model.SeatType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ngcliem.movieexplorer.domain.model.MovieDetail
import com.ngcliem.movieexplorer.domain.model.ReleaseStatus
import com.ngcliem.movieexplorer.domain.model.releaseStatusOf
import com.ngcliem.movieexplorer.ui.components.ErrorState
import com.ngcliem.movieexplorer.ui.components.LiveBadge
import com.ngcliem.movieexplorer.ui.components.LoadingState
import com.ngcliem.movieexplorer.ui.components.PosterImage
import com.ngcliem.movieexplorer.ui.components.RatingRow
import com.ngcliem.movieexplorer.ui.components.formatApiDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onBookClick: (Int) -> Unit = {},
    onLoginRequired: () -> Unit = {},
    viewModel: DetailViewModel = hiltViewModel(),
    reviewsViewModel: ReviewsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reviews by reviewsViewModel.uiState.collectAsStateWithLifecycle()
    val movie = state.movie

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack, colors = translucentIconButton()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (movie != null) {
                        IconButton(onClick = viewModel::onToggleFavorite, colors = translucentIconButton()) {
                            Icon(
                                imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = stringResource(if (state.isFavorite) R.string.remove_favorite else R.string.add_favorite),
                                tint = if (state.isFavorite) Color(0xFFFF4D67) else Color.White,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (movie != null) BookingBar(movie = movie, onBookClick = { onBookClick(movie.id) })
        },
    ) { innerPadding ->
        when {
            movie != null -> DetailContent(
                movie = movie,
                bottomPadding = innerPadding.calculateBottomPadding(),
                reviews = reviews,
                onWriteReview = reviewsViewModel::submit,
                onDeleteReview = reviewsViewModel::deleteMine,
                onRetryReviews = reviewsViewModel::loadTmdbReviews,
                onLoginRequired = onLoginRequired,
            )
            state.isLoading -> LoadingState(Modifier.padding(innerPadding))
            else -> ErrorState(
                message = state.errorMessage?.asString() ?: stringResource(R.string.error_generic),
                onRetry = viewModel::loadMovie,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun translucentIconButton() = IconButtonDefaults.iconButtonColors(
    containerColor = Color.Black.copy(alpha = 0.35f),
    contentColor = Color.White,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    movie: MovieDetail,
    bottomPadding: androidx.compose.ui.unit.Dp,
    reviews: ReviewsUiState,
    onWriteReview: (Int, String) -> Unit,
    onDeleteReview: () -> Unit,
    onRetryReviews: () -> Unit,
    onLoginRequired: () -> Unit,
) {
    val status = releaseStatusOf(movie.releaseDate)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomPadding + 16.dp),
    ) {
        // Backdrop with a fade into the background color.
        Box {
            PosterImage(
                url = movie.backdropUrl ?: movie.posterUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.35f),
                            0.5f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.background,
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .offset(y = (-70).dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
                PosterImage(
                    url = movie.posterUrl,
                    contentDescription = movie.title,
                    modifier = Modifier
                        .width(120.dp)
                        .aspectRatio(2f / 3f)
                        .shadow(12.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    when (status) {
                        ReleaseStatus.NOW_SHOWING -> LiveBadge(stringResource(R.string.badge_now_showing))
                        ReleaseStatus.UPCOMING -> LiveBadge(stringResource(R.string.badge_upcoming), color = Color(0xFF42A5F5))
                        ReleaseStatus.RELEASED, ReleaseStatus.UNKNOWN -> Unit
                    }
                    Text(movie.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    RatingRow(rating = movie.voteAverage, year = movie.releaseDate.take(4))
                }
            }

            movie.tagline?.let {
                Text(
                    text = "“$it”",
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                movie.runtimeMinutes?.let {
                    InfoPill(icon = Icons.Outlined.Schedule, text = formatRuntime(it), modifier = Modifier.weight(1f))
                }
                if (movie.releaseDate.isNotBlank()) {
                    InfoPill(
                        icon = Icons.Outlined.CalendarMonth,
                        text = stringResource(R.string.release_on, formatApiDate(movie.releaseDate)),
                        modifier = Modifier.weight(1.6f),
                    )
                }
            }

            if (movie.genres.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    movie.genres.forEach { genre ->
                        SuggestionChip(onClick = {}, label = { Text(genre) })
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.synopsis), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                ExpandableText(text = movie.overview.ifBlank { stringResource(R.string.no_synopsis) })
            }

            ReviewsSection(
                tmdbScore = movie.voteAverage,
                tmdbVotes = movie.voteCount,
                state = reviews,
                onWriteReview = onWriteReview,
                onDeleteMine = onDeleteReview,
                onRetry = onRetryReviews,
                onLoginRequired = onLoginRequired,
            )
        }
    }
}

@Composable
private fun InfoPill(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun BookingBar(movie: MovieDetail, onBookClick: () -> Unit) {
    val upcoming = releaseStatusOf(movie.releaseDate) == ReleaseStatus.UPCOMING
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (upcoming) R.string.booking_bar_early else R.string.booking_bar_open),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (upcoming) {
                        stringResource(R.string.booking_bar_early_sub, formatApiDate(movie.releaseDate))
                    } else {
                        stringResource(R.string.booking_bar_price, formatVnd(SeatType.STANDARD.price))
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onBookClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (upcoming) Color(0xFF1E88E5) else Color(0xFFE50914),
                    contentColor = Color.White,
                ),
                modifier = Modifier.height(48.dp),
            ) {
                Icon(Icons.Filled.ConfirmationNumber, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (upcoming) R.string.book_early else R.string.book_ticket), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 135 -> "2h 15m" */
internal fun formatRuntime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours == 0 -> "${mins}m"
        mins == 0 -> "${hours}h"
        else -> "${hours}h ${mins}m"
    }
}
