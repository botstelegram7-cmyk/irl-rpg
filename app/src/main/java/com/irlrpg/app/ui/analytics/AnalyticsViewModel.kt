package com.irlrpg.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlrpg.app.core.TodaySource
import com.irlrpg.app.data.repository.AnalyticsRepository
import com.irlrpg.app.domain.model.AnalyticsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

enum class AnalyticsRange(val label: String) {
    Today("Today"),
    Week("Week"),
    Month("Month"),
    Year("Year"),
}

/** Summary numbers for whichever range the user picked. */
data class RangeSummary(
    val xp: Int,
    val completions: Int,
    val minutes: Int,
    val prevXp: Int,
)

class AnalyticsViewModel(
    analyticsRepository: AnalyticsRepository,
    private val todaySource: TodaySource,
) : ViewModel() {

    val state: StateFlow<AnalyticsState?> = analyticsRepository.observeAnalytics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _range = MutableStateFlow(AnalyticsRange.Week)
    val range: StateFlow<AnalyticsRange> = _range.asStateFlow()

    fun setRange(value: AnalyticsRange) {
        _range.value = value
    }

    fun refreshDay() {
        todaySource.refresh()
    }

    fun summaryFor(state: AnalyticsState, range: AnalyticsRange): RangeSummary = when (range) {
        AnalyticsRange.Today -> RangeSummary(
            xp = state.today.xp,
            completions = state.today.completions,
            minutes = state.today.minutes,
            prevXp = state.yesterday.xp,
        )
        AnalyticsRange.Week -> RangeSummary(
            xp = state.weekXp,
            completions = state.weekCompletions,
            minutes = state.weekMinutes,
            prevXp = state.lastWeekXp,
        )
        AnalyticsRange.Month -> RangeSummary(
            xp = state.monthXp,
            completions = state.monthCompletions,
            minutes = state.monthMinutes,
            prevXp = 0,
        )
        AnalyticsRange.Year -> RangeSummary(
            xp = state.yearXp,
            completions = state.lifetime.totalCompletions,
            minutes = state.lifetime.totalMinutes,
            prevXp = 0,
        )
    }
}
