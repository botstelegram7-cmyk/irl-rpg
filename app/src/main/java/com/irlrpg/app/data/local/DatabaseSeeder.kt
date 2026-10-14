package com.irlrpg.app.data.local

import androidx.room.withTransaction
import com.irlrpg.app.data.local.entity.AttributeEntity
import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.local.entity.PlayerStatsEntity
import com.irlrpg.app.data.local.entity.SkillNodeEntity
import com.irlrpg.app.data.prefs.SettingsStore
import com.irlrpg.app.domain.model.AttributeKind
import com.irlrpg.app.domain.model.SkillUnlockRule
import java.time.LocalDate

/**
 * First-run setup: starter categories, the six skill attributes, and the default
 * skill tree.
 *
 * Everything here is idempotent and additive — running it on an existing Step 1
 * database adds the tree without touching a single XP total, log row or quest.
 * No sample quests, no fake achievements, no pre-filled progress.
 */
class DatabaseSeeder(
    private val database: IrlRpgDatabase,
    private val settings: SettingsStore,
) {

    suspend fun seedIfNeeded(today: LocalDate = LocalDate.now()) {
        if (settings.isSeeded) {
            // Step 2: the tree may still be missing for a Step 1 → Step 2 upgrade.
            seedSkillTreeIfEmpty()
            return
        }
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
        seedSkillTreeIfEmpty()
    }

    /**
     * Seeds the default five-branch skill tree, linking each node to the attribute
     * that powers it. Only runs when the tree is completely empty, so a user's own
     * edits are never overwritten.
     */
    private suspend fun seedSkillTreeIfEmpty() {
        if (database.skillNodeDao().count() > 0) return
        val attributes = database.attributeDao().all()
        val byKind = attributes.associateBy { it.attributeKind }
        fun attributeId(kind: AttributeKind): Long? = byKind[kind.name]?.id

        database.withTransaction {
            if (database.skillNodeDao().count() > 0) return@withTransaction

            fun node(
                name: String,
                emoji: String,
                description: String,
                accentId: String,
                parentId: Long?,
                attribute: AttributeKind,
                masteryXp: Int,
                unlockRule: SkillUnlockRule,
                unlockParentMastery: Int = 0,
                requiredAnyXp: Int = 0,
                sortOrder: Int,
            ) = SkillNodeEntity(
                name = name,
                emoji = emoji,
                description = description,
                accentId = accentId,
                parentId = parentId,
                attributeId = attributeId(attribute),
                masteryXp = masteryXp,
                unlockRule = unlockRule.name,
                unlockParentMastery = unlockParentMastery,
                requiredAnyXp = requiredAnyXp,
                isBuiltIn = true,
                sortOrder = sortOrder,
            )

            // ---- Branch 1: Reading ----
            val reading = database.skillNodeDao().insert(
                node(
                    "Reading", "📖", "Time spent turning pages.",
                    "sky", null, AttributeKind.Intellect, 240, SkillUnlockRule.Root, sortOrder = 0,
                )
            )
            val comprehension = database.skillNodeDao().insert(
                node(
                    "Comprehension", "🧩", "Understanding what you read, not just finishing it.",
                    "sky", reading, AttributeKind.Intellect, 320,
                    SkillUnlockRule.ParentMastery, unlockParentMastery = 120, sortOrder = 1,
                )
            )
            val analysis = database.skillNodeDao().insert(
                node(
                    "Analysis", "🔍", "Breaking arguments apart and seeing structure.",
                    "sky", comprehension, AttributeKind.Intellect, 400,
                    SkillUnlockRule.ParentMastery, unlockParentMastery = 160, sortOrder = 2,
                )
            )
            database.skillNodeDao().insert(
                node(
                    "Critical Thinking", "⚖️", "Judging ideas on evidence rather than vibes.",
                    "sky", analysis, AttributeKind.Intellect, 520,
                    SkillUnlockRule.ParentMastery, unlockParentMastery = 200, sortOrder = 3,
                )
            )

            // ---- Branch 2: Building ----
            val programming = database.skillNodeDao().insert(
                node(
                    "Programming", "💻", "Time spent writing and shipping code.",
                    "lavender", null, AttributeKind.Focus, 300, SkillUnlockRule.Root, sortOrder = 10,
                )
            )
            val fundamentals = database.skillNodeDao().insert(
                node(
                    "Fundamentals", "🧱", "Data structures, correctness and the basics done well.",
                    "lavender", programming, AttributeKind.Focus, 360,
                    SkillUnlockRule.ParentMastery, unlockParentMastery = 150, sortOrder = 11,
                )
            )
            val projects = database.skillNodeDao().insert(
                node(
                    "Projects", "🚀", "Finishing real things that other people can use.",
                    "lavender", fundamentals, AttributeKind.Focus, 460,
                    SkillUnlockRule.ParentMastery, unlockParentMastery = 200, sortOrder = 12,
                )
            )
            database.skillNodeDao().insert(
                node(
                    "Advanced Development", "🏗️", "Systems thinking, architecture, hard trade-offs.",
                    "lavender", projects, AttributeKind.Focus, 600,
                    SkillUnlockRule.ParentMastery, unlockParentMastery = 260, sortOrder = 13,
                )
            )

            // ---- Branch 3: Fitness ----
            val fitness = database.skillNodeDao().insert(
                node(
                    "Fitness", "🏃", "Showing up for your body.",
                    "peach", null, AttributeKind.Strength, 260, SkillUnlockRule.Root, sortOrder = 20,
                )
            )
            val endurance = database.skillNodeDao().insert(
                node(
                    "Endurance", "🫀", "Lasting longer than last month.",
                    "peach", fitness, AttributeKind.Strength, 340,
                    SkillUnlockRule.ParentProgress, sortOrder = 21,
                )
            )
            val strength = database.skillNodeDao().insert(
                node(
                    "Strength", "🏋️", "Moving heavier things, safely.",
                    "peach", fitness, AttributeKind.Strength, 340,
                    SkillUnlockRule.ParentProgress, sortOrder = 22,
                )
            )
            database.skillNodeDao().insert(
                node(
                    "Performance", "🥇", "Peak output when it counts.",
                    "peach", strength, AttributeKind.Strength, 520,
                    SkillUnlockRule.ParentMastery, unlockParentMastery = 200, sortOrder = 23,
                )
            )

            // ---- Branch 4: Writing ----
            val writing = database.skillNodeDao().insert(
                node(
                    "Writing", "✍️", "Regular time putting words down.",
                    "blush", null, AttributeKind.Creativity, 260, SkillUnlockRule.Root, sortOrder = 30,
                )
            )
            val vocabulary = database.skillNodeDao().insert(
                node(
                    "Vocabulary", "🔤", "A wider, sharper set of words.",
                    "blush", writing, AttributeKind.Creativity, 300,
                    SkillUnlockRule.ParentMastery, unlockParentMastery = 120, sortOrder = 31,
                )
            )
            val composition = database.skillNodeDao().insert(
                node(
                    "Composition", "🧭", "Structure that carries a reader.",
                    "blush", vocabulary, AttributeKind.Creativity, 420,
                    SkillUnlockRule.ParentMastery, unlockParentMastery = 160, sortOrder = 32,
                )
            )
            database.skillNodeDao().insert(
                node(
                    "Editing", "🪶", "Cutting what does not serve the piece.",
                    "blush", composition, AttributeKind.Creativity, 500,
                    SkillUnlockRule.ParentMastery, unlockParentMastery = 220, sortOrder = 33,
                )
            )

            // ---- Branch 5: Learning ----
            val learning = database.skillNodeDao().insert(
                node(
                    "Learning", "🎓", "Time spent studying deliberately.",
                    "butter", null, AttributeKind.Discipline, 280, SkillUnlockRule.Root, sortOrder = 40,
                )
            )
            val practice = database.skillNodeDao().insert(
                node(
                    "Practice", "🔁", "Repetition that compounds.",
                    "butter", learning, AttributeKind.Discipline, 340,
                    SkillUnlockRule.ParentMastery, unlockParentMastery = 140, sortOrder = 41,
                )
            )
            val recall = database.skillNodeDao().insert(
                node(
                    "Recall", "🧠", "Retrieving it without looking.",
                    "butter", practice, AttributeKind.Discipline, 440,
                    SkillUnlockRule.ParentMastery, unlockParentMastery = 180, sortOrder = 42,
                )
            )
            database.skillNodeDao().insert(
                node(
                    "Mastery", "🌟", "Fluent, automatic, teachable.",
                    "butter", recall, AttributeKind.Discipline, 620,
                    SkillUnlockRule.ParentMastery, unlockParentMastery = 240, sortOrder = 43,
                )
            )
        }
    }
}
