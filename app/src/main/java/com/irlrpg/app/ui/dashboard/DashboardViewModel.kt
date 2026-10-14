package com.irlrpg.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlrpg.app.core.TodaySource
import com.irlrpg.app.data.repository.DashboardRepository
import com.irlrpg.app.data.repository.PlayerRepository
import com.irlrpg.app.data.repository.ProgressSync
import com.irlrpg.app.domain.game.BossVictory
import com.irlrpg.app.domain.game.CosmeticCatalog
import com.irlrpg.app.domain.game.GameEngine
import com.irlrpg.app.domain.game.LogResult
import com.irlrpg.app.domain.model.AchievementDef
import com.irlrpg.app.domain.model.PlayerSnapshot
import com.irlrpg.app.domain.model.DashboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
    private val gameEngine: GameEngine,
    private val progressSync: ProgressSync,
    private val playerRepository: PlayerRepository,
    private val todaySource: TodaySource,
) : ViewModel() {

    val state: StateFlow<DashboardState?> = dashboardRepository.observeDashboard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _celebration = MutableStateFlow<LogResult?>(null)
    val celebration: StateFlow<LogResult?> = _celebration.asStateFlow()

    private val _busyQuestId = MutableStateFlow<Long?>(null)
    val busyQuestId: StateFlow<Long?> = _busyQuestId.asStateFlow()

    /** Achievements unlocked by the sweep that ran after the last action. */
    private val _unlocked = MutableStateFlow<List<AchievementDef>>(emptyList())
    val unlocked: StateFlow<List<AchievementDef>> = _unlocked.asStateFlow()

    /** Boss challenge completions triggered from this screen. */
    private val _victory = MutableStateFlow<Pair<BossVictory, Int>?>(null)
    val victory: StateFlow<Pair<BossVictory, Int>?> = _victory.asStateFlow()

    /** Avatar frame / title picked in Customize — purely visual. */
    val cosmetics: StateFlow<CosmeticsView> = playerRepository.observeSnapshot()
        .map { snapshot ->
            CosmeticsView(
                frameEmoji = CosmeticCatalog.byCode(snapshot.stats.cosmeticFrame)?.emoji ?: "🤍",
                titleName = CosmeticCatalog.byCode(snapshot.stats.displayTitle)?.name,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CosmeticsView())

    init {
        // Reconcile instances + rewards whenever the dashboard is opened.
        refreshDay()
    }

    /** Called when the screen resumes so a day rollover is picked up. */
    fun refreshDay() {
        todaySource.refresh()
        viewModelScope.launch {
            val event = runCatching { progressSync.sweep(todaySource.today()) }.getOrNull() ?: return@launch
            _unlocked.value = event.achievements
            event.victories.firstOrNull()?.let { win -> _victory.value = win to 0 }
        }
    }

    fun acknowledgeAchievement(def: AchievementDef) {
        _unlocked.value = _unlocked.value.filterNot { it.code == def.code }
    }

    fun dismissVictory() {
        _victory.value = null
    }

    fun log(questId: Long, amount: Long, stepId: Long? = null) {
        if (amount <= 0L) return
        viewModelScope.launch {
            _busyQuestId.value = questId
            val result = runCatching {
                gameEngine.logQuest(questId, amount, stepId = stepId, today = todaySource.today())
            }.getOrNull()
            _busyQuestId.value = null
            if (result != null) _celebration.value = result
            // One sweep right after the write keeps instances, boss rewards and
            // achievements consistent with the log the user just created.
            val event = runCatching { progressSync.sweep(todaySource.today()) }.getOrNull()
            if (event != null) {
                _unlocked.value = _unlocked.value + event.achievements
                event.victories.firstOrNull()?.let { win -> _victory.value = win to 0 }
            }
        }
    }

    fun logStep(questId: Long, stepId: Long, amount: Long) = log(questId, amount, stepId)

    fun dismissCelebration() {
        _celebration.value = null
    }
}

/** Cosmetic bits the dashboard shows next to the avatar. */
data class CosmeticsView(
    val frameEmoji: String = "🤍",
    val titleName: String? = null,
)
