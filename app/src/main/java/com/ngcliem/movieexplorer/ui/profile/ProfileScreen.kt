package com.ngcliem.movieexplorer.ui.profile

import com.ngcliem.movieexplorer.ui.components.AppDialog
import com.ngcliem.movieexplorer.ui.components.DialogPrimaryButton
import com.ngcliem.movieexplorer.ui.components.DialogSecondaryButton
import com.ngcliem.movieexplorer.ui.components.DialogTone
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.data.settings.AppLanguage
import com.ngcliem.movieexplorer.ui.components.displayName
import com.ngcliem.movieexplorer.ui.components.monthLabel
import com.ngcliem.movieexplorer.ui.components.rememberLanguageChanger
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.RadioButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ngcliem.movieexplorer.domain.model.BookingStats
import com.ngcliem.movieexplorer.domain.model.MemberTier
import com.ngcliem.movieexplorer.ui.components.LightStatusBarIcons
import com.ngcliem.movieexplorer.ui.components.LoadingState
import com.ngcliem.movieexplorer.ui.components.formatEpochMillis
import com.ngcliem.movieexplorer.ui.components.formatNumber
import com.ngcliem.movieexplorer.ui.components.formatVnd

@Composable
fun ProfileScreen(
    onMyTickets: () -> Unit,
    onSpecialTickets: () -> Unit,
    onMembership: () -> Unit,
    onChangePassword: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var pickLanguage by rememberSaveable { mutableStateOf(false) }
    val language by viewModel.language.collectAsStateWithLifecycle()
    val changeLanguage = rememberLanguageChanger(viewModel::setLanguage)
    val user = state.user

    if (state.isLoading || user == null) {
        LoadingState()
        return
    }
    val stats = state.stats

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ProfileHeader(
            name = user.fullName,
            email = user.email,
            initials = user.initials,
            memberSince = formatEpochMillis(user.createdAt),
            tier = stats.tier,
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TierProgressCard(stats = stats, onClick = onMembership)

            SectionTitle(stringResource(R.string.journey_title))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Filled.ConfirmationNumber, stats.totalTickets, stringResource(R.string.stat_tickets), Color(0xFFE50914), Modifier.weight(1f))
                StatCard(Icons.Filled.Movie, stats.moviesWatched, stringResource(R.string.stat_watched), Color(0xFF1E88E5), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Filled.AutoAwesome, stats.specialTickets, stringResource(R.string.stat_special), Color(0xFF8E24AA), Modifier.weight(1f))
                StatCard(Icons.Filled.Stars, stats.totalPoints, stringResource(R.string.stat_points), Color(0xFFFFA000), Modifier.weight(1f))
            }
            SpendingCard(stats = stats, memberSince = formatEpochMillis(user.createdAt))
            MonthlyChart(stats = stats)

            SectionTitle(stringResource(R.string.section_tools))
            Card(shape = RoundedCornerShape(16.dp)) {
                MenuItem(Icons.Filled.ConfirmationNumber, stringResource(R.string.menu_my_tickets), stringResource(R.string.menu_my_tickets_sub, stats.upcomingTickets, stats.totalBookings), onMyTickets)
                MenuItem(Icons.Filled.AutoAwesome, stringResource(R.string.menu_special), stringResource(R.string.menu_special_sub), onSpecialTickets)
                MenuItem(Icons.Filled.WorkspacePremium, stringResource(R.string.menu_membership), stringResource(R.string.menu_membership_sub, stats.tier.displayName(), formatNumber(stats.totalPoints)), onMembership)
            }

            SectionTitle(stringResource(R.string.section_settings))
            Card(shape = RoundedCornerShape(16.dp)) {
                MenuItem(
                    Icons.Filled.Lock,
                    stringResource(R.string.menu_change_password),
                    stringResource(R.string.menu_change_password_sub),
                    onChangePassword,
                    tint = Color(0xFF1E88E5),
                )
                MenuItem(
                    Icons.Filled.Language,
                    stringResource(R.string.menu_language),
                    language.nativeName,
                    { pickLanguage = true },
                    tint = Color(0xFF43A047),
                )
                MenuItem(
                    Icons.AutoMirrored.Filled.Logout,
                    stringResource(R.string.menu_logout),
                    null,
                    { confirmLogout = true },
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmLogout) {
        AppDialog(
            onDismissRequest = { confirmLogout = false },
            tone = DialogTone.DANGER,
            icon = Icons.AutoMirrored.Filled.Logout,
            title = stringResource(R.string.logout_title),
            confirmButton = {
                DialogPrimaryButton(stringResource(R.string.menu_logout), onClick = { confirmLogout = false; viewModel.logout(onLoggedOut) })
            },
            dismissButton = { DialogSecondaryButton(stringResource(R.string.action_cancel), onClick = { confirmLogout = false }) },
        ) {
            Text(
                stringResource(R.string.logout_message),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (pickLanguage) {
        AppDialog(
            onDismissRequest = { pickLanguage = false },
            tone = DialogTone.INFO,
            icon = Icons.Filled.Translate,
            title = stringResource(R.string.language_dialog_title),
            confirmButton = {
                DialogSecondaryButton(stringResource(R.string.action_close), onClick = { pickLanguage = false })
            },
        ) {
                Column {
                    AppLanguage.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    pickLanguage = false
                                    if (option != language) changeLanguage(option)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = option == language, onClick = null, modifier = Modifier.padding(12.dp))
                            Text(if (option == AppLanguage.VIETNAMESE) "🇻🇳  ${option.nativeName}" else "🇬🇧  ${option.nativeName}")
                        }
                    }
                }
        }
    }
}

