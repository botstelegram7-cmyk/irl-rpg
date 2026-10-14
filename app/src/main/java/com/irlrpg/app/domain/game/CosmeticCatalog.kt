package com.irlrpg.app.domain.game

import com.irlrpg.app.domain.model.AchievementView
import com.irlrpg.app.domain.model.CosmeticItem
import com.irlrpg.app.domain.model.CosmeticKind

/**
 * Cosmetic rewards — avatar frames and titles. Purely visual: they never gate
 * quests, XP or skills, and nothing here is purchasable. Each item is unlocked by
 * an achievement the player actually earned (or is the free default).
 */
object CosmeticCatalog {

    val items: List<CosmeticItem> = listOf(
        CosmeticItem(
            code = "frame_ivory",
            name = "Warm Ivory",
            emoji = "🤍",
            accentId = "lavender",
            kind = CosmeticKind.Frame,
            unlockHint = "Default frame",
        ),
        CosmeticItem(
            code = "frame_lavender",
            name = "Lavender Ring",
            emoji = "💜",
            accentId = "lavender",
            kind = CosmeticKind.Frame,
            unlockHint = "Earn the Level 10 achievement",
        ),
        CosmeticItem(
            code = "frame_mint",
            name = "Mint Ring",
            emoji = "💚",
            accentId = "mint",
            kind = CosmeticKind.Frame,
            unlockHint = "Master your first skill node",
        ),
        CosmeticItem(
            code = "frame_sky",
            name = "Sky Ring",
            emoji = "💙",
            accentId = "sky",
            kind = CosmeticKind.Frame,
            unlockHint = "Earn 5,000 XP",
        ),
        CosmeticItem(
            code = "frame_peach",
            name = "Sunrise Ring",
            emoji = "🧡",
            accentId = "peach",
            kind = CosmeticKind.Frame,
            unlockHint = "Keep a 30 day streak",
        ),
        CosmeticItem(
            code = "frame_blush",
            name = "Blush Ring",
            emoji = "🩷",
            accentId = "blush",
            kind = CosmeticKind.Frame,
            unlockHint = "Complete three boss challenges",
        ),
        CosmeticItem(
            code = "frame_gold",
            name = "Gold Ring",
            emoji = "💛",
            accentId = "butter",
            kind = CosmeticKind.Frame,
            unlockHint = "Reach level 25",
        ),
        CosmeticItem(
            code = "title_steadfast",
            name = "Steadfast",
            emoji = "🗓️",
            accentId = "sky",
            kind = CosmeticKind.Title,
            unlockHint = "Keep a 7 day streak",
        ),
        CosmeticItem(
            code = "title_relentless",
            name = "Relentless",
            emoji = "🌈",
            accentId = "mint",
            kind = CosmeticKind.Title,
            unlockHint = "Complete a quest every day of one week",
        ),
        CosmeticItem(
            code = "title_centurion",
            name = "Centurion",
            emoji = "💯",
            accentId = "peach",
            kind = CosmeticKind.Title,
            unlockHint = "Reach 100 quest completions",
        ),
        CosmeticItem(
            code = "title_bibliophile",
            name = "Bibliophile",
            emoji = "📚",
            accentId = "sky",
            kind = CosmeticKind.Title,
            unlockHint = "Finish your first book",
        ),
        CosmeticItem(
            code = "title_tree_master",
            name = "Tree Master",
            emoji = "🌳",
            accentId = "mint",
            kind = CosmeticKind.Title,
            unlockHint = "Master three skill nodes",
        ),
        CosmeticItem(
            code = "title_boss_slayer",
            name = "Boss Slayer",
            emoji = "🐉",
            accentId = "peach",
            kind = CosmeticKind.Title,
            unlockHint = "Complete a boss challenge",
        ),
        CosmeticItem(
            code = "title_ascendant_of_habit",
            name = "Ascendant of Habit",
            emoji = "🌠",
            accentId = "lavender",
            kind = CosmeticKind.Title,
            unlockHint = "Earn 25,000 XP",
        ),
    )

    private const val DEFAULT_FRAME = "frame_ivory"

    fun byCode(code: String?): CosmeticItem? = items.firstOrNull { it.code == code }

    fun frames(): List<CosmeticItem> = items.filter { it.kind == CosmeticKind.Frame }

    fun titles(): List<CosmeticItem> = items.filter { it.kind == CosmeticKind.Title }

    /**
     * A cosmetic is unlocked either because it is the free default, or because the
     * achievement that grants it has been earned.
     */
    fun isUnlocked(item: CosmeticItem, earnedCodes: Set<String>): Boolean {
        if (item.code == DEFAULT_FRAME) return true
        val grant = AchievementCatalog.all.firstOrNull { it.unlocksCosmetic == item.code }
        return grant != null && earnedCodes.contains(grant.code)
    }

    fun unlockedFrames(earnedCodes: Set<String>): List<CosmeticItem> =
        frames().filter { isUnlocked(it, earnedCodes) }

    fun unlockedTitles(earnedCodes: Set<String>): List<CosmeticItem> =
        titles().filter { isUnlocked(it, earnedCodes) }

    /**
     * Plain-language requirement for a locked cosmetic, showing live progress
     * towards the achievement that grants it ("250 of 500 XP logged").
     */
    fun requirementText(item: CosmeticItem, views: List<AchievementView>): String {
        if (item.code == DEFAULT_FRAME) return "Always available"
        val grant = AchievementCatalog.all.firstOrNull { it.unlocksCosmetic == item.code }
            ?: return item.unlockHint
        val view = views.firstOrNull { it.def.code == grant.code }
            ?: return "$grant.name — ${grant.description}"
        if (view.earned) return "$grant.name — earned"
        return "$grant.name — ${view.progress.coerceAtMost(grant.target)} of ${grant.target}"
    }

    /** Static hint used in messages when the live progress is not at hand. */
    fun requirementTextFor(item: CosmeticItem): String = item.unlockHint

    /** The achievement that grants this cosmetic, if any. */
    fun grantingAchievement(item: CosmeticItem) =
        AchievementCatalog.all.firstOrNull { it.unlocksCosmetic == item.code }
}
