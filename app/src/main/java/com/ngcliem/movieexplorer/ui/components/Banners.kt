package com.ngcliem.movieexplorer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.ngcliem.movieexplorer.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ngcliem.movieexplorer.domain.model.Movie
import com.ngcliem.movieexplorer.ui.theme.RatingStar
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

// ---------------------------------------------------------------------------
// Shimmer placeholder
// ---------------------------------------------------------------------------

/** Animated loading placeholder: a light band sweeping across the surface. */
fun Modifier.shimmer(shape: RoundedCornerShape = RoundedCornerShape(12.dp)): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1_300, easing = LinearEasing)),
        label = "shimmerProgress",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    this
        .clip(shape)
        .background(base)
        .background(
            Brush.linearGradient(
                colors = listOf(Color.Transparent, highlight, Color.Transparent),
                start = Offset(progress * 1000f - 300f, 0f),
                end = Offset(progress * 1000f + 300f, 300f),
            ),
        )
}

// ---------------------------------------------------------------------------
// Pager helpers
// ---------------------------------------------------------------------------

/** Huge page count so the carousel can loop forever in both directions. */
private const val LOOP_PAGE_COUNT = 10_000

private fun loopStartPage(size: Int): Int =
    if (size <= 1) 0 else LOOP_PAGE_COUNT / 2 - (LOOP_PAGE_COUNT / 2) % size

/** Advances the pager every [intervalMillis], pausing while the user drags it. */
@Composable
private fun AutoScroll(pagerState: PagerState, enabled: Boolean, intervalMillis: Long) {
    val dragged by pagerState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(pagerState.settledPage, dragged, enabled) {
        if (!enabled || dragged) return@LaunchedEffect
        delay(intervalMillis)
        pagerState.animateScrollToPage(
            page = pagerState.currentPage + 1,
            animationSpec = tween(900, easing = FastOutSlowInEasing),
        )
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

private fun PagerState.offsetFor(page: Int): Float =
    ((currentPage - page) + currentPageOffsetFraction)

@Composable
fun PagerDots(count: Int, selected: Int, modifier: Modifier = Modifier, activeColor: Color = Color.White) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val isSelected = index == selected
            val width by animateDpAsState(if (isSelected) 22.dp else 7.dp, label = "dotWidth")
            Box(
                modifier = Modifier
                    .height(7.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(if (isSelected) activeColor else activeColor.copy(alpha = 0.4f)),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Hero banner: backdrops of "now playing" movies
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroBanner(
    movies: List<Movie>,
    onMovieClick: (Int) -> Unit,
    onBookClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (movies.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = loopStartPage(movies.size)) {
        if (movies.size > 1) LOOP_PAGE_COUNT else 1
    }
    AutoScroll(pagerState, enabled = movies.size > 1, intervalMillis = 4_500)

    // Slow "Ken Burns" zoom on the backdrop.
    val zoom by rememberInfiniteTransition(label = "kenBurns").animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "zoom",
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
        ) { page ->
            val movie = movies[page % movies.size]
            val offset = pagerState.offsetFor(page)
            val fraction = 1f - offset.absoluteValue.coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleY = lerp(0.86f, 1f, fraction)
                        alpha = lerp(0.55f, 1f, fraction)
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onMovieClick(movie.id) },
            ) {
                PosterImage(
                    url = movie.backdropUrl ?: movie.posterUrl,
                    contentDescription = movie.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Parallax: the picture moves slower than the card.
                            translationX = offset * size.width * 0.35f
                            val scale = if (page == pagerState.currentPage) zoom else 1.05f
                            scaleX = scale
                            scaleY = scale
                        },
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.45f to Color.Black.copy(alpha = 0.15f),
                                1f to Color.Black.copy(alpha = 0.88f),
                            ),
                        ),
                )
                LiveBadge(
                    text = stringResource(R.string.badge_now_showing),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                )
                BannerCaption(
                    visible = pagerState.settledPage == page,
                    movie = movie,
                    onBookClick = onBookClick,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        PagerDots(
            count = movies.size,
            selected = pagerState.currentPage % movies.size,
            activeColor = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun BannerCaption(
    visible: Boolean,
    movie: Movie,
    onBookClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 },
        exit = fadeOut(tween(200)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = RatingStar, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${movie.voteAverage.formatRating()}/10",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Button(
                onClick = { onBookClick(movie.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE50914),
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Filled.ConfirmationNumber, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.book_ticket))
            }
        }
    }
}

