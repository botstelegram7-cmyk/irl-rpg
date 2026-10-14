package com.irlrpg.app.ui.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlrpg.app.data.local.entity.AttributeEntity
import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.repository.PlayerRepository
import com.irlrpg.app.data.repository.QuestRepository
import com.irlrpg.app.domain.model.AttributeKind
import com.irlrpg.app.domain.model.QuestDraft
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
            (scheduleKind != ScheduleKind.SelectedDays || days.isNotEmpty())
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

    fun setXpReward(value: Int) = update { it.copy(xpReward = value.coerceIn(10, 500)) }

    /** Switching tracking style re-points unit + target + XP to sensible defaults. */
    fun setTrackingKind(kind: TrackingKind) = update { state ->
        val (unit, target, xp) = when (kind) {
            TrackingKind.Time -> Triple(UnitKind.Minutes, "30", 60)
            TrackingKind.Count -> Triple(UnitKind.Pages, "25", 60)
            TrackingKind.Milestone -> Triple(UnitKind.Sessions, "1", 200)
            TrackingKind.Performance -> Triple(UnitKind.Points, "100", 120)
            TrackingKind.Custom -> Triple(UnitKind.Custom, "10", 60)
        }
        state.copy(
            trackingKind = kind,
            unit = unit,
            targetText = target,
            xpReward = xp,
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
            )
            if (current.editing && questId != null && questId > 0) {
                val existing = questRepository.quest(questId)
                if (existing != null) {
                    questRepository.updateQuest(draft.toEntity(id = questId, createdAt = existing.createdAt).copy(isActive = existing.isActive))
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
