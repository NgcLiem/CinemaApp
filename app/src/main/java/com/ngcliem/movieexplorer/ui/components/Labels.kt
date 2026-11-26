package com.ngcliem.movieexplorer.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.domain.model.MemberTier
import com.ngcliem.movieexplorer.domain.model.MovieCategory
import com.ngcliem.movieexplorer.domain.model.SeatType
import com.ngcliem.movieexplorer.domain.model.SpecialTag
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Display names for domain values. The domain layer has no text, so every
// label here follows the language chosen in the app.

@get:StringRes
val MovieCategory.titleRes: Int
    get() = when (this) {
        MovieCategory.NOW_PLAYING -> R.string.category_now_playing
        MovieCategory.UPCOMING -> R.string.category_upcoming
        MovieCategory.POPULAR -> R.string.category_popular
    }

@get:StringRes
val MemberTier.nameRes: Int
    get() = when (this) {
        MemberTier.MEMBER -> R.string.tier_member
        MemberTier.SILVER -> R.string.tier_silver
        MemberTier.GOLD -> R.string.tier_gold
        MemberTier.DIAMOND -> R.string.tier_diamond
    }

val MemberTier.benefitRes: List<Int>
    get() = when (this) {
        MemberTier.MEMBER -> listOf(R.string.benefit_member_1, R.string.benefit_member_2)
        MemberTier.SILVER -> listOf(R.string.benefit_silver_1, R.string.benefit_silver_2, R.string.benefit_silver_3)
        MemberTier.GOLD -> listOf(R.string.benefit_gold_1, R.string.benefit_gold_2, R.string.benefit_gold_3)
        MemberTier.DIAMOND -> listOf(R.string.benefit_diamond_1, R.string.benefit_diamond_2, R.string.benefit_diamond_3)
    }

@get:StringRes
val SpecialTag.labelRes: Int
    get() = when (this) {
        SpecialTag.EARLY_SCREENING -> R.string.tag_early
        SpecialTag.IMAX -> R.string.tag_imax
        SpecialTag.VIP_SEAT -> R.string.tag_vip
        SpecialTag.SWEETBOX -> R.string.tag_sweetbox
    }

@get:StringRes
val SeatType.labelRes: Int
    get() = when (this) {
        SeatType.STANDARD -> R.string.seat_standard
        SeatType.VIP -> R.string.seat_vip
        SeatType.SWEETBOX -> R.string.seat_sweetbox
    }

@Composable
fun MemberTier.displayName(): String = stringResource(nameRes)

@Composable
fun List<SpecialTag>.joinLabels(): String = map { stringResource(it.labelRes) }.joinToString(", ")

@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

@Composable
private fun isVietnamese(): Boolean = currentLocale().language == "vi"

/** "T2".."CN" in Vietnamese, "Mon".."Sun" in English. */
@Composable
fun LocalDate.weekdayLabel(): String =
    if (isVietnamese()) {
        if (dayOfWeek.value == 7) "CN" else "T${dayOfWeek.value + 1}"
    } else {
        dayOfWeek.getDisplayName(TextStyle.SHORT, currentLocale())
    }

/** "Th11" in Vietnamese, "Nov" in English. */
@Composable
fun LocalDate.monthLabel(): String = YearMonth.from(this).monthLabel()

@Composable
fun YearMonth.monthLabel(): String =
    if (isVietnamese()) {
        "T$monthValue"
    } else {
        format(DateTimeFormatter.ofPattern("MMM", currentLocale()))
    }
