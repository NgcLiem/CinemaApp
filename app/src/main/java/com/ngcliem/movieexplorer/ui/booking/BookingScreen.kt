package com.ngcliem.movieexplorer.ui.booking

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.ui.components.AppDialog
import com.ngcliem.movieexplorer.ui.components.DialogPrimaryButton
import com.ngcliem.movieexplorer.ui.components.DialogSecondaryButton
import com.ngcliem.movieexplorer.ui.components.DialogTone
import com.ngcliem.movieexplorer.ui.components.asString
import com.ngcliem.movieexplorer.ui.components.displayName
import com.ngcliem.movieexplorer.ui.components.joinLabels
import com.ngcliem.movieexplorer.ui.components.monthLabel
import com.ngcliem.movieexplorer.ui.components.weekdayLabel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ngcliem.movieexplorer.domain.model.Booking
import com.ngcliem.movieexplorer.domain.model.ScreenFormat
import com.ngcliem.movieexplorer.domain.model.SeatMap
import com.ngcliem.movieexplorer.domain.model.SeatType
import com.ngcliem.movieexplorer.ui.components.ErrorState
import com.ngcliem.movieexplorer.ui.components.LoadingState
import com.ngcliem.movieexplorer.ui.components.PosterImage
import com.ngcliem.movieexplorer.ui.components.formatFull
import com.ngcliem.movieexplorer.ui.components.formatVnd

