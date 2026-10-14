package com.irlrpg.app.data.local

import androidx.room.withTransaction
import com.irlrpg.app.data.local.entity.AttributeEntity
import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.local.entity.PlayerStatsEntity
import com.irlrpg.app.data.prefs.SettingsStore
import com.irlrpg.app.domain.model.AttributeKind
import java.time.LocalDate

/**
 * First-run setup: a handful of starter categories and the six skill attributes.
 * Nothing else is inserted — no sample quests, no fake achievements, no
 * pre-filled progress. Stats start honestly at zero.
 */
class DatabaseSeeder(
    private val database: IrlRpgDatabase,
    private val settings: SettingsStore,
) {

    suspend fun seedIfNeeded(today: LocalDate = LocalDate.now()) {
        if (settings.isSeeded) return
        database.withTransaction {
            if (database.categoryDao().count() == 0) {
                database.categoryDao().insertAll(
                    listOf(
                        CategoryEntity(name = "Reading", emoji = "📚", accentId = "sky", isBuiltIn = true),
                        CategoryEntity(name = "Writing", emoji = "✍️", accentId = "blush", isBuiltIn = true),
                        CategoryEntity(name = "Building", emoji = "💻", accentId = "lavender", isBuiltIn = true),
                        CategoryEntity(name = "Learning", emoji = "🎓", accentId = "butter", isBuiltIn = true),
                        CategoryEntity(name = "Fitness", emoji = "💪", accentId = "peach", isBuiltIn = true),
                        CategoryEntity(name = "Wellbeing", emoji = "🌿", accentId = "mint", isBuiltIn = true),
                    )
                )
            }
            if (database.attributeDao().count() == 0) {
                database.attributeDao().insertAll(
                    AttributeKind.entries.mapIndexed { index, kind ->
                        AttributeEntity(
                            name = kind.label,
                            emoji = kind.emoji,
                            attributeKind = kind.name,
                            accentId = kind.accentId,
                            isBuiltIn = true,
                            sortOrder = index,
                        )
                    }
                )
            }
            if (database.playerDao().get() == null) {
                database.playerDao().upsert(
                    PlayerStatsEntity(id = 1, name = "Adventurer", totalXp = 0, createdAtEpochDay = today.toEpochDay())
                )
            }
        }
        settings.isSeeded = true
    }
}
