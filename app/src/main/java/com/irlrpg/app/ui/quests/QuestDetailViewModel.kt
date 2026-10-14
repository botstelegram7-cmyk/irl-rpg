package com.irlrpg.app.ui.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlrpg.app.core.TodaySource
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.local.entity.QuestLogEntity
import com.irlrpg.app.data.repository.QuestProgress
import com.irlrpg.app.data.repository.QuestRepository
import com.irlrpg.app.domain.game.GameEngine
import com.irlrpg.app.domain.game.LogResult
import com.irlrpg.app.domain.model.QuestWithProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuestDetailState(
    val loading: Boolean = true,
    val quest: QuestEntity? = null,
    val progress: QuestWithProgress? = null,
    val logs: List<QuestLogEntity> = emptyList(),
    val periodLabel: String = "",
    val xpFromQuest: Int = 0,
    val completions: Int = 0,
    val deleted: Boolean = false,
)

class QuestDetailViewModel(
    private val questRepository: QuestRepository,
    private val gameEngine: GameEngine,
    private val todaySource: TodaySource,
    private val questId: Long,
) : ViewModel() {

    val state: StateFlow<QuestDetailState> = combine(
        questRepository.observeQuest(questId),
        questRepository.observeCategories(),
        questRepository.observeQuestLogs(questId),
        todaySource.flow,
    ) { quest, categories, logs, today ->
        if (quest == null) {
            QuestDetailState(loading = false, deleted = true)
        } else {
            val category = categories.firstOrNull { it.id == quest.categoryId }
            QuestDetailState(
                loading = false,
                quest = quest,
                progress = QuestProgress.withProgress(quest, logs, category, today),
                logs = logs,
                periodLabel = QuestProgress.periodLabel(quest),
                xpFromQuest = logs.sumOf { it.xpEarned },
                completions = logs.count { it.isCompletion },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuestDetailState())

    private val _celebration = MutableStateFlow<LogResult?>(null)
    val celebration: StateFlow<LogResult?> = _celebration.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun log(amount: Long) {
        if (amount <= 0L) return
        viewModelScope.launch {
            gameEngine.logQuest(questId, amount, today = todaySource.today())?.let { _celebration.value = it }
        }
    }

    fun undo(logId: Long) {
        viewModelScope.launch {
            val ok = gameEngine.undoLog(logId)
            _message.value = if (ok) "Entry removed · XP corrected" else "Could not remove that entry"
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            val removed = gameEngine.resetQuestProgress(questId)
            _message.value = if (removed > 0) {
                "Progress reset · $removed ${if (removed == 1) "entry" else "entries"} cleared"
            } else {
                "Nothing to reset"
            }
        }
    }

    fun toggleActive() {
        viewModelScope.launch {
            val quest = questRepository.quest(questId) ?: return@launch
            questRepository.setQuestActive(questId, !quest.isActive)
            _message.value = if (quest.isActive) "Quest paused" else "Quest resumed"
        }
    }

    fun deleteQuest() {
        viewModelScope.launch {
            questRepository.deleteQuest(questId)
            _message.value = "Quest deleted"
        }
    }

    fun dismissCelebration() {
        _celebration.value = null
    }

    fun consumeMessage() {
        _message.value = null
    }
}
