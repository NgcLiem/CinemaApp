package com.ngcliem.movieexplorer.ui.membership

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.ui.components.benefitRes
import com.ngcliem.movieexplorer.ui.components.displayName
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ngcliem.movieexplorer.domain.model.MemberTier
import com.ngcliem.movieexplorer.ui.components.LoadingState
import com.ngcliem.movieexplorer.ui.components.formatEpochMillis
import com.ngcliem.movieexplorer.ui.components.formatNumber
import com.ngcliem.movieexplorer.ui.profile.AccountViewModel
import com.ngcliem.movieexplorer.ui.profile.TierProgressCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembershipScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.membership_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val user = state.user
        if (state.isLoading || user == null) {
            LoadingState(Modifier.padding(innerPadding))
            return@Scaffold
        }
        val stats = state.stats
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MemberCard(name = user.fullName, memberId = "MX" + user.id.take(6).uppercase(), tier = stats.tier)
            TierProgressCard(stats = stats)

            Text(stringResource(R.string.tiers_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            MemberTier.entries.forEach { tier ->
                TierRow(tier = tier, current = stats.tier, points = stats.totalPoints)
            }

            Text(stringResource(R.string.earn_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(R.string.earn_1, R.string.earn_2, R.string.earn_3, R.string.earn_4)
                        .forEach { Text(stringResource(it)) }
                }
            }

            if (state.bookings.isNotEmpty()) {
                Text(stringResource(R.string.points_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        state.bookings.take(10).forEach { booking ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(booking.movieTitle, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(
                                        formatEpochMillis(booking.bookedAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text("+${formatNumber(booking.points)}", color = Color(0xFF43A047), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Shiny member card with a moving light reflection. */
@Composable
private fun MemberCard(name: String, memberId: String, tier: MemberTier) {
    val shine by rememberInfiniteTransition(label = "cardShine").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(3_000, easing = LinearEasing)),
        label = "shine",
    )
    val tierColor = Color(tier.colorHex)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.7f)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1B1B2F), tierColor.copy(alpha = 0.85f), Color(0xFF1B1B2F))))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.25f), Color.Transparent),
                    start = Offset(shine * 1000f - 250f, 0f),
                    end = Offset(shine * 1000f + 250f, 600f),
                ),
            )
            .padding(20.dp),
    ) {
        Row(Modifier.align(Alignment.TopStart), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = tierColor, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Meil Cinema Membership", color = Color.White, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.tier_name, tier.displayName()), color = tierColor, fontWeight = FontWeight.ExtraBold)
            }
        }
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(memberId, color = Color.White.copy(alpha = 0.85f), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
            Text(name.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "-${tier.discountPercent}%",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
private fun TierRow(tier: MemberTier, current: MemberTier, points: Int) {
    val reached = points >= tier.minPoints
    val isCurrent = tier == current
    val color = Color(tier.colorHex)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isCurrent) Modifier.border(2.dp, color, RoundedCornerShape(16.dp)) else Modifier)
            .animateContentSize(),
    ) {
        Row(Modifier.padding(14.dp)) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (reached) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFF1B1B2F),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tier.displayName(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (isCurrent) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.your_tier),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1B1B2F),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(color)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    if (tier.minPoints == 0) stringResource(R.string.tier_default) else stringResource(R.string.tier_from, formatNumber(tier.minPoints)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                tier.benefitRes.forEach { Text("✓ " + stringResource(it), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
