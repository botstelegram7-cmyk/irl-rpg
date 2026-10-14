package com.irlrpg.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlrpg.app.data.repository.AchievementRepository
import com.irlrpg.app.data.repository.PlayerRepository
import com.irlrpg.app.domain.game.CosmeticCatalog
import com.irlrpg.app.domain.model.CosmeticItem
import com.irlrpg.app.domain.model.CosmeticKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CosmeticRow(
    val item: CosmeticItem,
    val unlocked: Boolean,
    val selected: Boolean,
    val requirement: String,
)

data class CustomizeUiState(
    val frames: List<CosmeticRow> = emptyList(),
    val titles: List<CosmeticRow> = emptyList(),
    val playerName: String = "Adventurer",
    val level: Int = 1,
    val rankTitle: String = "Wanderer",
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
)

/**
 * Cosmetics are vanity only — they never gate real-life progress, and each one
 * is unlocked by an achievement that comes from recorded activity.
 */
class CustomizeViewModel(
    private val playerRepository: PlayerRepository,
    private val achievementRepository: AchievementRepository,
) : ViewModel() {

    val state: StateFlow<CustomizeUiState> = combine(
        playerRepository.observeSnapshot(),
        achievementRepository.observeViews(),
    ) { snapshot, views ->
        val earned = views.filter { it.earned }.map { it.def.code }.toSet()
        fun rows(kind: CosmeticKind): List<CosmeticRow> = CosmeticCatalog.items
            .filter { it.kind == kind }
            .map { item ->
                CosmeticRow(
                    item = item,
                    unlocked = CosmeticCatalog.isUnlocked(item, earned),
                    selected = when (kind) {
                        CosmeticKind.Frame -> snapshot.stats.cosmeticFrame == item.code
                        CosmeticKind.Title -> snapshot.stats.displayTitle == item.code
                    },
                    requirement = CosmeticCatalog.requirementText(item, views),
                )
            }

        CustomizeUiState(
            frames = rows(CosmeticKind.Frame),
            titles = rows(CosmeticKind.Title),
            playerName = snapshot.stats.name,
            level = snapshot.level,
            rankTitle = snapshot.rankTitle,
            unlockedCount = CosmeticCatalog.items.count { CosmeticCatalog.isUnlocked(it, earned) },
            totalCount = CosmeticCatalog.items.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomizeUiState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun selectFrame(item: CosmeticItem) {
        viewModelScope.launch {
            val unlocked = state.value.frames.firstOrNull { it.item.code == item.code }?.unlocked ?: false
            if (!unlocked) {
                _message.value = "Still locked — ${CosmeticCatalog.requirementTextFor(item)}"
                return@launch
            }
            playerRepository.setCosmeticFrame(item.code)
            _message.value = "${item.name} frame equipped"
        }
    }

    fun selectTitle(item: CosmeticItem) {
        viewModelScope.launch {
            val unlocked = state.value.titles.firstOrNull { it.item.code == item.code }?.unlocked ?: false
            if (!unlocked) {
                _message.value = "Still locked — ${CosmeticCatalog.requirementTextFor(item)}"
                return@launch
            }
            // Tapping the equipped title again removes it (back to plain name).
            val current = state.value.titles.firstOrNull { it.selected }?.item?.code
            playerRepository.setDisplayTitle(if (current == item.code) null else item.code)
            _message.value = if (current == item.code) "Title removed" else "\"${item.name}\" equipped"
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
