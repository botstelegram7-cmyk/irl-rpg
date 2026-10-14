package com.irlrpg.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlrpg.app.core.TodaySource
import com.irlrpg.app.data.repository.AnalyticsRepository
import com.irlrpg.app.domain.model.AnalyticsRange
import com.irlrpg.app.domain.model.AnalyticsState
import com.irlrpg.app.domain.model.RangeSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Analytics view state. The heavy lifting — bucketing and summing the recorded
 * log — happens in [AnalyticsRepository]; this class only tracks which range the
 * user is looking at and exposes the matching slice.
 */
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

    /** The summary for [range]; falls back to an empty summary before first load. */
    fun summaryFor(state: AnalyticsState, range: AnalyticsRange): RangeSummary =
        state.ranges[range] ?: RangeSummary(
            xp = 0,
            completions = 0,
            entries = 0,
            minutes = 0,
            activeDays = 0,
            previousXp = 0,
            previousCompletions = 0,
            previousLabel = "the previous period",
            bucketCaption = "",
        )
}
