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
import com.irlrpg.app.domain.model.QuestKind
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
    Challenges("Challenges"),
    MultiStep("Multi-step"),
    Paused("Paused"),
}

enum class QuestSort(val label: String) {
    Progress("Progress"),
    Recent("Newest"),
    Deadline("Deadline"),
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
    /** Free-text search over title, description and notes. */
    val query: String = "",
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

    private class Snapshot(
        val quests: List<com.irlrpg.app.data.local.entity.QuestEntity>,
        val categories: List<CategoryEntity>,
        val logs: List<com.irlrpg.app.data.local.entity.QuestLogEntity>,
        val stepsByQuest: Map<Long, List<com.irlrpg.app.data.local.entity.QuestStepEntity>>,
    )

    private val rawData = combine(
        questRepository.observeQuests(),
        questRepository.observeCategories(),
        questRepository.observeAllLogs(),
        questRepository.observeAllSteps(),
    ) { quests, categories, logs, steps ->
        Snapshot(quests, categories, logs, steps.groupBy { it.questId })
    }

    /** Progress recomputed whenever data changes *or* the day rolls over. */
    private val data = combine(rawData, todaySource.flow) { snapshot, today ->
        ProgressSnapshot(
            snapshot = snapshot,
            progress = QuestProgress.all(
                quests = snapshot.quests,
                logs = snapshot.logs,
                categories = snapshot.categories,
                today = today,
                stepsByQuest = snapshot.stepsByQuest,
            ),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProgressSnapshot(Snapshot(emptyList(), emptyList(), emptyList(), emptyMap()), emptyList()),
    )

    private class ProgressSnapshot(
        val snapshot: Snapshot,
        val progress: List<QuestWithProgress>,
    )

    init {
        viewModelScope.launch {
            data.collect { value ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    categories = value.snapshot.categories,
                    quests = applyFilters(value.progress, _uiState.value),
                )
            }
        }
    }

    fun refreshDay() = todaySource.refresh()

    fun setFilter(filter: QuestFilter) = update { it.copy(filter = filter) }

    fun setSort(sort: QuestSort) = update { it.copy(sort = sort) }

    fun setCategoryFilter(categoryId: Long?) = update { it.copy(categoryFilter = categoryId) }

    fun setQuery(query: String) = update { it.copy(query = query) }

    fun clearFilters() = update { it.copy(query = "", categoryFilter = null, filter = QuestFilter.All) }

    private fun update(transform: (QuestsUiState) -> QuestsUiState) {
        val next = transform(_uiState.value)
        _uiState.value = next.copy(quests = applyFilters(visibleProgress(), next))
    }

    private fun visibleProgress(): List<QuestWithProgress> = data.value.progress

    private fun applyFilters(
        source: List<QuestWithProgress>,
        state: QuestsUiState,
    ): List<QuestWithProgress> {
        val today = todaySource.today()
        val query = state.query.trim().lowercase()
        val filtered = source.filter { item ->
            val matchesFilter = when (state.filter) {
                QuestFilter.All -> true
                QuestFilter.Today -> item.quest.isActive && PeriodKeys.isScheduledFor(
                    ScheduleKind.fromName(item.quest.scheduleKind),
                    item.quest.scheduleDays,
                    today,
                )
                QuestFilter.Active -> item.quest.isActive && !item.isComplete
                QuestFilter.Challenges -> item.questKind == QuestKind.LongTerm
                QuestFilter.MultiStep -> item.questKind == QuestKind.MultiStep
                QuestFilter.Paused -> !item.quest.isActive
            }
            val matchesCategory = state.categoryFilter == null || item.quest.categoryId == state.categoryFilter
            val matchesQuery = query.isEmpty() ||
                item.quest.title.lowercase().contains(query) ||
                item.quest.description.orEmpty().lowercase().contains(query) ||
                item.quest.notes.orEmpty().lowercase().contains(query) ||
                item.category?.name.orEmpty().lowercase().contains(query)
            matchesFilter && matchesCategory && matchesQuery
        }
        return when (state.sort) {
            QuestSort.Progress -> filtered.sortedWith(
                compareBy<QuestWithProgress> { it.quest.isActive.not() }
                    .thenBy { it.isComplete }
                    .thenByDescending { it.fraction }
            )
            QuestSort.Recent -> filtered.sortedByDescending { it.quest.createdAt }
            // Quests with a deadline first (soonest first), then undated ones.
            QuestSort.Deadline -> filtered.sortedWith(
                compareBy<QuestWithProgress> { it.quest.deadlineEpochDay == null }
                    .thenBy { it.quest.deadlineEpochDay ?: Long.MAX_VALUE }
                    .thenBy { it.isComplete }
            )
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
