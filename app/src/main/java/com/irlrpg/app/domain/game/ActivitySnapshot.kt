package com.irlrpg.app.domain.game

import java.time.LocalDate

/**
 * A read-only digest of everything the achievement system is allowed to look at.
 *
 * Every field is derived from recorded rows (logs, quests, categories, skill
 * nodes, bosses) — there is deliberately no place to inject a "fake" value, which
 * is what keeps achievements honest.
 */
data class ActivitySnapshot(
    val totalXp: Int = 0,
    val level: Int = 1,
    val completions: Int = 0,
    val activeDays: Set<LocalDate> = emptySet(),
    val longestStreak: Int = 0,
    val fullWeekAchieved: Boolean = false,
    val masteredNodes: Int = 0,
    val branchComplete: Boolean = false,
    val pagesLogged: Long = 0,
    val minutesLogged: Int = 0,
    val bookFinished: Boolean = false,
    val bossesCompleted: Int = 0,
    val multiStepDone: Boolean = false,
    val longTermDone: Boolean = false,
    val customSkillNodes: Int = 0,
    val customCategories: Int = 0,
    val questCount: Int = 0,
    val customBossCreated: Boolean = false,
    val skillNodesMasteredNames: List<String> = emptyList(),
)
