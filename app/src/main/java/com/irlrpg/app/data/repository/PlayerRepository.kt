package com.irlrpg.app.data.repository

import com.irlrpg.app.data.local.dao.AttributeDao
import com.irlrpg.app.data.local.dao.PlayerDao
import com.irlrpg.app.data.local.dao.ProgressionDao
import com.irlrpg.app.data.local.entity.AttributeEntity
import com.irlrpg.app.data.local.entity.PlayerStatsEntity
import com.irlrpg.app.data.local.entity.ProgressionEventEntity
import com.irlrpg.app.domain.game.LevelCurve
import com.irlrpg.app.domain.model.PlayerSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Reads and edits the character profile: XP, level, attributes, history. */
class PlayerRepository(
    private val playerDao: PlayerDao,
    private val attributeDao: AttributeDao,
    private val progressionDao: ProgressionDao,
) {

    fun observeSnapshot(): Flow<PlayerSnapshot> =
        playerDao.observe().map { stats -> snapshotOf(stats ?: placeholderStats()) }

    fun observeAttributes(): Flow<List<AttributeEntity>> = attributeDao.observeAll()

    fun observeHistory(limit: Int = 25): Flow<List<ProgressionEventEntity>> =
        progressionDao.observeRecent(limit)

    suspend fun stats(): PlayerStatsEntity? = playerDao.get()

    suspend fun ensurePlayer(today: LocalDate): PlayerStatsEntity {
        val existing = playerDao.get()
        if (existing != null) return existing
        val fresh = placeholderStats(today)
        playerDao.upsert(fresh)
        return fresh
    }

    suspend fun rename(name: String) = playerDao.setName(name.trim().ifBlank { "Adventurer" })

    suspend fun attributes(): List<AttributeEntity> = attributeDao.all()

    suspend fun addAttribute(name: String, emoji: String, accentId: String): Long = attributeDao.insert(
        AttributeEntity(
            name = name.trim(),
            emoji = emoji.ifBlank { "✨" },
            attributeKind = null,
            accentId = accentId,
            isBuiltIn = false,
            sortOrder = 100,
        )
    )

    suspend fun updateAttribute(attribute: AttributeEntity) = attributeDao.update(attribute)

    suspend fun deleteAttribute(id: Long) = attributeDao.deleteById(id)

    companion object {
        fun placeholderStats(today: LocalDate = LocalDate.now()): PlayerStatsEntity = PlayerStatsEntity(
            id = 1,
            name = "Adventurer",
            totalXp = 0,
            createdAtEpochDay = today.toEpochDay(),
        )

        fun snapshotOf(stats: PlayerStatsEntity): PlayerSnapshot {
            val level = LevelCurve.levelForTotalXp(stats.totalXp)
            return PlayerSnapshot(
                stats = stats,
                level = level,
                rankTitle = LevelCurve.rankTitle(level),
                rankEmoji = LevelCurve.rankEmoji(level),
                xpIntoLevel = LevelCurve.xpIntoLevel(stats.totalXp),
                xpForCurrentLevel = LevelCurve.xpSpanOfLevel(stats.totalXp),
                nextLevelXp = LevelCurve.totalXpForLevel(level + 1),
                progress = LevelCurve.progressFraction(stats.totalXp),
            )
        }
    }
}
