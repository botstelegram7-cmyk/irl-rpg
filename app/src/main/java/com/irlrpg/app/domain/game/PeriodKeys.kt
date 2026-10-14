package com.irlrpg.app.domain.game

import com.irlrpg.app.domain.model.ScheduleKind
import java.time.LocalDate
import java.time.temporal.WeekFields

/**
 * Period keys decide which bucket a log entry counts towards: a daily quest
 * resets each day, a weekly quest each ISO week, and a flexible quest keeps
 * accumulating (useful for "finish 12 books" style goals).
 */
object PeriodKeys {

    fun dayKey(date: LocalDate): String = date.toString()

    fun weekKey(date: LocalDate): String {
        val fields = WeekFields.ISO
        val week = date.get(fields.weekOfWeekBasedYear())
        val year = date.get(fields.weekBasedYear())
        return "W$year-${week.toString().padStart(2, '0')}"
    }

    /** Key that never rolls over — progress accumulates for the lifetime of the quest. */
    const val FLEX_KEY = "flex"

    fun forSchedule(schedule: ScheduleKind, date: LocalDate): String = when (schedule) {
        ScheduleKind.Daily, ScheduleKind.Weekdays, ScheduleKind.SelectedDays -> dayKey(date)
        ScheduleKind.Weekly -> weekKey(date)
        ScheduleKind.Flexible -> FLEX_KEY
    }

    /** All keys that could hold today's progress — used for a single batched query. */
    fun currentKeys(date: LocalDate): List<String> = listOf(dayKey(date), weekKey(date), FLEX_KEY)

    /** True when the quest is expected on [date] according to its schedule. */
    fun isScheduledFor(
        schedule: ScheduleKind,
        scheduleDays: String,
        date: LocalDate,
    ): Boolean = when (schedule) {
        ScheduleKind.Daily -> true
        ScheduleKind.Weekdays -> date.dayOfWeek.value in 1..5
        ScheduleKind.SelectedDays -> scheduleDays
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .contains(date.dayOfWeek.value)
        ScheduleKind.Weekly -> true
        ScheduleKind.Flexible -> true
    }
}
