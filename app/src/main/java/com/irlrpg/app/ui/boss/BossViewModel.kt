package com.irlrpg.app.ui.boss

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlrpg.app.core.TodaySource
import com.irlrpg.app.data.repository.BossBoardState
import com.irlrpg.app.data.repository.BossRepository
import com.irlrpg.app.data.repository.ProgressSync
import com.irlrpg.app.domain.game.BossVictory
import com.irlrpg.app.domain.model.BossProgress
import com.irlrpg.app.domain.model.BossTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Draft for the custom challenge builder. */
data class BossDraft(
    val title: String = "",
    val description: String = "",
    val emoji: String = "🐉",
    val accentId: String = "peach",
    val rewardXp: Int = 400,
    val durationDays: Int = 30,
    val objectives: List<ObjectiveDraft> = listOf(ObjectiveDraft()),
    val error: String? = null,
) {
    val valid: Boolean get() = title.isNotBlank() && objectives.any { it.title.isNotBlank() }
}

data class ObjectiveDraft(
    val title: String = "",
    val target: String = "1",
    val questId: Long? = null,
)

class BossViewModel(
    private val repository: BossRepository,
    private val progressSync: ProgressSync,
    private val todaySource: TodaySource,
) : ViewModel() {

    /** Stored rows (quest links, template list) — reacts to database changes. */
    val board: StateFlow<BossBoardState> = repository.observeBoard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BossBoardState())

    /** Live progress, resolved from the append-only log so it is always accurate. */
    private val _progress = MutableStateFlow<List<BossProgress>>(emptyList())
    val progress: StateFlow<List<BossProgress>> = _progress.asStateFlow()

    private val _victory = MutableStateFlow<Pair<BossVictory, Int>?>(null)
    val victory: StateFlow<Pair<BossVictory, Int>?> = _victory.asStateFlow()

    private val _draft = MutableStateFlow<BossDraft?>(null)
    val draft: StateFlow<BossDraft?> = _draft.asStateFlow()

    private val _templatePicker = MutableStateFlow<BossTemplate?>(null)
    val templatePicker: StateFlow<BossTemplate?> = _templatePicker.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refreshDay() {
        todaySource.refresh()
        refresh()
    }

    /** Reloads progress and pays any challenge that is already satisfied. */
    fun refresh() {
        viewModelScope.launch {
            val sweep = runCatching { progressSync.sweep(todaySource.today()) }.getOrNull()
            _progress.value = repository.progressAll()
            sweep?.victories?.firstOrNull()?.let { victory -> celebrate(victory) }
        }
    }

    fun logObjective(objectiveId: Long, amount: Long) {
        viewModelScope.launch {
            if (repository.logManualObjective(objectiveId, amount)) {
                // A manual objective can be the last missing one: sweep immediately
                // so the reward is paid exactly once, right now.
                val sweep = runCatching { progressSync.sweep(todaySource.today()) }.getOrNull()
                _progress.value = repository.progressAll()
                sweep?.victories?.firstOrNull()?.let { victory -> celebrate(victory) }
            }
        }
    }

    fun dismissVictory() {
        _victory.value = null
    }

    fun startTemplate(template: BossTemplate) {
        _templatePicker.value = template
    }

    fun confirmTemplate(deadline: LocalDate) {
        val template = _templatePicker.value ?: return
        viewModelScope.launch {
            repository.createFromTemplate(template, deadline, emptyMap(), todaySource.today())
            _templatePicker.value = null
            _message.value = "\"${template.title}\" added — edit its objectives to fit your plan"
            progressSync.sweep(todaySource.today(), announce = true)
            refresh()
        }
    }

    fun dismissTemplate() {
        _templatePicker.value = null
    }

    fun openBuilder() {
        _draft.value = BossDraft()
    }

    fun closeBuilder() {
        _draft.value = null
    }

    fun updateDraft(transform: (BossDraft) -> BossDraft) {
        _draft.value = _draft.value?.let(transform)
    }

    fun addObjectiveRow() {
        updateDraft { it.copy(objectives = it.objectives + ObjectiveDraft()) }
    }

    fun removeObjectiveRow(index: Int) {
        updateDraft { draft ->
            if (draft.objectives.size <= 1) draft
            else draft.copy(objectives = draft.objectives.filterIndexed { i, _ -> i != index })
        }
    }

    fun saveDraft() {
        val draft = _draft.value ?: return
        if (!draft.valid) {
            _draft.value = draft.copy(error = "Give the challenge a title and at least one objective")
            return
        }
        viewModelScope.launch {
            repository.createCustom(
                title = draft.title,
                description = draft.description.ifBlank { null },
                emoji = draft.emoji,
                accentId = draft.accentId,
                deadline = todaySource.today().plusDays(draft.durationDays.toLong().coerceAtLeast(1L)),
                rewardXp = draft.rewardXp,
                objectives = draft.objectives
                    .filter { it.title.isNotBlank() }
                    .map { Triple(it.title, it.target.toLongOrNull()?.coerceAtLeast(1L) ?: 1L, it.questId) },
                today = todaySource.today(),
            )
            _draft.value = null
            _message.value = "Challenge created"
            progressSync.sweep(todaySource.today(), announce = true)
            refresh()
        }
    }

    fun archive(bossId: Long) {
        viewModelScope.launch {
            repository.archive(bossId)
            _message.value = "Challenge archived"
            refresh()
        }
    }

    fun reactivate(bossId: Long) {
        viewModelScope.launch {
            repository.reactivate(bossId)
            _message.value = "Challenge reactivated"
            refresh()
        }
    }

    fun delete(bossId: Long) {
        viewModelScope.launch {
            repository.delete(bossId)
            _message.value = "Challenge deleted"
            refresh()
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun templates(): List<BossTemplate> = repository.templates()

    private suspend fun celebrate(victory: BossVictory) {
        val total = repository.progressOf(victory.bossId)?.objectives?.size ?: victory.objectivesCompleted
        _victory.value = victory to total
    }
}
