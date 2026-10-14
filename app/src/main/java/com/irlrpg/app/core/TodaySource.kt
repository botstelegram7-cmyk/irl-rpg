package com.irlrpg.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Holds the current day and lets the UI ask for a re-read when the app comes
 * back to the foreground. No timers, no background work — a device that stays
 * open past midnight simply refreshes on the next resume.
 */
class TodaySource(private val clock: () -> LocalDate = { LocalDate.now() }) {

    private val state = MutableStateFlow(clock())
    val flow: StateFlow<LocalDate> = state.asStateFlow()

    fun refresh(): LocalDate {
        val now = clock()
        if (state.value != now) state.value = now
        return now
    }

    fun today(): LocalDate = state.value
}
