package com.irlrpg.app.ui.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlrpg.app.data.local.entity.AttributeEntity
import com.irlrpg.app.data.repository.PlayerRepository
import com.irlrpg.app.data.repository.QuestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SkillRow(
    val attribute: AttributeEntity,
    val xp: Int,
    val share: Float,
    val questCount: Int,
    val activeQuestCount: Int,
    val completions: Int,
)

data class SkillsUiState(
    val loading: Boolean = true,
    val rows: List<SkillRow> = emptyList(),
    val maxXp: Int = 0,
)

/**
 * Skills screen data. XP per attribute is recomputed from the activity log (the
 * same rows the engine wrote) — so the numbers can always be traced back to real
 * recorded sessions.
 */
class SkillsViewModel(
    private val playerRepository: PlayerRepository,
    private val questRepository: QuestRepository,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<SkillsUiState> = combine(
        playerRepository.observeAttributes(),
        questRepository.observeQuests(),
        questRepository.observeAllLogs(),
    ) { attributes, quests, logs ->
        val questAttribute = quests.associate { it.id to it.attributeKind }
        val xpByAttribute = HashMap<Long, Int>()
        val completionsByAttribute = HashMap<Long, Int>()
        logs.forEach { log ->
            val value = questAttribute[log.questId] ?: return@forEach
            val attribute = attributes.firstOrNull { it.attributeKind == value || it.name == value }
                ?: return@forEach
            xpByAttribute[attribute.id] = (xpByAttribute[attribute.id] ?: 0) + log.xpEarned
            if (log.isCompletion) {
                completionsByAttribute[attribute.id] = (completionsByAttribute[attribute.id] ?: 0) + 1
            }
        }
        val totalXp = xpByAttribute.values.sum().coerceAtLeast(1)
        SkillsUiState(
            loading = false,
            rows = attributes.map { attribute ->
                val xp = xpByAttribute[attribute.id] ?: 0
                SkillRow(
                    attribute = attribute,
                    xp = xp,
                    share = xp.toFloat() / totalXp.toFloat(),
                    questCount = quests.count { it.attributeKind == attribute.attributeKind || it.attributeKind == attribute.name },
                    activeQuestCount = quests.count {
                        it.isActive && (it.attributeKind == attribute.attributeKind || it.attributeKind == attribute.name)
                    },
                    completions = completionsByAttribute[attribute.id] ?: 0,
                )
            }.sortedWith(compareByDescending<SkillRow> { it.xp }.thenBy { it.attribute.sortOrder }),
            maxXp = xpByAttribute.values.maxOrNull() ?: 0,
        )
    }.map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SkillsUiState())

    fun addSkill(name: String, emoji: String, accentId: String) {
        if (name.isBlank()) {
            _message.value = "Give your skill a name"
            return
        }
        viewModelScope.launch {
            playerRepository.addAttribute(name, emoji, accentId)
            _message.value = "Added skill \"${name.trim()}\""
        }
    }

    fun updateSkill(attribute: AttributeEntity, name: String, emoji: String, accentId: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playerRepository.updateAttribute(attribute.copy(name = name.trim(), emoji = emoji, accentId = accentId))
            _message.value = "Updated \"${name.trim()}\""
        }
    }

    fun deleteSkill(attribute: AttributeEntity) {
        viewModelScope.launch {
            playerRepository.deleteAttribute(attribute.id)
            _message.value = "Removed \"${attribute.name}\""
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
