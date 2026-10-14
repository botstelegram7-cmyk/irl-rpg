package com.irlrpg.app.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Small date helpers shared by the UI and repositories. */
object DateX {

    fun today(): LocalDate = LocalDate.now()

    /** ISO week starting Monday. */
    fun startOfWeek(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun endOfWeek(date: LocalDate): LocalDate = startOfWeek(date).plusDays(6)

    fun startOfMonth(date: LocalDate): LocalDate = date.withDayOfMonth(1)

    fun startOfYear(date: LocalDate): LocalDate = date.withDayOfYear(1)

    fun weekDaysOf(date: LocalDate): List<LocalDate> {
        val start = startOfWeek(date)
        return (0..6).map { start.plusDays(it.toLong()) }
    }

    fun shortDayLabel(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2)

    fun dayAndMonth(date: LocalDate): String {
        val month: Month = date.month
        return "${date.dayOfMonth} ${month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}"
    }

    fun fullDate(date: LocalDate): String =
        "${dayAndMonth(date)} ${date.year}"

    /** "Today", "Yesterday", otherwise a short date. */
    fun relativeLabel(date: LocalDate, today: LocalDate = today()): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> dayAndMonth(date)
    }

    fun greeting(hour: Int): String = when (hour) {
        in 0..4 -> "Still up?"
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }

    /** Minutes rendered as "1h 20m" / "45m". */
    fun formatMinutes(minutes: Int): String {
        if (minutes <= 0) return "0m"
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours == 0 -> "${rest}m"
            rest == 0 -> "${hours}h"
            else -> "${hours}h ${rest}m"
        }
    }
}
