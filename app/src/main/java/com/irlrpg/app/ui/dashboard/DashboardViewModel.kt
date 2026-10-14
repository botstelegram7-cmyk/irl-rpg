package com.irlrpg.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlrpg.app.core.TodaySource
import com.irlrpg.app.data.repository.DashboardRepository
import com.irlrpg.app.domain.game.GameEngine
import com.irlrpg.app.domain.game.LogResult
import com.irlrpg.app.domain.model.DashboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
    private val gameEngine: GameEngine,
    private val todaySource: TodaySource,
) : ViewModel() {

    val state: StateFlow<DashboardState?> = dashboardRepository.observeDashboard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _celebration = MutableStateFlow<LogResult?>(null)
    val celebration: StateFlow<LogResult?> = _celebration.asStateFlow()

    private val _busyQuestId = MutableStateFlow<Long?>(null)
    val busyQuestId: StateFlow<Long?> = _busyQuestId.asStateFlow()

    /** Called when the screen resumes so a day rollover is picked up. */
    fun refreshDay() {
        todaySource.refresh()
    }

    fun log(questId: Long, amount: Long) {
        if (amount <= 0L) return
        viewModelScope.launch {
            _busyQuestId.value = questId
            val result = runCatching { gameEngine.logQuest(questId, amount, today = todaySource.today()) }.getOrNull()
            _busyQuestId.value = null
            if (result != null) _celebration.value = result
        }
    }

    fun dismissCelebration() {
        _celebration.value = null
    }
}
