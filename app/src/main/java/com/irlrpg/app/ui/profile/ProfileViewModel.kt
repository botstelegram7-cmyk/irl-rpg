package com.irlrpg.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlrpg.app.data.local.entity.ProgressionEventEntity
import com.irlrpg.app.data.repository.PlayerRepository
import com.irlrpg.app.data.repository.QuestRepository
import com.irlrpg.app.domain.game.LevelCurve
import com.irlrpg.app.domain.model.PlayerSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val player: PlayerSnapshot? = null,
    val history: List<ProgressionEventEntity> = emptyList(),
    val questCount: Int = 0,
    val activeQuestCount: Int = 0,
    val categoriesCount: Int = 0,
    val skillCount: Int = 0,
    val logCount: Int = 0,
    val levelUps: Int = 0,
) {
    val nextLevelXp: Int get() = player?.nextLevelXp ?: 0
}

class ProfileViewModel(
    private val playerRepository: PlayerRepository,
    questRepository: QuestRepository,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private data class ProfileBundle(
        val snapshot: PlayerSnapshot,
        val history: List<ProgressionEventEntity>,
        val quests: List<com.irlrpg.app.data.local.entity.QuestEntity>,
        val logs: List<com.irlrpg.app.data.local.entity.QuestLogEntity>,
    )

    val uiState: StateFlow<ProfileUiState> = combine(
        combine(
            playerRepository.observeSnapshot(),
            playerRepository.observeHistory(30),
            questRepository.observeQuests(),
            questRepository.observeAllLogs(),
        ) { snapshot, history, quests, logs -> ProfileBundle(snapshot, history, quests, logs) },
        questRepository.observeCategories(),
        playerRepository.observeAttributes(),
    ) { bundle, categories, attributes ->
        ProfileUiState(
            loading = false,
            player = bundle.snapshot,
            history = bundle.history,
            questCount = bundle.quests.size,
            activeQuestCount = bundle.quests.count { it.isActive },
            categoriesCount = categories.size,
            skillCount = attributes.size,
            logCount = bundle.logs.size,
            // Each level after the first was earned exactly one level-up.
            levelUps = (LevelCurve.levelForTotalXp(bundle.snapshot.stats.totalXp) - 1).coerceAtLeast(0),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun rename(name: String) {
        viewModelScope.launch {
            playerRepository.rename(name)
            _message.value = "Saved your new name"
        }
    }

    fun rankTitleFor(level: Int): String = LevelCurve.rankTitle(level)

    fun consumeMessage() {
        _message.value = null
    }
}
