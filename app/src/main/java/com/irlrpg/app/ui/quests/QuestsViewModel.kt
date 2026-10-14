package com.irlrpg.app.ui.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlrpg.app.core.TodaySource
import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.repository.QuestProgress
import com.irlrpg.app.data.repository.QuestRepository
import com.irlrpg.app.domain.game.GameEngine
import com.irlrpg.app.domain.game.LogResult
import com.irlrpg.app.domain.game.PeriodKeys
import com.irlrpg.app.domain.model.QuestWithProgress
import com.irlrpg.app.domain.model.ScheduleKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class QuestFilter(val label: String) {
    All("All"),
    Today("Today"),
    Active("Active"),
    Paused("Paused"),
}

enum class QuestSort(val label: String) {
    Progress("Progress"),
    Recent("Newest"),
    Xp("XP reward"),
    Title("A-Z"),
}

data class QuestsUiState(
    val loading: Boolean = true,
    val quests: List<QuestWithProgress> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val filter: QuestFilter = QuestFilter.All,
    val sort: QuestSort = QuestSort.Progress,
    val categoryFilter: Long? = null,
)

class QuestsViewModel(
    private val questRepository: QuestRepository,
    private val gameEngine: GameEngine,
    private val todaySource: TodaySource,
) : ViewModel() {

    private val _celebration = MutableStateFlow<LogResult?>(null)
    val celebration: StateFlow<LogResult?> = _celebration.asStateFlow()

    fun log(questId: Long, amount: Long) {
        if (amount <= 0L) return
        viewModelScope.launch {
            gameEngine.logQuest(questId, amount, today = todaySource.today())?.let { _celebration.value = it }
        }
    }

    fun dismissCelebration() {
        _celebration.value = null
    }

    private val _uiState = MutableStateFlow(QuestsUiState())
    val uiState: StateFlow<QuestsUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val data = combine(
        questRepository.observeQuests(),
        questRepository.observeCategories(),
        questRepository.observeAllLogs(),
        todaySource.flow,
    ) { quests, categories, logs, today ->
        Triple(quests, categories, QuestProgress.all(quests, logs, categories, today))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Triple(emptyList(), emptyList(), emptyList()))

    init {
        viewModelScope.launch {
            data.collect { (_, categories, withProgress) ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    categories = categories,
                    quests = applyFilters(withProgress, _uiState.value),
                )
            }
        }
    }

    fun refreshDay() = todaySource.refresh()

    fun setFilter(filter: QuestFilter) = update { it.copy(filter = filter) }

    fun setSort(sort: QuestSort) = update { it.copy(sort = sort) }

    fun setCategoryFilter(categoryId: Long?) = update { it.copy(categoryFilter = categoryId) }

    private fun update(transform: (QuestsUiState) -> QuestsUiState) {
        val next = transform(_uiState.value)
        _uiState.value = next.copy(quests = applyFilters(visibleProgress(), next))
    }

    private fun visibleProgress(): List<QuestWithProgress> {
        val (_, _, progress) = data.value
        return progress
    }

    private fun applyFilters(
        source: List<QuestWithProgress>,
        state: QuestsUiState,
    ): List<QuestWithProgress> {
        val today = todaySource.today()
        val filtered = source.filter { item ->
            val matchesFilter = when (state.filter) {
                QuestFilter.All -> true
                QuestFilter.Today -> item.quest.isActive && PeriodKeys.isScheduledFor(
                    ScheduleKind.fromName(item.quest.scheduleKind),
                    item.quest.scheduleDays,
                    today,
                )
                QuestFilter.Active -> item.quest.isActive && !item.isComplete
                QuestFilter.Paused -> !item.quest.isActive
            }
            val matchesCategory = state.categoryFilter == null || item.quest.categoryId == state.categoryFilter
            matchesFilter && matchesCategory
        }
        return when (state.sort) {
            QuestSort.Progress -> filtered.sortedWith(
                compareBy<QuestWithProgress> { it.quest.isActive.not() }
                    .thenBy { it.isComplete }
                    .thenByDescending { it.fraction }
            )
            QuestSort.Recent -> filtered.sortedByDescending { it.quest.createdAt }
            QuestSort.Xp -> filtered.sortedByDescending { it.quest.xpReward }
            QuestSort.Title -> filtered.sortedBy { it.quest.title.lowercase() }
        }
    }

    fun toggleActive(id: Long) {
        viewModelScope.launch {
            val quest = questRepository.quest(id) ?: return@launch
            questRepository.setQuestActive(id, !quest.isActive)
            _message.value = if (quest.isActive) "Paused \"${quest.title}\"" else "Resumed \"${quest.title}\""
        }
    }

    fun deleteQuest(id: Long) {
        viewModelScope.launch {
            val quest = questRepository.quest(id) ?: return@launch
            questRepository.deleteQuest(id)
            _message.value = "Deleted \"${quest.title}\""
        }
    }

    fun duplicateQuest(id: Long) {
        viewModelScope.launch {
            val quest = questRepository.quest(id) ?: return@launch
            questRepository.createQuest(
                com.irlrpg.app.domain.model.QuestDraft.fromEntity(quest).copy(title = "${quest.title} (copy)")
            )
            _message.value = "Duplicated \"${quest.title}\""
        }
    }

    fun addCategory(name: String, emoji: String, accentId: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            questRepository.createCategory(name, emoji, accentId)
            _message.value = "Added category \"${name.trim()}\""
        }
    }

    fun renameCategory(category: CategoryEntity, name: String, emoji: String, accentId: String) {
        viewModelScope.launch {
            questRepository.updateCategory(
                category.copy(name = name.trim(), emoji = emoji, accentId = accentId)
            )
            _message.value = "Updated \"${name.trim()}\""
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            questRepository.deleteCategory(category.id)
            if (_uiState.value.categoryFilter == category.id) setCategoryFilter(null)
            _message.value = "Deleted \"${category.name}\" — its quests are now uncategorised"
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