@Composable
private fun ProfileHeader(name: String, email: String, initials: String, memberSince: String, tier: MemberTier) {
    LightStatusBarIcons()
    val shift by rememberInfiniteTransition(label = "header").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "shift",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFE50914), Color(0xFF6A1B9A), Color(0xFF1A237E)),
                    start = Offset(shift * 400f, 0f),
                    end = Offset(800f + shift * 400f, 900f),
                ),
            )
            .statusBarsPadding()
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .border(3.dp, Color(tier.colorHex), CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(email, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                TierBadge(tier)
                Text(stringResource(R.string.member_since, memberSince), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun TierBadge(tier: MemberTier) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(tier.colorHex))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = Color(0xFF1B1B2F), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(tier.displayName().uppercase(), color = Color(0xFF1B1B2F), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun TierProgressCard(stats: BookingStats, onClick: (() -> Unit)? = null) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(stats.tierProgress) {
        progress.animateTo(stats.tierProgress, tween(1_200, easing = FastOutSlowInEasing))
    }
    val tierColor = Color(stats.tier.colorHex)
    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B2F), disabledContainerColor = Color(0xFF1B1B2F)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.current_tier), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                    Text(stats.tier.displayName(), color = tierColor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatNumber(stats.totalPoints), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.points_unit), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.value)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(tierColor.copy(alpha = 0.6f), tierColor))),
                )
            }
            val next = stats.tier.next
            Text(
                text = if (next == null) {
                    stringResource(R.string.top_tier, stats.tier.discountPercent)
                } else {
                    stringResource(R.string.to_next_tier, formatNumber(stats.pointsToNextTier ?: 0), next.displayName(), next.discountPercent)
                },
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

/** Number that counts up from 0 when shown. */
@Composable
private fun CountUpText(value: Int, style: androidx.compose.ui.text.TextStyle, color: Color = Color.Unspecified) {
    val animated by animateFloatAsState(value.toFloat(), tween(1_000, easing = FastOutSlowInEasing), label = "countUp")
    Text(formatNumber(animated.toInt()), style = style, color = color, fontWeight = FontWeight.Bold)
}

@Composable
private fun StatCard(icon: ImageVector, value: Int, label: String, color: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            CountUpText(value, MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SpendingCard(stats: BookingStats, memberSince: String) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x2243A047)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Payments, contentDescription = null, tint = Color(0xFF43A047))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.total_spent_since, memberSince), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatVnd(stats.totalSpent), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = stats.firstBookingAt?.let { stringResource(R.string.bookings_summary, stats.totalBookings, formatEpochMillis(it)) }
                        ?: stringResource(R.string.no_bookings_yet),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Animated bar chart: tickets booked in each of the last 6 months. */
@Composable
private fun MonthlyChart(stats: BookingStats) {
    if (stats.monthly.isEmpty()) return
    val max = (stats.monthly.maxOf { it.second }).coerceAtLeast(1)
    val grow = remember { Animatable(0f) }
    LaunchedEffect(stats.monthly) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(1_000, easing = FastOutSlowInEasing))
    }
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.chart_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                stats.monthly.forEachIndexed { index, (month, count) ->
                    val isCurrent = index == stats.monthly.lastIndex
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text("$count", style = MaterialTheme.typography.labelSmall)
                        Box(
                            Modifier
                                .width(22.dp)
                                .height((100 * grow.value * count / max).dp.coerceAtLeast(3.dp))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (isCurrent) {
                                        Brush.verticalGradient(listOf(Color(0xFFFF6A00), Color(0xFFE50914)))
                                    } else {
                                        Brush.verticalGradient(listOf(Color(0xFF7986CB), Color(0xFF3949AB)))
                                    },
                                ),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(month.monthLabel(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
