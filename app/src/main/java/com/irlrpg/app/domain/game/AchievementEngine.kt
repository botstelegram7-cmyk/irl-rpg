package com.irlrpg.app.domain.game

import com.irlrpg.app.domain.model.AchievementCategory
import com.irlrpg.app.domain.model.AchievementDef
import com.irlrpg.app.domain.model.AchievementView

/**
 * Pure evaluation of achievements.
 *
 * `progress(snapshot, def)` returns the player's *current* value for a condition
 * — it never returns a value that the recorded data does not support, and a
 * definition is never reported as earned on its own: earning is decided
 * exclusively by rows in `achievements_earned` that this engine asks the
 * repository to insert when a condition is first satisfied.
 */
object AchievementEngine {

    fun progress(snapshot: ActivitySnapshot, def: AchievementDef): Int = when (def.code) {
        "first_steps" -> snapshot.completions.coerceAtMost(1)
        "xp_500", "xp_5000", "xp_25000" -> snapshot.totalXp
        "level_10", "level_25" -> snapshot.level
        "completions_25", "completions_100" -> snapshot.completions

        "streak_3", "streak_7", "streak_30" -> snapshot.longestStreak
        "active_days_50" -> snapshot.activeDays.size
        "full_week_any" -> if (snapshot.fullWeekAchieved) 1 else 0

        "first_mastery" -> snapshot.masteredNodes
        "mastery_3" -> snapshot.masteredNodes
        "branch_complete" -> if (snapshot.branchComplete) 1 else 0
        "pages_500" -> snapshot.pagesLogged.toInt()
        "focus_25h" -> snapshot.minutesLogged
        "first_book" -> if (snapshot.bookFinished) 1 else 0

        "first_boss" -> snapshot.bossesCompleted
        "boss_3" -> snapshot.bossesCompleted
        "multi_step_done" -> if (snapshot.multiStepDone) 1 else 0
        "long_term_done" -> if (snapshot.longTermDone) 1 else 0

        "skill_tree_grower" -> snapshot.customSkillNodes
        "category_maker" -> snapshot.customCategories
        "quest_architect" -> snapshot.questCount
        "boss_builder" -> if (snapshot.customBossCreated) 1 else 0

        else -> 0
    }

    fun isSatisfied(snapshot: ActivitySnapshot, def: AchievementDef): Boolean =
        def.target > 0 && progress(snapshot, def) >= def.target

    /** Full catalogue with live progress and the recorded earn date, if any. */
    fun views(
        snapshot: ActivitySnapshot,
        earnedOn: Map<String, Long>,
        catalogue: List<AchievementDef> = AchievementCatalog.all,
    ): List<AchievementView> = catalogue.map { def ->
        AchievementView(
            def = def,
            progress = progress(snapshot, def).coerceAtMost(if (def.target > 0) def.target else 0),
            earnedEpochDay = earnedOn[def.code],
        )
    }

    /**
     * Definitions whose condition is met right now. The repository inserts only
     * the ones missing from `earnedOn`, so this is idempotent.
     */
    fun newlyEarned(
        snapshot: ActivitySnapshot,
        earnedOn: Map<String, Long>,
        catalogue: List<AchievementDef> = AchievementCatalog.all,
    ): List<AchievementDef> = catalogue.filter { def ->
        earnedOn[def.code] == null && isSatisfied(snapshot, def)
    }

    fun grouped(views: List<AchievementView>): Map<AchievementCategory, List<AchievementView>> =
        AchievementCategory.entries.associateWith { category ->
            views.filter { it.def.category == category }
        }

    fun summary(views: List<AchievementView>): Pair<Int, Int> =
        views.count { it.earned } to views.size

    /**
     * Detects a Monday–Sunday week in which every single day recorded activity.
     * Runs over the real active-day set, so the achievement can only appear when
     * the data supports it.
     */
    fun hasFullWeek(activeDays: Set<java.time.LocalDate>): Boolean {
        if (activeDays.size < 7) return false
        for (day in activeDays) {
            val start = com.irlrpg.app.core.DateX.startOfWeek(day)
            if ((0..6).all { activeDays.contains(start.plusDays(it.toLong())) }) return true
        }
        return false
    }

    /** Cosmetic codes unlocked by the achievements earned so far. */
    fun unlockedCosmetics(earnedCodes: Set<String>): Set<String> =
        AchievementCatalog.all
            .filter { it.unlocksCosmetic != null && earnedCodes.contains(it.code) }
            .mapNotNull { it.unlocksCosmetic }
            .toSet()
}
