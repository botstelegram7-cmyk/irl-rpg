package com.irlrpg.app.domain.game

import com.irlrpg.app.domain.model.AchievementCategory
import com.irlrpg.app.domain.model.AchievementDef
import com.irlrpg.app.domain.model.AchievementTier

/**
 * Every achievement definition in the app.
 *
 * Definitions are constants (so the list is stable and translatable), but nothing
 * here is ever "pre-completed": an achievement is only shown as earned when a row
 * exists in `achievements_earned`, which `AchievementEngine` writes after the
 * condition is verified against the player's real recorded data.
 */
object AchievementCatalog {

    val all: List<AchievementDef> = listOf(
        // ---- Progress ----
        AchievementDef(
            code = "first_steps",
            name = "First Steps",
            description = "Reach the target of your very first quest.",
            emoji = "🌱",
            category = AchievementCategory.Progress,
            tier = AchievementTier.Bronze,
            target = 1,
        ),
        AchievementDef(
            code = "xp_500",
            name = "Getting Warm",
            description = "Earn 500 XP in total.",
            emoji = "⚡",
            category = AchievementCategory.Progress,
            tier = AchievementTier.Bronze,
            target = 500,
        ),
        AchievementDef(
            code = "xp_5000",
            name = "High Voltage",
            description = "Earn 5,000 XP in total.",
            emoji = "🔋",
            category = AchievementCategory.Progress,
            tier = AchievementTier.Silver,
            target = 5000,
            unlocksCosmetic = "frame_sky",
        ),
        AchievementDef(
            code = "xp_25000",
            name = "Unstoppable",
            description = "Earn 25,000 XP in total.",
            emoji = "🌠",
            category = AchievementCategory.Progress,
            tier = AchievementTier.Gold,
            target = 25000,
            unlocksCosmetic = "title_ascendant_of_habit",
        ),
        AchievementDef(
            code = "level_10",
            name = "Double Digits",
            description = "Reach character level 10.",
            emoji = "🔟",
            category = AchievementCategory.Progress,
            tier = AchievementTier.Silver,
            target = 10,
            unlocksCosmetic = "frame_lavender",
        ),
        AchievementDef(
            code = "level_25",
            name = "Grandmaster Path",
            description = "Reach character level 25.",
            emoji = "👑",
            category = AchievementCategory.Progress,
            tier = AchievementTier.Gold,
            target = 25,
            unlocksCosmetic = "frame_gold",
        ),
        AchievementDef(
            code = "completions_25",
            name = "Twenty-Five Wins",
            description = "Reach the target of 25 quests.",
            emoji = "🏅",
            category = AchievementCategory.Progress,
            tier = AchievementTier.Bronze,
            target = 25,
        ),
        AchievementDef(
            code = "completions_100",
            name = "Century Club",
            description = "Reach the target of 100 quests.",
            emoji = "💯",
            category = AchievementCategory.Progress,
            tier = AchievementTier.Silver,
            target = 100,
            unlocksCosmetic = "title_centurion",
        ),

        // ---- Consistency ----
        AchievementDef(
            code = "streak_3",
            name = "Momentum",
            description = "Keep a 3 day streak of completed quests.",
            emoji = "🔥",
            category = AchievementCategory.Consistency,
            tier = AchievementTier.Bronze,
            target = 3,
        ),
        AchievementDef(
            code = "streak_7",
            name = "Full Week",
            description = "Keep a 7 day streak of completed quests.",
            emoji = "🗓️",
            category = AchievementCategory.Consistency,
            tier = AchievementTier.Silver,
            target = 7,
            unlocksCosmetic = "title_steadfast",
        ),
        AchievementDef(
            code = "streak_30",
            name = "Month of Fire",
            description = "Keep a 30 day streak of completed quests.",
            emoji = "🌋",
            category = AchievementCategory.Consistency,
            tier = AchievementTier.Gold,
            target = 30,
            unlocksCosmetic = "frame_peach",
        ),
        AchievementDef(
            code = "active_days_50",
            name = "Fifty Days In",
            description = "Record activity on 50 different days.",
            emoji = "📆",
            category = AchievementCategory.Consistency,
            tier = AchievementTier.Silver,
            target = 50,
        ),
        AchievementDef(
            code = "full_week_any",
            name = "Every Day That Week",
            description = "Complete a quest on all seven days of one week.",
            emoji = "🌈",
            category = AchievementCategory.Consistency,
            tier = AchievementTier.Silver,
            target = 1,
            unlocksCosmetic = "title_relentless",
        ),

        // ---- Mastery (skills) ----
        AchievementDef(
            code = "first_mastery",
            name = "Skill Mastered",
            description = "Master your first skill tree node.",
            emoji = "🌟",
            category = AchievementCategory.Mastery,
            tier = AchievementTier.Silver,
            target = 1,
            unlocksCosmetic = "frame_mint",
        ),
        AchievementDef(
            code = "mastery_3",
            name = "Three Masteries",
            description = "Master three skill tree nodes.",
            emoji = "✨",
            category = AchievementCategory.Mastery,
            tier = AchievementTier.Gold,
            target = 3,
            unlocksCosmetic = "title_tree_master",
        ),
        AchievementDef(
            code = "branch_complete",
            name = "Branch Completed",
            description = "Master a node together with all of its children.",
            emoji = "🌳",
            category = AchievementCategory.Mastery,
            tier = AchievementTier.Gold,
            target = 1,
        ),
        AchievementDef(
            code = "pages_500",
            name = "Five Hundred Pages",
            description = "Log 500 pages across your reading quests.",
            emoji = "📖",
            category = AchievementCategory.Mastery,
            tier = AchievementTier.Silver,
            target = 500,
        ),
        AchievementDef(
            code = "focus_25h",
            name = "Twenty-Five Hours",
            description = "Track 25 hours of focused activity.",
            emoji = "⏳",
            category = AchievementCategory.Mastery,
            tier = AchievementTier.Silver,
            target = 1500,
        ),
        AchievementDef(
            code = "first_book",
            name = "First Book Finished",
            description = "Complete a milestone quest in a reading category.",
            emoji = "📚",
            category = AchievementCategory.Mastery,
            tier = AchievementTier.Gold,
            target = 1,
            unlocksCosmetic = "title_bibliophile",
        ),

        // ---- Challenges (bosses) ----
        AchievementDef(
            code = "first_boss",
            name = "Boss Down",
            description = "Complete your first boss challenge.",
            emoji = "🐉",
            category = AchievementCategory.Challenges,
            tier = AchievementTier.Silver,
            target = 1,
            unlocksCosmetic = "title_boss_slayer",
        ),
        AchievementDef(
            code = "boss_3",
            name = "Giant Slayer",
            description = "Complete three boss challenges.",
            emoji = "⚔️",
            category = AchievementCategory.Challenges,
            tier = AchievementTier.Gold,
            target = 3,
            unlocksCosmetic = "frame_blush",
        ),
        AchievementDef(
            code = "multi_step_done",
            name = "Step by Step",
            description = "Finish every step of a multi-step quest.",
            emoji = "🪜",
            category = AchievementCategory.Challenges,
            tier = AchievementTier.Silver,
            target = 1,
        ),
        AchievementDef(
            code = "long_term_done",
            name = "Long Haul",
            description = "Complete a long-term challenge.",
            emoji = "🏔️",
            category = AchievementCategory.Challenges,
            tier = AchievementTier.Gold,
            target = 1,
        ),

        // ---- Creator ----
        AchievementDef(
            code = "skill_tree_grower",
            name = "Grow Your Tree",
            description = "Create a custom skill node.",
            emoji = "🛠️",
            category = AchievementCategory.Creator,
            tier = AchievementTier.Bronze,
            target = 1,
        ),
        AchievementDef(
            code = "category_maker",
            name = "Organiser",
            description = "Create three categories of your own.",
            emoji = "🗂️",
            category = AchievementCategory.Creator,
            tier = AchievementTier.Bronze,
            target = 3,
        ),
        AchievementDef(
            code = "quest_architect",
            name = "Quest Architect",
            description = "Build 10 quests of any kind.",
            emoji = "🧱",
            category = AchievementCategory.Creator,
            tier = AchievementTier.Silver,
            target = 10,
        ),
        AchievementDef(
            code = "boss_builder",
            name = "Challenge Designer",
            description = "Create your own custom boss challenge.",
            emoji = "🎨",
            category = AchievementCategory.Creator,
            tier = AchievementTier.Bronze,
            target = 1,
        ),
    )

    fun byCode(code: String): AchievementDef? = all.firstOrNull { it.code == code }

    fun byCategory(category: AchievementCategory): List<AchievementDef> =
        all.filter { it.category == category }
}
