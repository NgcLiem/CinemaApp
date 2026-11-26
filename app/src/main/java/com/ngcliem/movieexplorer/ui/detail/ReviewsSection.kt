package com.ngcliem.movieexplorer.ui.detail

import com.ngcliem.movieexplorer.ui.components.AppDialog
import com.ngcliem.movieexplorer.ui.components.DialogPrimaryButton
import com.ngcliem.movieexplorer.ui.components.DialogSecondaryButton
import com.ngcliem.movieexplorer.ui.components.DialogTone
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.ui.components.asString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ngcliem.movieexplorer.domain.model.Review
import com.ngcliem.movieexplorer.ui.components.PosterImage
import com.ngcliem.movieexplorer.ui.components.formatApiDate
import com.ngcliem.movieexplorer.ui.components.formatRating
import com.ngcliem.movieexplorer.ui.theme.RatingStar

private const val COLLAPSED_REVIEW_COUNT = 3

@Composable
fun ReviewsSection(
    tmdbScore: Double,
    tmdbVotes: Int,
    state: ReviewsUiState,
    onWriteReview: (stars: Int, content: String) -> Unit,
    onDeleteMine: () -> Unit,
    onRetry: () -> Unit,
    onLoginRequired: () -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showAll by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.reviews_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        RatingSummaryCard(tmdbScore = tmdbScore, tmdbVotes = tmdbVotes, state = state)

        val mine = state.myReview
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if (state.isLoggedIn) showDialog = true else onLoginRequired() }) {
                Icon(Icons.Outlined.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(if (mine == null) R.string.write_review else R.string.edit_review))
            }
            if (mine != null) {
                TextButton(onClick = onDeleteMine) { Text(stringResource(R.string.action_delete)) }
            }
        }

        val reviews = state.allReviews
        when {
            reviews.isEmpty() && state.isLoadingTmdb -> Text(stringResource(R.string.reviews_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            reviews.isEmpty() && state.tmdbError != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.tmdbError.asString(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
            }
            reviews.isEmpty() -> Text(
                stringResource(R.string.reviews_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {
                val visible = if (showAll) reviews else reviews.take(COLLAPSED_REVIEW_COUNT)
                Column(
                    modifier = Modifier.animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    visible.forEach { ReviewCard(it) }
                }
                if (reviews.size > COLLAPSED_REVIEW_COUNT) {
                    TextButton(onClick = { showAll = !showAll }) {
                        Text(if (showAll) stringResource(R.string.see_less) else stringResource(R.string.reviews_show_all, reviews.size))
                    }
                }
            }
        }
    }

    if (showDialog) {
        WriteReviewDialog(
            initialStars = state.myReview?.rating?.div(2)?.toInt() ?: 0,
            initialContent = state.myReview?.content.orEmpty(),
            onDismiss = { showDialog = false },
            onSubmit = { stars, content ->
                onWriteReview(stars, content)
                showDialog = false
            },
        )
    }
}

@Composable
private fun RatingSummaryCard(tmdbScore: Double, tmdbVotes: Int, state: ReviewsUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tmdbScore.formatRating(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                StarBar(stars = tmdbScore / 2, size = 16)
                Text(stringResource(R.string.votes_tmdb, tmdbVotes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val histogram = state.starHistogram
                val total = histogram.sum().coerceAtLeast(1)
                for (star in 5 downTo 1) {
                    val target = histogram[star - 1].toFloat() / total
                    val fill by animateFloatAsState(target, tween(800), label = "bar$star")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$star", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(12.dp))
                        Icon(Icons.Filled.Star, null, tint = RatingStar, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Box(
                            Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(fill)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(RatingStar),
                            )
                        }
                    }
                }
                val avg = state.appAverageStars
                Text(
                    text = if (avg == null) {
                        stringResource(R.string.app_reviews_none)
                    } else {
                        stringResource(R.string.app_reviews_avg, "%.1f".format(avg), state.appReviews.size)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun StarBar(stars: Double, size: Int = 18) {
    Row {
        for (i in 1..5) {
            Icon(
                imageVector = if (stars >= i - 0.25) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = RatingStar,
                modifier = Modifier.size(size.dp),
            )
        }
    }
}

@Composable
private fun ReviewCard(review: Review) {
    var expanded by rememberSaveable(review.id) { mutableStateOf(false) }
    val author = review.author.ifBlank { stringResource(R.string.review_anonymous) }
    Card(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (review.isMine) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(author.take(1).uppercase(), fontWeight = FontWeight.Bold)
                    if (review.avatarUrl != null) {
                        PosterImage(url = review.avatarUrl, contentDescription = null, modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (review.isMine) stringResource(R.string.review_you, author) else author,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(
                            formatApiDate(review.createdAt).takeIf { it.isNotBlank() },
                            if (review.isFromApp) "Meil Cinema" else "TMDB",
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                review.rating?.let { StarBar(stars = it / 2, size = 14) }
            }
            if (review.content.isNotBlank()) {
                Text(
                    text = review.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WriteReviewDialog(
    initialStars: Int,
    initialContent: String,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit,
) {
    var stars by rememberSaveable { mutableIntStateOf(initialStars) }
    var content by rememberSaveable { mutableStateOf(initialContent) }
    val labels = listOf(
        R.string.star_0, R.string.star_1, R.string.star_2,
        R.string.star_3, R.string.star_4, R.string.star_5,
    ).map { stringResource(it) }

    AppDialog(
        onDismissRequest = onDismiss,
        tone = DialogTone.GOLD,
        icon = Icons.Filled.RateReview,
        title = stringResource(R.string.review_dialog_title),
        confirmButton = {
            DialogPrimaryButton(stringResource(R.string.action_send), onClick = { onSubmit(stars, content) }, enabled = stars > 0)
        },
        dismissButton = { DialogSecondaryButton(stringResource(R.string.action_cancel), onClick = onDismiss) },
    ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    for (i in 1..5) {
                        val scale by animateFloatAsState(if (i <= stars) 1.2f else 1f, label = "star$i")
                        IconButton(onClick = { stars = i }) {
                            Icon(
                                imageVector = if (i <= stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = stringResource(R.string.star_cd, i),
                                tint = RatingStar,
                                modifier = Modifier
                                    .size(32.dp)
                                    .graphicsLayer { scaleX = scale; scaleY = scale },
                            )
                        }
                    }
                }
                Text(labels[stars], style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 500) content = it },
                    label = { Text(stringResource(R.string.review_hint)) },
                    supportingText = { Text("${content.length}/500") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
    }
}

@Composable
fun GenreChips(genres: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        genres.take(3).forEach { AssistChip(onClick = {}, label = { Text(it) }) }
    }
}

/** Clickable text that toggles between a short preview and the full description. */
@Composable
fun ExpandableText(text: String, collapsedLines: Int = 4) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflows by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .animateContentSize()
            .clickable { expanded = !expanded },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
        )
        if (overflows || expanded) {
            Text(
                text = stringResource(if (expanded) R.string.see_less else R.string.see_more),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
