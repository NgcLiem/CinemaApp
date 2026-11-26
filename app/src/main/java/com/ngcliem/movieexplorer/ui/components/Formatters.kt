package com.ngcliem.movieexplorer.ui.components

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val vietnamese = Locale("vi", "VN")
private val dayMonthYear = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val dayMonth = DateTimeFormatter.ofPattern("dd/MM")

/** 150000 -> "150.000đ" */
fun formatVnd(amount: Long): String = NumberFormat.getInstance(vietnamese).format(amount) + "đ"

fun formatNumber(value: Int): String = NumberFormat.getInstance(vietnamese).format(value)

fun LocalDate.formatFull(): String = format(dayMonthYear)

fun LocalDate.formatShort(): String = format(dayMonth)

fun formatEpochMillis(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().formatFull()

/** "2024-12-20" -> "20/12/2024", anything unparsable is returned unchanged. */
fun formatApiDate(value: String): String =
    runCatching { LocalDate.parse(value.take(10)).formatFull() }.getOrDefault(value)