private val SeatStandard = Color(0xFF5C6BC0)
private val SeatVip = Color(0xFFFFA000)
private val SeatSweetbox = Color(0xFFEC407A)
private val SeatSelected = Color(0xFF43A047)
private val SeatTaken = Color(0xFF616161)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    onBack: () -> Unit,
    onViewTickets: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirming by rememberSaveable { mutableStateOf(false) }

    val message = state.message?.asString()
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.booking_title), fontWeight = FontWeight.Bold)
                        state.movie?.let {
                            Text(it.title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (state.movie != null) {
                SummaryBar(state = state, onContinue = { confirming = true })
            }
        },
    ) { innerPadding ->
        when {
            state.movie != null -> BookingContent(state, viewModel, Modifier.padding(innerPadding))
            state.isLoading -> LoadingState(Modifier.padding(innerPadding))
            else -> ErrorState(
                message = state.errorMessage?.asString() ?: stringResource(R.string.error_movie_load),
                onRetry = viewModel::loadMovie,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (confirming && state.completedBooking == null && state.payment == null) {
        ConfirmDialog(
            state = state,
            onDismiss = { confirming = false },
            onConfirm = {
                confirming = false
                viewModel.startQrPayment()
            },
        )
    }

    state.payment?.let { payment ->
        QrPaymentDialog(
            payment = payment,
            isVerifying = state.isVerifyingPayment,
            onPaid = viewModel::confirmPaid,
            onRenew = viewModel::startQrPayment,
            onCancel = viewModel::cancelPayment,
        )
    }

    state.completedBooking?.let { booking ->
        SuccessDialog(booking = booking, onViewTickets = onViewTickets, onClose = onBack)
    }
}

@Composable
private fun BookingContent(state: BookingUiState, viewModel: BookingViewModel, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.isEarlyScreening) {
            Text(
                text = stringResource(R.string.booking_early_banner),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1E88E5), Color(0xFF8E24AA))))
                    .padding(12.dp),
            )
        }

        StepTitle(stringResource(R.string.step_cinema))
        ChipRow {
            SeatMap.cinemas.forEach { cinema ->
                FilterChip(
                    selected = cinema == state.cinema,
                    onClick = { viewModel.selectCinema(cinema) },
                    label = { Text(cinema) },
                )
            }
        }

        StepTitle(stringResource(R.string.step_date))
        ChipRow {
            state.dates.forEach { date ->
                val selected = date == state.date
                val bg by animateColorAsState(
                    if (selected) Color(0xFFE50914) else MaterialTheme.colorScheme.surfaceVariant,
                    label = "dateBg",
                )
                Column(
                    modifier = Modifier
                        .width(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(bg)
                        .clickable { viewModel.selectDate(date) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    Text(date.weekdayLabel(), color = color, style = MaterialTheme.typography.labelMedium)
                    Text("${date.dayOfMonth}", color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(date.monthLabel(), color = color, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        StepTitle(stringResource(R.string.step_time))
        if (state.availableTimes.isEmpty()) {
            Text(
                stringResource(R.string.no_showtimes),
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ChipRow {
                state.availableTimes.forEach { time ->
                    FilterChip(
                        selected = time == state.time,
                        onClick = { viewModel.selectTime(time) },
                        label = { Text(time, fontWeight = FontWeight.Bold) },
                    )
                }
            }
        }

        StepTitle(stringResource(R.string.step_format))
        ChipRow {
            ScreenFormat.entries.forEach { format ->
                FilterChip(
                    selected = format == state.format,
                    onClick = { viewModel.selectFormat(format) },
                    label = {
                        Text(if (format.surcharge > 0) "${format.label} (+${formatVnd(format.surcharge)})" else format.label)
                    },
                )
            }
        }

        if (state.time != null) {
            StepTitle(stringResource(R.string.step_seats))
            SeatPicker(
                taken = state.takenSeats,
                selected = state.selectedSeats,
                onSeatClick = viewModel::toggleSeat,
            )
            SeatLegend()
        }
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun SeatPicker(taken: Set<String>, selected: List<String>, onSeatClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Curved glowing screen.
        val screenColor = MaterialTheme.colorScheme.primary
        Canvas(
            Modifier
                .fillMaxWidth(0.85f)
                .height(28.dp),
        ) {
            val path = Path().apply {
                moveTo(0f, size.height)
                quadraticBezierTo(size.width / 2, -size.height * 0.6f, size.width, size.height)
            }
            drawPath(path, screenColor.copy(alpha = 0.25f), style = Stroke(width = 18f))
            drawPath(path, screenColor, style = Stroke(width = 5f))
        }
        Text(stringResource(R.string.screen_label), style = MaterialTheme.typography.labelSmall, letterSpacing = 4.sp)
        Spacer(Modifier.height(6.dp))

        SeatMap.rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$row", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(14.dp))
                for (column in 1..SeatMap.COLUMNS) {
                    val id = SeatMap.seatId(row, column)
                    Seat(
                        id = id,
                        isTaken = id in taken,
                        isSelected = id in selected,
                        onClick = { onSeatClick(id) },
                        modifier = Modifier.weight(1f),
                    )
                    // Aisle in the middle.
                    if (column == SeatMap.COLUMNS / 2) Spacer(Modifier.width(10.dp))
                }
            }
        }
    }
}

@Composable
private fun Seat(id: String, isTaken: Boolean, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val type = SeatMap.typeOf(id)
    val base = when (type) {
        SeatType.STANDARD -> SeatStandard
        SeatType.VIP -> SeatVip
        SeatType.SWEETBOX -> SeatSweetbox
    }
    val color by animateColorAsState(
        when {
            isTaken -> SeatTaken
            isSelected -> SeatSelected
            else -> base.copy(alpha = 0.22f)
        },
        label = "seatColor",
    )
    val scale by animateFloatAsState(
        if (isSelected) 1.12f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "seatScale",
    )
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
            .background(color)
            .border(1.dp, if (isTaken) Color.Transparent else base, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
            .clickable(enabled = !isTaken, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isTaken) "✕" else id.drop(1),
            fontSize = 9.sp,
            color = if (isTaken || isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SeatLegend() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendItem(SeatStandard.copy(alpha = 0.4f), stringResource(R.string.legend_price, stringResource(R.string.seat_standard), formatVnd(SeatType.STANDARD.price)))
            LegendItem(SeatVip.copy(alpha = 0.4f), stringResource(R.string.legend_price, stringResource(R.string.seat_vip), formatVnd(SeatType.VIP.price)))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendItem(SeatSweetbox.copy(alpha = 0.4f), stringResource(R.string.legend_sweetbox, formatVnd(SeatType.SWEETBOX.price)))
            LegendItem(SeatSelected, stringResource(R.string.legend_selected))
            LegendItem(SeatTaken, stringResource(R.string.legend_taken))
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SummaryBar(state: BookingUiState, onContinue: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 12.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (state.selectedSeats.isEmpty()) {
                        stringResource(R.string.no_seat_selected)
                    } else {
                        stringResource(R.string.seats_label, state.selectedSeats.joinToString(", "))
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                AnimatedContent(
                    targetState = state.total,
                    transitionSpec = {
                        (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                    },
                    label = "total",
                ) { total ->
                    Text(formatVnd(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                if (state.discount > 0) {
                    Text(
                        stringResource(R.string.discount_applied, formatVnd(state.discount), state.tier.displayName(), state.tier.discountPercent),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF43A047),
                    )
                }
            }
            Button(
                onClick = onContinue,
                enabled = state.canSubmit,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914), contentColor = Color.White),
                modifier = Modifier.height(48.dp),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.pay), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialog(state: BookingUiState, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AppDialog(
        onDismissRequest = onDismiss,
        tone = DialogTone.BRAND,
        icon = Icons.Filled.ConfirmationNumber,
        title = stringResource(R.string.confirm_booking_title),
        confirmButton = { DialogPrimaryButton(stringResource(R.string.pay_with_qr), onClick = onConfirm) },
        dismissButton = { DialogSecondaryButton(stringResource(R.string.action_cancel), onClick = onDismiss) },
    ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SummaryLine(stringResource(R.string.label_movie), state.movie?.title.orEmpty())
                SummaryLine(stringResource(R.string.label_cinema), state.cinema)
                SummaryLine(stringResource(R.string.label_showtime), "${state.time} • ${state.date.formatFull()}")
                SummaryLine(stringResource(R.string.label_format), state.format.label)
                SummaryLine(stringResource(R.string.label_seats), state.selectedSeats.joinToString(", "))
                if (state.specialTags.isNotEmpty()) SummaryLine(stringResource(R.string.label_special), state.specialTags.joinLabels())
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SummaryLine(stringResource(R.string.label_subtotal), formatVnd(state.subtotal))
                if (state.discount > 0) SummaryLine(stringResource(R.string.label_tier_discount, state.tier.displayName()), "-${formatVnd(state.discount)}")
                SummaryLine(stringResource(R.string.label_total), formatVnd(state.total), bold = true)
                Text(
                    stringResource(R.string.points_will_earn, (state.total / 1000).toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = DialogTone.GOLD.accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false) {
    Row {
        Text(label, modifier = Modifier.width(96.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SuccessDialog(booking: Booking, onViewTickets: () -> Unit, onClose: () -> Unit) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow))
    }
    AppDialog(
        onDismissRequest = {},
        dismissible = false,
        tone = DialogTone.SUCCESS,
        iconContent = {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = DialogTone.SUCCESS.accent,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer { scaleX = pop.value; scaleY = pop.value },
            )
        },
        title = stringResource(R.string.booking_success),
        confirmButton = { DialogPrimaryButton(stringResource(R.string.view_my_tickets), onClick = onViewTickets) },
        dismissButton = { DialogSecondaryButton(stringResource(R.string.action_close), onClick = onClose) },
    ) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PosterImage(
                    url = booking.posterUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .width(80.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Text(booking.movieTitle, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(stringResource(R.string.ticket_code, booking.code), style = MaterialTheme.typography.titleMedium, color = DialogTone.SUCCESS.accent, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.booking_summary_line, booking.showTime, booking.showDate.formatFull(), booking.seats.joinToString(", ")), textAlign = TextAlign.Center)
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(DialogTone.GOLD.accent.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(stringResource(R.string.points_earned, booking.points), color = DialogTone.GOLD.accent, fontWeight = FontWeight.Bold)
                }
            }
    }
}
