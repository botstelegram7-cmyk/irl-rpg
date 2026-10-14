package com.irlrpg.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irlrpg.app.data.repository.HistoryRepository
import com.irlrpg.app.domain.model.CompletionHistoryEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class QuestHistoryOption(
    val questId: Long,
    val title: String,
    val emoji: String,
)

data class HistoryUiState(
    val entries: List<CompletionHistoryEntry> = emptyList(),
    val questOptions: List<QuestHistoryOption> = emptyList(),
    val totalXp: Int = 0,
)

class HistoryViewModel(
    repository: HistoryRepository,
) : ViewModel() {

    val state: StateFlow<HistoryUiState> = repository.observeHistory()
        .map { entries ->
            HistoryUiState(
                entries = entries,
                questOptions = entries
                    .map { QuestHistoryOption(it.questId, it.questTitle, it.emoji) }
                    .distinctBy { it.questId },
                totalXp = entries.sumOf { it.xp },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}
