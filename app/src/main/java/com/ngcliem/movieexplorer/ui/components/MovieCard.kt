package com.ngcliem.movieexplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ngcliem.movieexplorer.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ngcliem.movieexplorer.domain.model.Movie
import com.ngcliem.movieexplorer.ui.theme.MovieExplorerTheme
import com.ngcliem.movieexplorer.ui.theme.RatingStar
import java.util.Locale

@Composable
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier) {
        Column {
            PosterImage(
                url = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
            )
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                RatingRow(rating = movie.voteAverage, year = movie.releaseDate.take(4))
            }
        }
    }
}

@Composable
fun PosterImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        // The grey background doubles as a placeholder while loading or when there is no image.
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
fun RatingRow(rating: Double, year: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = stringResource(R.string.rating_cd),
            tint = RatingStar,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = rating.formatRating(),
            style = MaterialTheme.typography.labelMedium,
        )
        if (year.isNotBlank()) {
            Text(
                text = "• $year",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun Double.formatRating(): String = String.format(Locale.US, "%.1f", this)

@Preview
@Composable
private fun MovieCardPreview() {
    MovieExplorerTheme(dynamicColor = false) {
        MovieCard(
            movie = Movie(
                id = 1,
                title = "A Very Long Movie Title That Wraps",
                overview = "",
                posterUrl = null,
                backdropUrl = null,
                releaseDate = "2024-05-01",
                voteAverage = 7.85,
            ),
            onClick = {},
        )
    }
}