/** Small red badge with a pulsing dot. */
@Composable
fun LiveBadge(text: String, modifier: Modifier = Modifier, color: Color = Color(0xFFE50914)) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .graphicsLayer { alpha = pulse }
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------------------
// Promotion banners
// ---------------------------------------------------------------------------

data class Promotion(
    @androidx.annotation.StringRes val title: Int,
    @androidx.annotation.StringRes val subtitle: Int,
    val icon: ImageVector,
    val colors: List<Color>,
)

val defaultPromotions = listOf(
    Promotion(
        title = R.string.promo1_title,
        subtitle = R.string.promo1_subtitle,
        icon = Icons.Filled.LocalActivity,
        colors = listOf(Color(0xFFFF512F), Color(0xFFDD2476), Color(0xFF8E2DE2)),
    ),
    Promotion(
        title = R.string.promo2_title,
        subtitle = R.string.promo2_subtitle,
        icon = Icons.Filled.Fastfood,
        colors = listOf(Color(0xFFF7971E), Color(0xFFFFD200), Color(0xFFFF6A00)),
    ),
    Promotion(
        title = R.string.promo3_title,
        subtitle = R.string.promo3_subtitle,
        icon = Icons.Filled.WorkspacePremium,
        colors = listOf(Color(0xFF1D976C), Color(0xFF00C9FF), Color(0xFF3A1C71)),
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PromoBannerCarousel(
    modifier: Modifier = Modifier,
    promotions: List<Promotion> = defaultPromotions,
    onPromotionClick: (Promotion) -> Unit = {},
) {
    val pagerState = rememberPagerState(initialPage = loopStartPage(promotions.size)) { LOOP_PAGE_COUNT }
    AutoScroll(pagerState, enabled = true, intervalMillis = 3_500)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val promo = promotions[page % promotions.size]
            val fraction = 1f - pagerState.offsetFor(page).absoluteValue.coerceIn(0f, 1f)
            PromoBanner(
                promotion = promo,
                modifier = Modifier
                    .graphicsLayer {
                        val scale = lerp(0.9f, 1f, fraction)
                        scaleX = scale
                        scaleY = scale
                        rotationY = pagerState.offsetFor(page) * -12f
                    }
                    .clickable { onPromotionClick(promo) },
            )
        }
        Spacer(Modifier.height(8.dp))
        PagerDots(
            count = promotions.size,
            selected = pagerState.currentPage % promotions.size,
            activeColor = MaterialTheme.colorScheme.primary,
        )
    }
}

/** A promotion card with a flowing gradient, a light sweep and a floating icon. */
@Composable
fun PromoBanner(promotion: Promotion, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "promo")
    val flow by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "gradientFlow",
    )
    val sweep by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2_600, delayMillis = 600, easing = FastOutSlowInEasing)),
        label = "lightSweep",
    )
    val float by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "iconFloat",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = promotion.colors,
                    start = Offset(flow * 600f, 0f),
                    end = Offset(600f + flow * 600f, 400f),
                ),
            ),
    ) {
        // Shiny light sweeping across the card.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent),
                        start = Offset(sweep * 1200f - 200f, 0f),
                        end = Offset(sweep * 1200f + 100f, 400f),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(promotion.title),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = stringResource(promotion.subtitle),
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                imageVector = promotion.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        translationY = float.dp.toPx()
                        rotationZ = float * 1.5f
                    },
            )
        }
    }
}
