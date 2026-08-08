package pt.mybooks.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("pt", "PT"))
private val inputFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")
    .withLocale(Locale("pt", "PT"))
    .withResolverStyle(ResolverStyle.STRICT)

fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

fun formatEpochDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(displayFormatter)

fun formatEpochDayForInput(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(inputFormatter)

fun parseDateInput(value: String): Long? = try {
    LocalDate.parse(value.trim(), inputFormatter).toEpochDay()
} catch (_: DateTimeParseException) {
    null
}

fun daysUntil(epochDay: Long, today: Long = todayEpochDay()): Long = epochDay - today
