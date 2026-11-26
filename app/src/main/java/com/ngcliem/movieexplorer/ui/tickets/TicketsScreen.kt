package com.ngcliem.movieexplorer.ui.tickets

import com.ngcliem.movieexplorer.ui.components.AppDialog
import com.ngcliem.movieexplorer.ui.components.DialogPrimaryButton
import com.ngcliem.movieexplorer.ui.components.DialogSecondaryButton
import com.ngcliem.movieexplorer.ui.components.DialogTone
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.ui.components.labelRes
import com.ngcliem.movieexplorer.ui.components.joinLabels
import com.ngcliem.movieexplorer.ui.components.weekdayLabel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ngcliem.movieexplorer.domain.model.Booking
import com.ngcliem.movieexplorer.domain.model.SeatMap
import com.ngcliem.movieexplorer.ui.components.EmptyState
import com.ngcliem.movieexplorer.ui.components.LoadingState
import com.ngcliem.movieexplorer.ui.components.PosterImage
import com.ngcliem.movieexplorer.ui.components.formatEpochMillis
import com.ngcliem.movieexplorer.ui.components.formatFull
import com.ngcliem.movieexplorer.ui.components.formatVnd
import com.ngcliem.movieexplorer.ui.profile.AccountViewModel

/** "My tickets", or only the special tickets when [specialOnly] is true. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(
    specialOnly: Boolean,
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var opened by remember { mutableStateOf<Booking?>(null) }

    val source = if (specialOnly) state.bookings.filter { it.isSpecial } else state.bookings
    val upcoming = source.filter { it.isUpcoming() }.sortedBy { it.showDateTime }
    val past = source.filterNot { it.isUpcoming() }.sortedByDescending { it.showDateTime }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(if (specialOnly) R.string.special_tickets_title else R.string.tickets_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                )
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_upcoming_count, upcoming.size)) })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tab_watched_count, past.size)) })
                }
            }
        },
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(innerPadding))
            return@Scaffold
        }
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(innerPadding),
            label = "ticketsTab",
        ) { selectedTab ->
            val list = if (selectedTab == 0) upcoming else past
            if (list.isEmpty()) {
                EmptyState(
                    message = stringResource(
                        when {
                            specialOnly -> R.string.empty_special
                            selectedTab == 0 -> R.string.empty_upcoming
                            else -> R.string.empty_watched
                        },
                    ),
                    icon = Icons.Outlined.ConfirmationNumber,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (specialOnly && selectedTab == 0) {
                        item { SpecialInfoBanner() }
                    }
                    items(list, key = { it.id }) { booking ->
                        TicketCard(
                            booking = booking,
                            isPast = selectedTab == 1,
                            onClick = { opened = booking },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    opened?.let { booking ->
        TicketDetailDialog(
            booking = booking,
            onDismiss = { opened = null },
            onMovieClick = {
                opened = null
                onMovieClick(booking.movieId)
            },
        )
    }
}

@Composable
private fun SpecialInfoBanner() {
    Text(
        text = stringResource(R.string.special_info),
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF8E24AA), Color(0xFFE50914))))
            .padding(12.dp),
    )
}

/** Rounded rectangle with two half-circle cut-outs, like a paper ticket. */
private class TicketShape(private val notchFromTop: Dp, private val notchRadius: Dp = 10.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { notchRadius.toPx() }
        val y = with(density) { notchFromTop.toPx() }
        val card = Path().apply {
            addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(with(density) { 16.dp.toPx() })))
        }
        val notches = Path().apply {
            addOval(Rect(Offset(0f, y), r))
            addOval(Rect(Offset(size.width, y), r))
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, card, notches))
    }
}

private val TicketTopHeight = 132.dp

@Composable
private fun TicketCard(booking: Booking, isPast: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = TicketShape(notchFromTop = TicketTopHeight)
    val background = if (booking.isSpecial) {
        Brush.linearGradient(listOf(Color(0xFF2A1240), Color(0xFF1B1B2F)))
    } else {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
    }
    val textColor = if (booking.isSpecial) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = textColor.copy(alpha = 0.7f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .height(TicketTopHeight)
                .padding(14.dp),
        ) {
            PosterImage(
                url = booking.posterUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(2f / 3f, matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    booking.movieTitle,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(booking.cinema, color = subColor, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text(
                    "${booking.showTime} • ${booking.showDate.weekdayLabel()}, ${booking.showDate.formatFull()}",
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(stringResource(R.string.ticket_seats_line, booking.seats.joinToString(", "), booking.format.label), color = subColor, style = MaterialTheme.typography.bodySmall)
                if (booking.isSpecial) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        booking.specialTags.take(3).forEach { TagChip(stringResource(it.labelRes)) }
                    }
                }
            }
        }
        // Perforation line between the two halves.
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 16.dp),
        ) {
            drawLine(
                color = subColor,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ticket_code_label), color = subColor, style = MaterialTheme.typography.labelSmall)
                Text(booking.code, color = textColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatVnd(booking.totalPrice), color = textColor, fontWeight = FontWeight.Bold)
                Text(
                    if (isPast) stringResource(R.string.ticket_used) else stringResource(R.string.points_plus, booking.points),
                    color = if (isPast) subColor else Color(0xFF66BB6A),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Text(
        text = text,
        color = Color(0xFF1B1B2F),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFFFC94D))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun TicketDetailDialog(booking: Booking, onDismiss: () -> Unit, onMovieClick: () -> Unit) {
    AppDialog(
        onDismissRequest = onDismiss,
        tone = DialogTone.BRAND,
        icon = Icons.Filled.ConfirmationNumber,
        title = booking.movieTitle,
        confirmButton = { DialogPrimaryButton(stringResource(R.string.action_close), onClick = onDismiss) },
        dismissButton = { DialogSecondaryButton(stringResource(R.string.view_movie), onClick = onMovieClick) },
    ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Barcode(
                    code = booking.code,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                )
                Text(booking.code, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(
                    stringResource(R.string.show_code_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                DetailLine(stringResource(R.string.label_cinema), booking.cinema)
                DetailLine(stringResource(R.string.label_showtime), "${booking.showTime} • ${booking.showDate.formatFull()}")
                DetailLine(stringResource(R.string.label_format), booking.format.label)
                DetailLine(stringResource(R.string.label_seats), booking.seats.map { "$it (${stringResource(SeatMap.typeOf(it).labelRes)})" }.joinToString(", "))
                if (booking.isSpecial) DetailLine(stringResource(R.string.label_special), booking.specialTags.joinLabels())
                DetailLine(stringResource(R.string.label_paid), formatVnd(booking.totalPrice))
                DetailLine(stringResource(R.string.label_points), "+${booking.points}")
                DetailLine(stringResource(R.string.label_booked_on), formatEpochMillis(booking.bookedAt))
            }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.width(90.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

/** Decorative barcode generated from the booking code (always the same for a code). */
@Composable
private fun Barcode(code: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .padding(8.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val bits = code.flatMap { char ->
                val v = char.code
                (0 until 7).map { (v shr it) and 1 == 1 }
            }
            val unit = size.width / bits.sumOf { if (it) 2.0 else 1.0 }.toFloat()
            var x = 0f
            bits.forEachIndexed { i, bit ->
                val w = if (bit) unit * 2f else unit
                if (i % 2 == 0) {
                    drawRect(Color.Black, topLeft = Offset(x, 0f), size = Size(w, size.height))
                }
                x += w
            }
        }
    }
}
