package com.irlrpg.app.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlrpg.app.data.repository.AchievementRepository
import com.irlrpg.app.domain.model.AchievementView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AchievementsUiState(
    val loading: Boolean = true,
    val achievements: List<AchievementView> = emptyList(),
    val earnedCount: Int = 0,
    val totalCount: Int = 0,
    val recentCount: Int = 0,
)

class AchievementsViewModel(
    private val repository: AchievementRepository,
) : ViewModel() {

    /**
     * Live gallery: every row re-derives its progress from recorded activity, so
     * logging a quest anywhere in the app is reflected here immediately.
     */
    val state: StateFlow<AchievementsUiState> = repository.observeViews()
        .map { views ->
            val weekAgo = LocalDate.now().toEpochDay() - 7
            AchievementsUiState(
                loading = false,
                achievements = views,
                earnedCount = views.count { it.earned },
                totalCount = views.size,
                recentCount = views.count { it.earned && (it.earnedEpochDay ?: 0L) >= weekAgo },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AchievementsUiState())

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    /** Re-check history in case something was logged on another screen. */
    fun refresh() {
        viewModelScope.launch { runCatching { repository.sync() } }
    }

    fun consumeToast() {
        _toast.value = null
    }
}
