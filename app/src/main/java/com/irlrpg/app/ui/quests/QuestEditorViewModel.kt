package com.irlrpg.app.ui.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlrpg.app.data.local.entity.AttributeEntity
import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.repository.PlayerRepository
import com.irlrpg.app.data.repository.QuestRepository
import com.irlrpg.app.domain.model.AttributeKind
import com.irlrpg.app.domain.model.QuestDifficulty
import com.irlrpg.app.domain.model.QuestDraft
import com.irlrpg.app.domain.model.QuestKind
import com.irlrpg.app.domain.model.QuestStepDraft
import com.irlrpg.app.domain.model.ScheduleKind
import com.irlrpg.app.domain.model.TrackingKind
import com.irlrpg.app.domain.model.UnitKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuestEditorState(
    val editing: Boolean = false,
    val title: String = "",
    val emoji: String = "📘",
    val categoryId: Long? = null,
    val trackingKind: TrackingKind = TrackingKind.Time,
    val unit: UnitKind = UnitKind.Minutes,
    val customUnit: String = "",
    val targetText: String = "30",
    val scheduleKind: ScheduleKind = ScheduleKind.Daily,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val xpReward: Int = 60,
    val attributeName: String = AttributeKind.Focus.label,
    val notes: String = "",
    // ---- Step 2 fields ----
    val description: String = "",
    val difficulty: QuestDifficulty = QuestDifficulty.Medium,
    val questKind: QuestKind = QuestKind.Standard,
    /** null = no deadline at all (the default, matching Step 1 behaviour). */
    val deadlineInDays: Int? = null,
    val durationDays: Int = 30,
    val steps: List<QuestStepDraft> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val attributes: List<AttributeEntity> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    val target: Long get() = targetText.trim().toLongOrNull()?.coerceAtLeast(1L) ?: 1L
    val isMilestone: Boolean get() = trackingKind == TrackingKind.Milestone
    val unitLabel: String
        get() = when {
            unit == UnitKind.Custom -> customUnit.ifBlank { "unit" }
            else -> unit.label.lowercase()
        }
    val valid: Boolean
        get() = title.isNotBlank() &&
            (targetText.trim().toLongOrNull() ?: 0L) >= 1L &&
            (trackingKind == TrackingKind.Milestone || target >= 1L) &&
            (scheduleKind != ScheduleKind.SelectedDays || days.isNotEmpty()) &&
            (questKind != QuestKind.MultiStep || steps.any { it.title.isNotBlank() })

    val isMultiStep: Boolean get() = questKind == QuestKind.MultiStep
    val isLongTerm: Boolean get() = questKind == QuestKind.LongTerm
}

