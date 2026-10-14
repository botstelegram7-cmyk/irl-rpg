package com.irlrpg.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Step 1 → Step 2 schema evolution.
 *
 * Everything here is additive: existing tables keep every row, new quest columns
 * carry defaults that reproduce exactly the Step 1 behaviour (`MEDIUM` difficulty,
 * `STANDARD` shape, no deadline), and all new tables start empty. No user progress
 * — XP, levels, logs, attributes — is touched.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ---- quests: description, difficulty, deadline, shape, challenge window ----
        db.execSQL("ALTER TABLE `quests` ADD COLUMN `description` TEXT")
        db.execSQL("ALTER TABLE `quests` ADD COLUMN `difficulty` TEXT NOT NULL DEFAULT 'MEDIUM'")
        db.execSQL("ALTER TABLE `quests` ADD COLUMN `deadlineEpochDay` INTEGER")
        db.execSQL("ALTER TABLE `quests` ADD COLUMN `questKind` TEXT NOT NULL DEFAULT 'STANDARD'")
        db.execSQL("ALTER TABLE `quests` ADD COLUMN `durationDays` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `quests` ADD COLUMN `startedEpochDay` INTEGER NOT NULL DEFAULT 0")

        // ---- quest_logs: entries can now target a step of a multi-step quest ----
        db.execSQL("ALTER TABLE `quest_logs` ADD COLUMN `stepId` INTEGER")

        // ---- player_stats: cosmetic selections (never gameplay-affecting) ----
        db.execSQL("ALTER TABLE `player_stats` ADD COLUMN `displayTitle` TEXT")
        db.execSQL("ALTER TABLE `player_stats` ADD COLUMN `cosmeticFrame` TEXT NOT NULL DEFAULT 'frame_ivory'")

        // ---- multi-step objectives ----
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `quest_steps` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `questId` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `targetAmount` INTEGER NOT NULL,
                `unit` TEXT,
                `customUnit` TEXT,
                `isRequired` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`questId`) REFERENCES `quests`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_steps_questId` ON `quest_steps` (`questId`)")

        // ---- materialised recurring instances (unique per quest + period) ----
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `quest_instances` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `questId` INTEGER NOT NULL,
                `periodKey` TEXT NOT NULL,
                `epochDay` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `xpEarned` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`questId`) REFERENCES `quests`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_instances_questId` ON `quest_instances` (`questId`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_quest_instances_questId_periodKey` " +
                "ON `quest_instances` (`questId`, `periodKey`)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_instances_epochDay` ON `quest_instances` (`epochDay`)")

        // ---- universal skill tree ----
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `skill_nodes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `emoji` TEXT NOT NULL,
                `description` TEXT,
                `accentId` TEXT NOT NULL,
                `parentId` INTEGER,
                `attributeId` INTEGER,
                `masteryXp` INTEGER NOT NULL,
                `unlockRule` TEXT NOT NULL,
                `unlockParentMastery` INTEGER NOT NULL,
                `requiredAnyXp` INTEGER NOT NULL,
                `isBuiltIn` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`parentId`) REFERENCES `skill_nodes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`attributeId`) REFERENCES `attributes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_skill_nodes_parentId` ON `skill_nodes` (`parentId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_skill_nodes_attributeId` ON `skill_nodes` (`attributeId`)")

        // ---- earned achievements (definitions stay in code) ----
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `achievements_earned` (
                `code` TEXT PRIMARY KEY NOT NULL,
                `category` TEXT NOT NULL,
                `epochDay` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // ---- boss challenges ----
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `boss_challenges` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `emoji` TEXT NOT NULL,
                `accentId` TEXT NOT NULL,
                `deadlineEpochDay` INTEGER,
                `rewardXp` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `templateCode` TEXT,
                `startedEpochDay` INTEGER NOT NULL,
                `completedAtEpochDay` INTEGER,
                `xpAwarded` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `boss_objectives` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `bossId` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `metric` TEXT NOT NULL,
                `questId` INTEGER,
                `unitName` TEXT NOT NULL,
                `target` INTEGER NOT NULL,
                `isRequired` INTEGER NOT NULL,
                `manualProgress` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                FOREIGN KEY(`bossId`) REFERENCES `boss_challenges`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`questId`) REFERENCES `quests`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_boss_objectives_bossId` ON `boss_objectives` (`bossId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_boss_objectives_questId` ON `boss_objectives` (`questId`)")

        // Existing rows: give long-term fields a sensible start day (the day the
        // quest was created) without changing any progress value.
        db.execSQL(
            "UPDATE `quests` SET `startedEpochDay` = CAST(`createdAt` / 86400000 AS INTEGER) " +
                "WHERE `startedEpochDay` = 0"
        )
    }
}

/** All migrations, in order. */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
