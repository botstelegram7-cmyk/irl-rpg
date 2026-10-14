package com.irlrpg.app.data.repository

import com.irlrpg.app.data.local.dao.CategoryDao
import com.irlrpg.app.data.local.dao.QuestDao
import com.irlrpg.app.data.local.dao.QuestLogDao
import com.irlrpg.app.domain.game.LevelProgression
import com.irlrpg.app.domain.game.LevelRules
import com.irlrpg.app.domain.model.CompletionHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate

/**
 * Read-only view of the append-only activity log, newest first.
 *
 * The log is the single source of truth for every number the app displays, so
 * this screen doubles as the audit trail: nothing appears here that was not
 * explicitly recorded by a quest action.
 */
class HistoryRepository(
    private val logDao: QuestLogDao,
    private val questDao: QuestDao,
    private val categoryDao: CategoryDao,
    private val levelRules: () -> LevelRules = { LevelRules.Default },
) {

    fun observeHistory(limit: Int = 300): Flow<List<CompletionHistoryEntry>> = combine(
        logDao.observeCompletions(limit),
        logDao.observeAll(),
        questDao.observeAll(),
        categoryDao.observeAll(),
    ) { completions, allLogs, quests, categories ->
        val questsById = quests.associateBy { it.id }
        val categoriesById = categories.associateBy { it.id }
        val progression = LevelProgression(levelRules())

        // Level-up flags are re-derived from the full ordered log, so they stay
        // correct after edits, deletions or undo.
        val levelUpIds = mutableSetOf<Long>()
        var running = 0
        allLogs.sortedWith(compareBy({ it.epochDay }, { it.id })).forEach { log ->
            val before = progression.levelForTotalXp(running)
            running += log.xpEarned
            val after = progression.levelForTotalXp(running)
            if (after > before) levelUpIds.add(log.id)
        }

        completions.map { log ->
            val quest = questsById[log.questId]
            val categoryName = quest?.categoryId?.let { categoriesById[it]?.name }
            CompletionHistoryEntry(
                logId = log.id,
                questId = log.questId,
                questTitle = quest?.title ?: "Deleted quest",
                emoji = quest?.emoji ?: "✨",
                amountLabel = buildString {
                    if (log.amount > 1L) append("${log.amount} ${log.unitLabel}")
                    else append(log.unitLabel)
                    categoryName?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it)
                    }
                },
                xp = log.xpEarned,
                date = LocalDate.ofEpochDay(log.epochDay),
                levelUp = levelUpIds.contains(log.id),
            )
        }
    }.flowOn(Dispatchers.Default)
}