class QuestEditorViewModel(
    private val questRepository: QuestRepository,
    private val playerRepository: PlayerRepository,
    private val questId: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(QuestEditorState())
    val state: StateFlow<QuestEditorState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = questRepository.categoriesOnce()
            val attributes = playerRepository.attributes()
            var next = _state.value.copy(
                categories = categories,
                attributes = attributes,
                loading = false,
                attributeName = attributes.firstOrNull { it.attributeKind == AttributeKind.Focus.name }?.name
                    ?: attributes.firstOrNull()?.name
                    ?: AttributeKind.Focus.label,
            )
            if (questId != null && questId > 0) {
                questRepository.quest(questId)?.let { quest ->
                    val draft = QuestDraft.fromEntity(quest)
                    next = next.copy(
                        editing = true,
                        title = draft.title,
                        emoji = draft.emoji,
                        categoryId = draft.categoryId,
                        trackingKind = draft.trackingKind,
                        unit = draft.unit,
                        customUnit = draft.customUnit.orEmpty(),
                        targetText = draft.targetAmount.toString(),
                        scheduleKind = draft.scheduleKind,
                        days = draft.scheduleDays.ifEmpty { (1..7).toSet() },
                        xpReward = draft.xpReward,
                        attributeName = draft.attributeName,
                        notes = draft.notes.orEmpty(),
                        description = draft.description.orEmpty(),
                        difficulty = draft.difficulty,
                        questKind = draft.questKind,
                        durationDays = draft.durationDays.takeIf { it > 0 } ?: 30,
                        deadlineInDays = draft.deadlineEpochDay?.let {
                            (it - java.time.LocalDate.now().toEpochDay()).toInt()
                        },
                    )
                    next = next.copy(
                        steps = questRepository.stepsOnce(questId).map { step ->
                            QuestStepDraft(
                                title = step.title,
                                targetAmount = step.targetAmount,
                                isRequired = step.isRequired,
                                id = step.id,
                            )
                        }
                    )
                }
            }
            _state.value = next
        }
    }

    fun setTitle(value: String) = update { it.copy(title = value, error = null) }

    fun setEmoji(value: String) = update { it.copy(emoji = value) }

    fun setCategory(categoryId: Long?) = update { it.copy(categoryId = categoryId) }

    fun setNotes(value: String) = update { it.copy(notes = value) }

    fun setDescription(value: String) = update { it.copy(description = value, error = null) }

    fun setDifficulty(difficulty: QuestDifficulty) = update { state ->
        // Difficulty suggests a reward, but a reward the user already tuned is kept.
        val suggested = difficulty.suggestedXp
        state.copy(
            difficulty = difficulty,
            xpReward = if (state.xpReward == state.difficulty.suggestedXp) suggested else state.xpReward,
        )
    }

    fun setQuestKind(kind: QuestKind) = update { state ->
        val defaultSteps = if (kind == QuestKind.MultiStep && state.steps.isEmpty()) {
            listOf(QuestStepDraft(title = "", targetAmount = 1L))
        } else {
            state.steps
        }
        state.copy(questKind = kind, steps = defaultSteps, error = null)
    }

    /** null turns the deadline off entirely — no pressure, no overdue badge. */
    fun setDeadlineDays(days: Int?) = update { it.copy(deadlineInDays = days?.coerceIn(0, 1_095)) }

    fun setDurationDays(days: Int) = update { it.copy(durationDays = days.coerceIn(1, 730)) }

    // ---- multi-step editor ----

    fun addStep() = update { it.copy(steps = it.steps + QuestStepDraft(title = "", targetAmount = 1L)) }

    fun removeStep(index: Int) = update { state ->
        state.copy(steps = state.steps.filterIndexed { i, _ -> i != index })
    }

    fun updateStepTitle(index: Int, value: String) = update { state ->
        state.copy(
            steps = state.steps.mapIndexed { i, step -> if (i == index) step.copy(title = value) else step },
            error = null,
        )
    }

    fun updateStepTarget(index: Int, value: String) = update { state ->
        val target = value.filter { it.isDigit() }.take(6).toLongOrNull()?.coerceAtLeast(1L) ?: 1L
        state.copy(
            steps = state.steps.mapIndexed { i, step -> if (i == index) step.copy(targetAmount = target) else step },
        )
    }

    fun toggleStepRequired(index: Int) = update { state ->
        state.copy(
            steps = state.steps.mapIndexed { i, step ->
                if (i == index) step.copy(isRequired = !step.isRequired) else step
            },
        )
    }

    fun setCustomUnit(value: String) = update { it.copy(customUnit = value) }

    fun setTargetText(value: String) = update { it.copy(targetText = value.filter { ch -> ch.isDigit() }, error = null) }

    fun setSchedule(schedule: ScheduleKind) = update {
        it.copy(scheduleKind = schedule, days = it.days.ifEmpty { (1..7).toSet() })
    }

    fun toggleDay(day: Int) = update { state ->
        val days = if (state.days.contains(day)) state.days - day else state.days + day
        state.copy(days = days)
    }

    fun setAttribute(name: String) = update { it.copy(attributeName = name) }

    fun setXpReward(value: Int) = update { it.copy(xpReward = value.coerceIn(10, 1_000)) }

    /**
     * Switching tracking style re-points to sensible defaults, but preserves
     * user-entered target/xp if they already differ from the previous defaults.
     */
    fun setTrackingKind(kind: TrackingKind) = update { state ->
        if (state.trackingKind == kind) return@update state
        val defaultsFor: (TrackingKind) -> Triple<UnitKind, String, Int> = { k ->
            when (k) {
                TrackingKind.Time -> Triple(UnitKind.Minutes, "30", 60)
                TrackingKind.Count -> Triple(UnitKind.Pages, "25", 60)
                TrackingKind.Milestone -> Triple(UnitKind.Sessions, "1", 200)
                TrackingKind.Performance -> Triple(UnitKind.Points, "100", 120)
                TrackingKind.Custom -> Triple(UnitKind.Custom, "10", 60)
            }
        }
        val (newUnit, newTarget, newXp) = defaultsFor(kind)
        val (oldUnit, oldTarget, oldXp) = defaultsFor(state.trackingKind)
        // Only overwrite a field if the user hasn't customised it away from the old default
        val keepTarget = state.targetText != oldTarget
        val keepXp = state.xpReward != oldXp
        val keepUnit = state.unit != oldUnit
        state.copy(
            trackingKind = kind,
            unit = if (keepUnit) state.unit else newUnit,
            targetText = if (keepTarget) state.targetText else newTarget,
            xpReward = if (keepXp) state.xpReward else newXp,
            customUnit = if (kind == TrackingKind.Custom) state.customUnit.ifBlank { "steps" } else state.customUnit,
        )
    }

    fun setUnit(unit: UnitKind) = update { it.copy(unit = unit) }

    fun save() {
        val current = _state.value
        if (!current.valid) {
            _state.value = current.copy(
                error = when {
                    current.title.isBlank() -> "Give your quest a title"
                    current.scheduleKind == ScheduleKind.SelectedDays && current.days.isEmpty() ->
                        "Pick at least one day"
                    else -> "Set a target of at least 1 ${current.unitLabel}"
                }
            )
            return
        }
        viewModelScope.launch {
            _state.value = current.copy(saving = true, error = null)
            val today = java.time.LocalDate.now().toEpochDay()
            val draft = QuestDraft(
                title = current.title,
                emoji = current.emoji,
                categoryId = current.categoryId,
                trackingKind = current.trackingKind,
                unit = current.unit,
                customUnit = current.customUnit,
                targetAmount = current.target,
                scheduleKind = current.scheduleKind,
                scheduleDays = if (current.scheduleKind == ScheduleKind.SelectedDays) current.days else current.days,
                xpReward = current.xpReward,
                attributeName = current.attributeName,
                notes = current.notes,
                description = current.description,
                difficulty = current.difficulty,
                questKind = current.questKind,
                durationDays = if (current.questKind == QuestKind.LongTerm) current.durationDays else 0,
                deadlineEpochDay = current.deadlineInDays?.let { today + it },
                startedEpochDay = today,
                steps = current.steps.filter { it.title.isNotBlank() },
            )
            if (current.editing && questId != null && questId > 0) {
                val existing = questRepository.quest(questId)
                if (existing != null) {
                    questRepository.updateQuest(
                        entity = draft.toEntity(id = questId, createdAt = existing.createdAt).copy(
                            isActive = existing.isActive,
                            // A quest keeps the day it actually started, so long-term
                            // progress is never silently reset by an edit.
                            startedEpochDay = existing.startedEpochDay.takeIf { it > 0L } ?: today,
                        ),
                        steps = draft.steps,
                    )
                }
            } else {
                questRepository.createQuest(draft)
            }
            _state.value = _state.value.copy(saving = false, saved = true)
        }
    }

    private fun update(transform: (QuestEditorState) -> QuestEditorState) {
        _state.value = transform(_state.value)
    }
}
